/*
 * NetworkClient.java — Manages the TCP socket connection to GoServer.
 * Runs a background listener thread that reads server messages and
 * dispatches them to the currently registered ServerMessageListener.
 * All listener callbacks fire on the network thread; the UI classes
 * are responsible for wrapping updates in SwingUtilities.invokeLater().
 */
package com.mycompany.gogameclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Client-side networking class for the Go game.
 *
 * @author abdel
 */
public class NetworkClient {

    /* ========================================= */
    /* Callback interface for server messages */
    /* ========================================= */
    public interface ServerMessageListener {
        /** Called when server assigns a color (BLACK / WHITE) */
        void onWelcome(String color);

        /** Called when both players are connected and the game begins */
        void onGameStarted();

        /** Called with the full 81-char board string after every move */
        void onBoardUpdate(String boardData);

        /** Called to indicate whose turn it is (BLACK / WHITE) */
        void onTurnChange(String whoseTurn);

        /** Called when a move was accepted — for move-history display */
        void onMoveOk(int row, int col, String color);

        /** Called when a pass was accepted — for move-history display */
        void onPassOk(String color);

        /** Called when the client's move was rejected by the server */
        void onInvalidMove();

        /** Called when the game ends (winner = BLACK / WHITE / DRAW) */
        void onGameOver(String winner);

        /** Called when the opponent disconnects mid-game */
        void onOpponentDisconnected();

        /** Called for any generic server message */
        void onMessage(String text);
    }

    /* ========================================= */
    /* Fields */
    /* ========================================= */
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ServerMessageListener listener;
    private Thread listenerThread;
    private volatile boolean running;

    /* ========================================= */
    /* Constructor — connects to the server */
    /* Throws IOException if connection fails */
    /* ========================================= */
    public NetworkClient(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;

        /* background daemon thread that reads server messages */
        listenerThread = new Thread(this::listenLoop, "NetworkClient-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /* ========================================= */
    /* Set / swap the active message listener */
    /* ========================================= */
    public void setListener(ServerMessageListener newListener) {
        this.listener = newListener;
    }

    /* ========================================= */
    /* Outgoing message methods */
    /* ========================================= */

    /** Request placing a stone at (row, col) */
    public void sendMove(int row, int col) {
        out.println("MOVE:" + row + "," + col);
    }

    /** Pass this turn */
    public void sendPass() {
        out.println("PASS");
    }

    /** Request a rematch after game over */
    public void sendRestart() {
        out.println("RESTART_REQ");
    }

    /** Cleanly close the connection */
    public void disconnect() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            /* best-effort close */
        }
    }

    /* ========================================= */
    /* Background listener loop */
    /* Reads one line at a time and dispatches */
    /* to the registered ServerMessageListener */
    /* ========================================= */
    private void listenLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                parseAndDispatch(line);
            }
        } catch (IOException e) {
            /* socket closed — notify listener if still active */
            if (running && listener != null) {
                listener.onOpponentDisconnected();
            }
        } finally {
            running = false;
        }
    }

    /* ========================================= */
    /* Protocol parser */
    /*                                           */
    /* Messages from server: */
    /* WELCOME:BLACK | WELCOME:WHITE */
    /* GAME_STARTED */
    /* UPDATE:<81-char board> */
    /* TURN:BLACK | TURN:WHITE */
    /* MOVE_OK:r,c,COLOR */
    /* PASS_OK:COLOR */
    /* INVALID_MOVE */
    /* GAME_OVER:BLACK | WHITE | DRAW */
    /* OPPONENT_DISCONNECTED */
    /* MESSAGE:text */
    /* ========================================= */
    private void parseAndDispatch(String line) {
        if (listener == null) {
            return;
        }

        if (line.startsWith("WELCOME:")) {
            listener.onWelcome(line.substring(8));

        } else if (line.equals("GAME_STARTED")) {
            listener.onGameStarted();

        } else if (line.startsWith("UPDATE:")) {
            listener.onBoardUpdate(line.substring(7));

        } else if (line.startsWith("TURN:")) {
            listener.onTurnChange(line.substring(5));

        } else if (line.startsWith("MOVE_OK:")) {
            /* format: MOVE_OK:r,c,COLOR */
            String[] parts = line.substring(8).split(",");
            int r = Integer.parseInt(parts[0]);
            int c = Integer.parseInt(parts[1]);
            String color = parts[2];
            listener.onMoveOk(r, c, color);

        } else if (line.startsWith("PASS_OK:")) {
            listener.onPassOk(line.substring(8));

        } else if (line.equals("INVALID_MOVE")) {
            listener.onInvalidMove();

        } else if (line.startsWith("GAME_OVER:")) {
            listener.onGameOver(line.substring(10));

        } else if (line.equals("OPPONENT_DISCONNECTED")) {
            listener.onOpponentDisconnected();

        } else if (line.startsWith("MESSAGE:")) {
            listener.onMessage(line.substring(8));
        }
    }
}
