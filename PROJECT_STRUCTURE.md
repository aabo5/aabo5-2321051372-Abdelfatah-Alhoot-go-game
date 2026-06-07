# Project Structure

A breakdown of every directory and file in the repository and what it does.

## Top Level

```
.
├── GoGameClient/           # Client application (Swing GUI + networking)
├── GoGameServer/           # Server application (headless, console-only)
├── Screenshots/            # Application screenshots for documentation
├── README.md               # Main project documentation
├── ARCHITECTURE.md         # Architecture deep-dive
├── INSTALLATION.md         # Build and run instructions
├── PROJECT_STRUCTURE.md    # This file
├── USER_GUIDE.md           # How to play
├── CONTRIBUTING.md         # Contribution guidelines
├── REPOSITORY_REVIEW.md    # Code review and improvement suggestions
├── project_report.pdf      # Project report document
└── .gitignore              # Git ignore rules for Maven output, IDE files, OS files
```

## GoGameClient

The client module. Built with Maven. Depends on `org.netbeans.external:AbsoluteLayout` for the NetBeans form-designer layout.

```
GoGameClient/
├── pom.xml                                     # Maven build configuration
│                                               #   - groupId: com.mycompany
│                                               #   - artifactId: GoGameClient
│                                               #   - Java 17 source/target
│                                               #   - exec.mainClass: GoGameClient
│
├── resources/
│   ├── fonts/
│   │   └── Retro-Gaming.ttf                    # Custom pixel font (31 KB)
│   │                                           #   Loaded by FontUtil.java at runtime
│   │                                           #   Falls back to system font if missing
│   │
│   └── images/
│       ├── GoGameIcon.ico                      # Application icon (unused in code)
│       ├── hand_black.png                      # Cursor image — hand holding black stone
│       ├── hand_white.png                      # Cursor image — hand holding white stone
│       └── hand_empty.png                      # Cursor image — empty hand (opponent's turn)
│                                               #   All three are 32×32 scaled at runtime
│                                               #   Loaded by GameScreen.initCursors()
│
├── src/
│   ├── main/java/com/mycompany/gogameclient/
│   │   ├── GoGameClient.java                   # Entry point
│   │   │                                       #   main() → EventQueue.invokeLater(StartScreen)
│   │   │
│   │   ├── StartScreen.java                    # First screen — connection lobby
│   │   │                                       #   IP text field, START/EXIT buttons
│   │   │                                       #   Inner class: LobbyListener
│   │   │
│   │   ├── GameScreen.java                     # Main game screen
│   │   │                                       #   Board panel (custom paintComponent)
│   │   │                                       #   Sidebar: status, move history, buttons
│   │   │                                       #   Inner class: GameScreenListener
│   │   │                                       #   Custom cursors, click-to-grid conversion
│   │   │
│   │   ├── EndScreen.java                      # Post-game screen
│   │   │                                       #   Winner display, PLAY AGAIN, EXIT
│   │   │                                       #   Inner class: EndScreenListener
│   │   │
│   │   ├── NetworkClient.java                  # TCP client + message dispatcher
│   │   │                                       #   Interface: ServerMessageListener
│   │   │                                       #   Daemon listener thread
│   │   │                                       #   sendMove(), sendPass(), sendRestart()
│   │   │
│   │   ├── GameLogic.java                      # Go rules engine (shared with server)
│   │   │                                       #   Board representation (int[9][9])
│   │   │                                       #   Liberty DFS, capture, suicide check
│   │   │                                       #   Territory scoring (flood-fill)
│   │   │                                       #   Board serialization/deserialization
│   │   │
│   │   ├── FontUtil.java                       # Custom font loader
│   │   │                                       #   Loads Retro-Gaming.ttf
│   │   │                                       #   Recursively applies to Swing containers
│   │   │
│   │   ├── StartScreen.form                    # NetBeans GUI designer layout (XML)
│   │   ├── GameScreen.form                     # NetBeans GUI designer layout (XML)
│   │   └── EndScreen.form                      # NetBeans GUI designer layout (XML)
│   │
│   └── test/java/                              # Test directory (empty — no tests written)
│
└── target/                                     # Maven build output (gitignored)
    ├── GoGameClient-1.0-SNAPSHOT.jar
    └── GoGameClient-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## GoGameServer

The server module. Built with Maven. Depends on `GoGameClient` (to access `GameLogic`). Uses `maven-assembly-plugin` to produce a fat JAR with all dependencies bundled.

```
GoGameServer/
├── pom.xml                                     # Maven build configuration
│                                               #   - depends on GoGameClient:1.0-SNAPSHOT
│                                               #   - maven-assembly-plugin for fat JAR
│                                               #   - exec.mainClass: GoServer
│
├── src/
│   ├── main/java/com/mycompany/gogameserver/
│   │   ├── GoServer.java                       # Entry point
│   │   │                                       #   ServerSocket on port 5000
│   │   │                                       #   Infinite accept loop
│   │   │                                       #   Player pairing logic
│   │   │
│   │   ├── ClientHandler.java                  # Per-player socket handler
│   │   │                                       #   Implements Runnable
│   │   │                                       #   Reads MOVE/PASS/RESTART_REQ
│   │   │                                       #   Forwards to GameSession
│   │   │
│   │   └── GameSession.java                    # Game state manager
│   │                                           #   synchronized methods
│   │                                           #   Move validation + broadcast
│   │                                           #   Pass tracking → game over
│   │                                           #   Restart voting
│   │                                           #   Disconnect handling
│   │
│   └── test/java/                              # Test directory (empty — no tests written)
│
└── target/                                     # Maven build output (gitignored)
    ├── GoGameServer-1.0-SNAPSHOT.jar
    └── GoGameServer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Screenshots

Contains PNG screenshots of the running application, referenced by the README:

```
Screenshots/
├── StartScreen.PNG                 # Connection lobby with IP input
├── Gameplay.PNG                    # Active game with stones on the board
├── EndScreen.PNG                   # Game over — winner displayed
├── WaitingForOpponentReplay.PNG    # Waiting for opponent to accept rematch
├── OpponentDisconnected.PNG        # Dialog shown when opponent drops out
└── ServerLog.PNG                   # Server console output on AWS EC2
```

## .form Files

The three `.form` files (`StartScreen.form`, `GameScreen.form`, `EndScreen.form`) are XML layout descriptors generated by the NetBeans GUI Designer (Matisse). They define the visual layout of each screen — component positions, sizes, borders, fonts, colors, and event bindings.

The corresponding `.java` files contain auto-generated `initComponents()` methods (marked with `// GEN-BEGIN` / `// GEN-END` comments) that build the Swing component tree at runtime. These methods should not be edited by hand — changes should be made through the NetBeans form editor.

## What's Not in the Repository

- **No database** — all game state is held in memory and lost when the server stops.
- **No configuration files** — all settings (port, board size) are hardcoded constants.
- **No test classes** — the `src/test/java/` directories exist but are empty.
- **No CI/CD configuration** — no GitHub Actions, Jenkins, or similar.
- **No Dockerfile** — the server could easily be containerized but no config exists yet.
