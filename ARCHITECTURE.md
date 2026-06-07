# Architecture

This document describes the internal architecture of the Go Game project — how the components are organized, how they communicate, and how data flows through the system.

## System Model

The project is a classic client-server architecture:

```
┌───────────────┐                           ┌───────────────┐
│   Client A    │──── TCP (port 5000) ──────│               │
│  (Swing GUI)  │                           │   GoServer    │
└───────────────┘                           │  (headless)   │
                                            │               │
┌───────────────┐                           │               │
│   Client B    │──── TCP (port 5000) ──────│               │
│  (Swing GUI)  │                           └───────────────┘
└───────────────┘
```

- The **server** is the single source of truth for all game state. It validates every move, applies game rules, and broadcasts the resulting board to both clients.
- The **clients** are rendering terminals. They display whatever board state the server sends and forward user actions (clicks, passes) to the server. They do not independently validate moves.
- Communication uses **plain-text lines over TCP** (one message per line, delimited by `\n`).

## Server Architecture

The server module (`GoGameServer`) has three classes:

### GoServer

The entry point. Opens a `ServerSocket` on port 5000 and runs an infinite loop:

1. Accept a connection.
2. If no player is waiting, assign this player BLACK and store them in the `waitingPlayer` slot. Start their `ClientHandler` thread immediately (so disconnections during the wait are detected).
3. If a player is already waiting, assign the new player WHITE and create a `GameSession` for the pair.
4. Clear the `waitingPlayer` slot and loop back for the next pair.

The server does not limit the number of concurrent sessions. Each session is isolated and runs on its own pair of threads.

Before pairing, the server checks `waitingPlayer.isSocketClosed()` to handle cases where the first player disconnected while waiting for an opponent.

### ClientHandler

One instance per connected player. Implements `Runnable` and runs on a dedicated thread.

**Constructor:**
- Opens `BufferedReader` (input) and `PrintWriter` (output, auto-flush) on the socket.
- Immediately sends `WELCOME:<COLOR>` to the client so it knows its assigned color.

**Read loop (`run()`):**
- Reads lines from the socket in a `while` loop.
- Parses the command prefix:
  - `MOVE:<r>,<c>` → calls `session.handleMove(this, r, c)`
  - `PASS` → calls `session.handlePass(this)`
  - `RESTART_REQ` → calls `session.handleRestartRequest(this)`
- On `IOException` (client disconnected), falls through to the `finally` block which calls `session.handleDisconnect(this)`.

### GameSession

The core game state container. Holds:
- A `GameLogic` instance (the authoritative board).
- References to both `ClientHandler` instances.
- Turn tracking (`currentTurn`), pass tracking (`consecutivePasses`), game state flag (`gameInProgress`), and restart voting flags.

**All public methods are `synchronized`** because two `ClientHandler` threads can call into the session concurrently. Without synchronization, two moves processed simultaneously could corrupt the board or skip turn advancement.

**Key methods:**

| Method                  | What it does                                                                                           |
|-------------------------|--------------------------------------------------------------------------------------------------------|
| `startNewGame()`        | Resets board, sets turn to BLACK, clears pass counter, broadcasts `GAME_STARTED` + board + turn.        |
| `handleMove(sender,r,c)`| Validates turn order and move legality via `game.placeStone()`. On success: resets pass counter, broadcasts `MOVE_OK`, updated board, and next turn. On failure: sends `INVALID_MOVE` to sender only. |
| `handlePass(sender)`    | Increments `consecutivePasses`. If ≥ 2, calls `endGame()`. Otherwise advances the turn.                |
| `handleRestartRequest()`| Sets a per-player restart flag. When both flags are set, calls `startNewGame()`.                        |
| `handleDisconnect()`    | Sets `gameInProgress = false`, notifies the opponent with `OPPONENT_DISCONNECTED`.                      |
| `endGame()`             | Calls `game.countTerritory()` for both colors, compares scores, broadcasts `GAME_OVER:<result>`.        |

## Client Architecture

The client module (`GoGameClient`) has seven classes that form a screen-based navigation flow:

```
StartScreen ──(connect + wait)──► GameScreen ──(game over)──► EndScreen
                                      │                            │
                                 EXIT → StartScreen      PLAY AGAIN → GameScreen
                                                         EXIT → System.exit(0)
```

### GoGameClient

The `main()` method. Schedules `StartScreen` creation on the Swing Event Dispatch Thread (EDT) via `EventQueue.invokeLater()`.

### StartScreen

A `JFrame` with an IP text field and a START button. On button click:
1. Reads the IP address from the text field.
2. Spawns a new thread to open a `NetworkClient` connection (to avoid freezing the GUI).
3. Sets a `LobbyListener` on the `NetworkClient`.

The `LobbyListener` handles:
- `onWelcome(color)` — stores the assigned color and updates the button text to "You are BLACK — waiting..."
- `onGameStarted()` — creates a `GameScreen`, passes it the `NetworkClient` and color, and disposes `StartScreen`.
- `onOpponentDisconnected()` — shows a warning dialog and re-enables the START button.

### GameScreen

The main game window. Contains:
- A board panel (`jPanel_Board`) with a custom `paintComponent` that draws the grid and stones.
- A sidebar with: match status (connection, turn, scores), move history list, PASS button, EXIT button.

**Board rendering:** The `paintComponent` override draws 9 vertical and 9 horizontal grid lines, then iterates through `game.getBoard()` and draws filled rectangles (with outlines) for each non-empty cell.

**Click handling:** `handleClick()` converts pixel coordinates to grid indices using `Math.round(x / cellWidth)`, checks bounds, and calls `networkClient.sendMove(i, j)`.

**Custom cursors:** Three 32×32 PNG images (black hand, white hand, empty hand) are loaded at initialization. The cursor changes to the player's stone color on their turn and switches to the empty hand on the opponent's turn.

**`GameScreenListener` callbacks:**
- `onBoardUpdate(data)` — deserializes the board and repaints.
- `onTurnChange(whoseTurn)` — updates `isMyTurn` flag and cursor.
- `onMoveOk(r, c, color)` — appends to the move history list.
- `onPassOk(color)` — appends a "PASS" entry to the history.
- `onInvalidMove()` — shows a temporary error message.
- `onGameOver(winner)` — creates `EndScreen` and disposes `GameScreen`.
- `onOpponentDisconnected()` — shows a dialog, disconnects, returns to `StartScreen`.

### EndScreen

Displays the winner label and two buttons: PLAY AGAIN and EXIT.
- **PLAY AGAIN:** calls `networkClient.sendRestart()`, disables the button, and shows "WAITING..." until the opponent also accepts.
- **EXIT:** disconnects and calls `System.exit(0)`.

The `EndScreenListener` listens for `onGameStarted()` (both players accepted rematch) to create a new `GameScreen`.

The `myColor` field is preserved across rematches because the server does not re-send the color assignment on restart.

### NetworkClient

Manages the TCP connection and incoming message dispatch.

**Constructor:** Opens a `Socket`, wraps it in `BufferedReader`/`PrintWriter`, and starts a daemon listener thread.

**Listener thread (`listenLoop`):** Reads lines in a loop. Each line is passed to `parseAndDispatch()`, which uses string prefix matching (`startsWith`) to determine the message type and calls the appropriate method on the current `ServerMessageListener`.

**Listener swapping:** The `setListener()` method allows different screens to register themselves as the active listener. When `StartScreen` is visible, it handles lobby messages. When `GameScreen` is active, it handles game messages. This is the Observer pattern — the `ServerMessageListener` interface has 10 callback methods, and each screen provides its own implementation.

**Thread safety:**
- `running` is `volatile` so that `disconnect()` (called from the EDT) can signal the listener thread to stop.
- All listener callbacks that touch Swing components use `SwingUtilities.invokeLater()` since the listener thread is not the EDT.

### GameLogic

The Go rules engine. Used by both client and server (the server's `GoGameServer` module depends on `GoGameClient` in Maven to access this class).

**Board representation:** `int[9][9]` array where `0` = empty, `1` = black, `2` = white.

**Core algorithms:**

1. **Liberty check (`hasLiberty`):** DFS traversal of a connected group of same-color stones. Marks cells as visited, checks all four orthogonal neighbors. If any neighbor is empty, the group has a liberty. If a same-color unvisited neighbor exists, recurse into it.

2. **Capture check (`checkCaptures`):** After placing a stone, checks all four neighbors for enemy stones. For each enemy neighbor, runs `hasLiberty` on its group. If the group has no liberties, `removeGroup` sets all its cells to EMPTY.

3. **Suicide validation (in `placeStone`):** After captures are processed, runs `hasLiberty` on the newly placed stone. If it has no liberties (and capturing didn't free any), the move is reverted and `placeStone` returns `false`.

   The order is critical: captures are resolved **before** the suicide check. Capturing enemy stones may create liberties for the placed stone, so checking suicide first would incorrectly reject valid moves.

4. **Territory scoring (`countTerritory`):** Iterates the board. Counts all stones of the given color. For each unvisited empty cell, runs a flood-fill (`floodFillTerritory`) to collect the connected empty region and the set of stone colors bordering it. If the region is bordered by exactly one color, those empty cells count as that color's territory.

5. **Serialization (`serializeBoard` / `deserializeBoard`):** Converts the board to/from an 81-character string for network transmission. Row-major order, each cell is the char `'0'`, `'1'`, or `'2'`.

### FontUtil

Static utility class. Loads `Retro-Gaming.ttf` from one of three hardcoded file paths (to handle different working directories). Registers the font with the JVM's `GraphicsEnvironment`. The `setCustomFont()` method recursively walks a Swing container tree and applies the font to every component, preserving each component's existing size and style.

If the font file is missing, the class prints an error to stderr but does not crash — the GUI falls back to the default system font.

## Threading Model

| Thread               | Purpose                                                       |
|----------------------|---------------------------------------------------------------|
| Server main thread   | Runs the `ServerSocket.accept()` loop                         |
| ClientHandler thread | One per connected player — reads commands from that socket     |
| Swing EDT            | Handles all GUI events and rendering on the client             |
| NetworkClient thread | Daemon thread on the client — reads server messages            |
| Connection thread    | Short-lived thread on the client — opens the TCP socket        |

**Synchronization points:**
- `GameSession` methods are `synchronized` to serialize access from two `ClientHandler` threads.
- `isMyTurn` in `GameScreen` is `volatile` — read by the EDT (click handler) and written by the network listener thread.
- `running` in `NetworkClient` is `volatile` — read by the listener thread and written by the EDT (via `disconnect()`).
- All Swing updates from non-EDT threads use `SwingUtilities.invokeLater()`.

## Dependency Graph

```
GoGameServer
    └── depends on ──► GoGameClient (Maven dependency)
                            └── provides ──► GameLogic.java (used by GameSession)

GoGameClient
    └── depends on ──► org.netbeans.external:AbsoluteLayout (layout manager for .form files)
```

The server module imports `com.mycompany.gogameclient.GameLogic` to use the same rules engine that the client uses for rendering. This means `GameLogic` is shared code, but it lives in the client module.
