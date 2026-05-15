// GoServer.java - Go game server to handle multiplayer
package com.mycompany.gogameserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import com.mycompany.gogameclient.GameLogic;

public class GoServer {

    private static final int PORT = 5000;

    // Start the server
    public static void main(String[] args) {
        new GoServer().start();
    }

    // Wait for players to connect in an infinite loop
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("GO Game Server started on port " + PORT);

            while (true) {
                System.out.println("Waiting for 2 players to connect to a new session...");

                // accept player 1 (BLACK)
                Socket s1 = serverSocket.accept();
                ClientHandler playerBlack = new ClientHandler(s1, GameLogic.BLACK);
                System.out.println("[Server] Player 1 (BLACK) connected.");
                playerBlack.sendMessage("MESSAGE:Waiting for opponent...");

                // accept player 2 (WHITE)
                Socket s2 = serverSocket.accept();
                ClientHandler playerWhite = new ClientHandler(s2, GameLogic.WHITE);
                System.out.println("[Server] Player 2 (WHITE) connected.");

                // Create a new session for these two players
                GameSession session = new GameSession(playerBlack, playerWhite);
                playerBlack.setSession(session);
                playerWhite.setSession(session);

                // Start their network threads
                new Thread(playerBlack, "Player-BLACK").start();
                new Thread(playerWhite, "Player-WHITE").start();

                // start the game
                session.startNewGame();
            }

        } catch (IOException e) {
            System.err.println("[Server] Fatal error: " + e.getMessage());
        }
    }
}
