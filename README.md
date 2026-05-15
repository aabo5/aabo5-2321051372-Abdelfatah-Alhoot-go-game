# Java Multiplayer Go Game ♟️

A fully-functional, networked, multiplayer Go (Weiqi/Baduk) game built entirely in Java. This project was developed as a university submission to demonstrate client-server architecture, multithreading, and GUI application design.

## 🌟 Features

*   **Multiplayer Networking**: Play against opponents in real-time over TCP sockets.
*   **Unlimited Sessions**: The server is designed to handle unlimited concurrent game sessions (pairs of 2 players) simultaneously.
*   **Core Go Mechanics**: 
    *   9x9 grid layout.
    *   Validates legal moves (prevents suicide and overriding stones).
    *   Detects and processes captured groups of stones.
    *   Chinese-style territory scoring (counts both stones on board and surrounded empty intersections) at the end of the game.
*   **Game Flow**: Support for passing turns, automatically detecting game over (two consecutive passes), and a voting system for rematches.
*   **Custom UI**: A retro-styled graphical interface built using the NetBeans Swing GUI Designer, featuring custom retro fonts and a hand-drawn grid.

## 🏗️ Project Architecture

The repository is structured as a Maven project containing two modules:

### 1. `GoGameServer` (The Backend)
A headless console application that acts as the single source of truth for all games.
*   **`GoServer.java`**: The main entry point. Runs an infinite loop accepting incoming connections and pairs them up.
*   **`GameSession.java`**: Represents an isolated "room" for two players. It holds the board state, manages turns, tracks consecutive passes, and broadcasts updates to both clients.
*   **`ClientHandler.java`**: A dedicated thread assigned to each connected player to handle incoming messages without blocking the main server.

### 2. `GoGameClient` (The Frontend)
A Java Swing application where players interact with the game.
*   **`GoGameClient.java`**: The entry point that launches the UI.
*   **`StartScreen.java`**: The connection lobby where users enter the server IP.
*   **`GameScreen.java`**: The main interface displaying the board and handling mouse clicks. 
*   **`EndScreen.java`**: Displays the final score and winner, offering a "Play Again" button.
*   **`GameLogic.java`**: Contains the algorithms (like Flood-fill via DFS) for evaluating liberties, captures, and territory scoring.
*   **`NetworkClient.java`**: A daemon thread that connects to the server and delegates incoming network commands to the active screen via a listener interface.

## 📡 Networking Protocol

The client and server communicate via a custom String-based protocol over TCP:
*   `WELCOME:BLACK` / `WELCOME:WHITE` -> Assigns a player color upon connection.
*   `MOVE:r,c` -> Client requests to place a stone at row `r` and column `c`.
*   `UPDATE:<81-char-string>` -> Server broadcasts the complete board state using `0` (Empty), `1` (Black), and `2` (White).
*   `PASS` -> Client passes their turn.
*   `RESTART_REQ` -> Client requests a rematch.
*   `GAME_OVER:<WINNER>` -> Server announces the end of the game.

## 🚀 How to Run

1.  **Start the Server:**
    *   Open the `GoGameServer` module in your IDE (like NetBeans or IntelliJ).
    *   Run `GoServer.java`. You will see it listening on port `5000` in the console.
2.  **Start the Clients:**
    *   Open the `GoGameClient` module.
    *   Run `GoGameClient.java` to launch the first player window.
    *   Run `GoGameClient.java` again to launch the second player window.
3.  **Play:**
    *   In both client windows, leave the IP address as `localhost` (or enter the IP if running on different machines) and click **Connect**.
    *   The server will pair the two windows into a session, and the game will begin!

## 🛠️ Built With

*   **Java 11+**
*   **Maven** (Dependency and Build Management)
*   **Java Swing** (UI Framework)
*   **NetBeans GUI Builder** (For `.form` layout files)
