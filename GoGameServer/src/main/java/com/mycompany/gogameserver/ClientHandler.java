/*
 * ClientHandler.java — One instance per connected player.
 * Runs on its own thread, reading messages from the client socket
 * and delegating game actions to the GoServer.
 */
package com.mycompany.gogameserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import com.mycompany.gogameclient.GameLogic;

/**
 * Handles a single client connection on the server side.
 *
 * @author abdel
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final GoServer server;
    private final int playerColor; /* GameLogic.BLACK or GameLogic.WHITE */
    private final String playerName; /* "BLACK" or "WHITE" */

    /* ========================================= */
    /* Constructor */
    /* ========================================= */
    public ClientHandler(Socket socket, GoServer server, int playerColor) throws IOException {
        this.socket = socket;
        this.server = server;
        this.playerColor = playerColor;
        this.playerName = (playerColor == GameLogic.BLACK) ? "BLACK" : "WHITE";
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    /* ========================================= */
    /* Accessors */
    /* ========================================= */
    public int getPlayerColor() {
        return playerColor;
    }

    public String getPlayerName() {
        return playerName;
    }

    /* ========================================= */
    /* Send a protocol message to this client */
    /* ========================================= */
    public void sendMessage(String msg) {
        out.println(msg);
    }

    /* ========================================= */
    /* Main read loop (runs on its own thread) */
    /*                                           */
    /* Incoming messages from the client: */
    /* MOVE:r,c — place a stone */
    /* PASS — pass the turn */
    /* RESTART_REQ — request a rematch */
    /* ========================================= */
    @Override
    public void run() {
        try {
            /* tell this client which color they are */
            sendMessage("WELCOME:" + playerName);
            System.out.println("[Server] " + playerName + " connected from "
                    + socket.getInetAddress());

            String line;
            while ((line = in.readLine()) != null) {

                System.out.println("[Server] " + playerName + " >> " + line);

                if (line.startsWith("MOVE:")) {
                    /* parse MOVE:r,c */
                    String[] parts = line.substring(5).split(",");
                    int r = Integer.parseInt(parts[0]);
                    int c = Integer.parseInt(parts[1]);
                    server.handleMove(this, r, c);

                } else if (line.equals("PASS")) {
                    server.handlePass(this);

                } else if (line.equals("RESTART_REQ")) {
                    server.handleRestartRequest(this);
                }
            }
        } catch (IOException e) {
            System.out.println("[Server] " + playerName + " disconnected unexpectedly.");
        } finally {
            server.handleDisconnect(this);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
