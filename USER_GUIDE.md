# User Guide

How to play Go Game from start to finish.

## Starting a Game

### 1. Start the Server

Run the server first. You need one server running for players to connect to.

```bash
cd GoGameServer
java -cp target/GoGameServer-1.0-SNAPSHOT-jar-with-dependencies.jar com.mycompany.gogameserver.GoServer
```

The server starts listening on port 5000. You'll see:

```
GO Game Server started on port 5000
[Server] Waiting for new connections...
```

Leave this running for the entire play session.

### 2. Launch Two Clients

Each player runs their own client. You can run both on the same computer or on different machines.

```bash
cd GoGameClient
mvn exec:java
```

A window appears with the title "GO GAME" and a text field labeled "SERVER IP ADRESS."

### 3. Connect to the Server

- If playing on the same machine as the server, enter `127.0.0.1`.
- If playing over a network, enter the server machine's IP address.

Click **START**.

The first player to connect is assigned **BLACK** and sees "You are BLACK — waiting..." while the server waits for an opponent.

When the second player connects and clicks START, the server pairs them and both clients automatically transition to the game board.

## Playing the Game

### The Board

The game uses a 9×9 Go board. The board is drawn as a grid of intersecting lines on a tan background.

### Placing Stones

- Click on any intersection to place a stone.
- You can only place stones when it's your turn — the bottom-left label shows either "> Your Turn (BLACK)" or "Opponent's Turn."
- Your mouse cursor changes to a hand holding your stone color when it's your turn, and an empty hand when it's not.
- If your move is invalid (occupied intersection, suicide move), you'll see "Invalid move! Try again." in the status area. Your turn is not consumed — try placing elsewhere.

### Passing

Click the **PASS** button in the sidebar to skip your turn. Passing does not end the game on its own — only two consecutive passes trigger the end.

You can still make moves after passing. A pass just means you choose not to play this turn.

### The Sidebar

The right sidebar displays:

| Section         | Information                                         |
|-----------------|-----------------------------------------------------|
| Match Status    | Connection state and whose turn it is               |
| Scores          | Your score and opponent's score (updated live)       |
| Player Moves    | Scrollable list of all moves and passes with numbers |
| PASS button     | Skips your turn                                     |
| EXIT GAME button| Disconnects and returns to the start screen          |

### Game Rules Enforced

The server enforces the following Go rules:

1. **No placing on occupied intersections** — you can't place a stone where one already exists.
2. **Capture** — when your stone placement leaves an adjacent enemy group with no liberties (no empty adjacent intersections), that enemy group is removed from the board.
3. **No suicide** — you can't place a stone that would leave your own group with no liberties, unless that move captures an enemy group first (which creates liberties).
4. **Turn order** — Black always goes first. Players alternate turns. You can't move out of turn.

### Scoring

The game uses **Chinese-style territory scoring**:

- **Stones on the board** count as points for their color.
- **Empty regions** that are completely surrounded by a single color count as territory for that color.
- The player with more total points wins.

Scores are displayed in the sidebar and update after every move.

## Ending a Game

The game ends when **both players pass consecutively**. This is how Go games normally end — when neither player sees a worthwhile move.

After the game ends:

1. The server calculates the final score.
2. Both clients see the **End Screen** showing "WINNER: BLACK", "WINNER: WHITE", or "WINNER: DRAW".
3. Two buttons are available:
   - **PLAY AGAIN** — sends a rematch request to the server.
   - **EXIT** — disconnects and closes the application.

## Rematching

When you click **PLAY AGAIN**:

1. Your button changes to "WAITING..." and you see "Waiting for opponent..."
2. If the opponent also clicks PLAY AGAIN, the server starts a new game immediately.
3. Both clients transition back to the game board with a fresh 9×9 grid.
4. Your assigned color (BLACK or WHITE) stays the same across rematches.

If the opponent clicks EXIT instead, you'll see "Your opponent has disconnected" and the PLAY AGAIN button is disabled.

## Disconnection

If your opponent disconnects at any point during the game:

- You'll see a dialog: "Your opponent has disconnected. Returning to Start Screen."
- Click OK to return to the start screen, where you can connect to the server again and wait for a new opponent.

If the server goes down, the client shows a connection error and returns to the start screen.

## Exiting

- From the **Start Screen**: click **EXIT** to close the application.
- From the **Game Screen**: click **EXIT GAME** in the sidebar. This disconnects from the server and returns to the start screen.
- From the **End Screen**: click **EXIT** to disconnect and close the application entirely.

## Quick Reference

| Action               | How                                              |
|----------------------|--------------------------------------------------|
| Connect              | Enter server IP → click START                    |
| Place a stone        | Click an intersection on the board               |
| Pass                 | Click the PASS button                            |
| End the game         | Both players pass consecutively                  |
| Rematch              | Both players click PLAY AGAIN on the end screen  |
| Exit mid-game        | Click EXIT GAME in the sidebar                   |
| Exit after game      | Click EXIT on the end screen                     |
