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

            ClientHandler waitingPlayer = null;

            while (true) {
                System.out.println("[Server] Waiting for new connections...");
                
                Socket clientSocket = null;
                try {
                    // Accept one player at a time (blocks here until someone joins)
                    clientSocket = serverSocket.accept();
                    System.out.println("[Server] Connection received from " + clientSocket.getInetAddress());

                    // If we have a waiting player, but their socket is closed (they dropped while waiting), discard them
                    if (waitingPlayer != null && waitingPlayer.isSocketClosed()) {
                        System.out.println("[Server] Previous waiting player disconnected. Lobby reset.");
                        waitingPlayer = null;
                    }

                    if (waitingPlayer == null) {
                        // This is Player 1 (Black)
                        waitingPlayer = new ClientHandler(clientSocket, GameLogic.BLACK);
                        
                        // Start thread IMMEDIATELY so we can detect if they disconnect while waiting
                        new Thread(waitingPlayer, "Player-BLACK").start();
                        
                        waitingPlayer.sendMessage("MESSAGE:Waiting for opponent...");
                        System.out.println("[Server] Player 1 is waiting in lobby.");
                        
                    } else {
                        // This is Player 2 (White)
                        ClientHandler newPlayer = new ClientHandler(clientSocket, GameLogic.WHITE);
                        new Thread(newPlayer, "Player-WHITE").start();
                        
                        System.out.println("[Server] Player 2 joined. Creating session.");

                        // Create a new session for these two players
                        GameSession session = new GameSession(waitingPlayer, newPlayer);
                        waitingPlayer.setSession(session);
                        newPlayer.setSession(session);

                        // start the game
                        session.startNewGame();
                        
                        // Clear lobby for the next pair
                        waitingPlayer = null;
                    }

                } catch (IOException e) {
                    System.err.println("[Server] Error handling connection: " + e.getMessage());
                    // Crucial fix: close the socket safely and loop back to accept the next one!
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        try {
                            clientSocket.close();
                        } catch (IOException ignored) {}
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("[Server] Fatal server socket error: " + e.getMessage());
        }
    }
}
