// ClientHandler.java - Handles communication with a single player
package com.mycompany.gogameserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import com.mycompany.gogameclient.GameLogic;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private GameSession session;
    private final int playerColor; // GameLogic.BLACK or GameLogic.WHITE
    private final String playerName; // "BLACK" or "WHITE"

    // Constructor
    public ClientHandler(Socket socket, int playerColor) throws IOException {
        this.socket = socket;
        this.playerColor = playerColor;
        this.playerName = (playerColor == GameLogic.BLACK) ? "BLACK" : "WHITE";
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        
        // Tell this client which color they are immediately upon connection
        sendMessage("WELCOME:" + playerName);
        System.out.println("[Server] " + playerName + " connected from " + socket.getInetAddress());
    }

    // Assign the game session
    public void setSession(GameSession session) {
        this.session = session;
    }

    // Getters for player details
    public int getPlayerColor() {
        return playerColor;
    }

    public String getPlayerName() {
        return playerName;
    }

    // Send a message to this client
    public void sendMessage(String msg) {
        out.println(msg);
    }

    // Main read loop runs on its own thread
    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {

                System.out.println("[Server] " + playerName + " >> " + line);

                if (line.startsWith("MOVE:")) {
                    // parse MOVE:r,c
                    String[] parts = line.substring(5).split(",");
                    int r = Integer.parseInt(parts[0]);
                    int c = Integer.parseInt(parts[1]);
                    if (session != null) session.handleMove(this, r, c);

                } else if (line.equals("PASS")) {
                    if (session != null) session.handlePass(this);

                } else if (line.equals("RESTART_REQ")) {
                    if (session != null) session.handleRestartRequest(this);
                }
            }
        } catch (IOException e) {
            System.out.println("[Server] " + playerName + " disconnected unexpectedly.");
        } finally {
            if (session != null) {
                session.handleDisconnect(this);
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
