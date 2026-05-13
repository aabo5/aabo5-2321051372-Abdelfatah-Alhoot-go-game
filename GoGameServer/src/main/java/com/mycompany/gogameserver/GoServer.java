/*
 * GoServer.java — Authoritative multiplayer Go game server.
 *
 * Standalone console application (no GUI).
 * Binds to port 5000, waits for exactly TWO clients, then manages
 * the game loop: validates moves via GameLogic, broadcasts state,
 * tracks turns, detects game-over (two consecutive passes), and
 * handles rematch requests.
 *
 * Protocol (server → client):
 *   WELCOME:BLACK | WELCOME:WHITE
 *   GAME_STARTED
 *   UPDATE:<81-char board>
 *   TURN:BLACK | TURN:WHITE
 *   MOVE_OK:r,c,COLOR
 *   PASS_OK:COLOR
 *   INVALID_MOVE
 *   GAME_OVER:BLACK | WHITE | DRAW
 *   OPPONENT_DISCONNECTED
 *   MESSAGE:text
 */
package com.mycompany.gogameserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import com.mycompany.gogameclient.GameLogic;

/**
 * Authoritative Go game server — source of truth for all game state.
 *
 * @author abdel
 */
public class GoServer {

    private static final int PORT = 5000;

    /* game logic — the single source of truth */
    private final GameLogic game = new GameLogic();

    /* the two connected players */
    private ClientHandler playerBlack;
    private ClientHandler playerWhite;

    /* turn tracking */
    private int currentTurn = GameLogic.BLACK;
    private int consecutivePasses = 0;
    private boolean gameInProgress = false;

    /* restart voting */
    private boolean blackWantsRestart = false;
    private boolean whiteWantsRestart = false;

    /* ========================================= */
    /* Entry point */
    /* ========================================= */
    public static void main(String[] args) {
        new GoServer().start();
    }

    /* ========================================= */
    /* Start the server and wait for players */
    /* ========================================= */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("=========================================");
            System.out.println("  GO Game Server started on port " + PORT);
            System.out.println("  Waiting for 2 players to connect...");
            System.out.println("=========================================");

            /* accept player 1 (BLACK) */
            Socket s1 = serverSocket.accept();
            playerBlack = new ClientHandler(s1, this, GameLogic.BLACK);
            new Thread(playerBlack, "Player-BLACK").start();
            System.out.println("[Server] Player 1 (BLACK) connected.");
            playerBlack.sendMessage("MESSAGE:Waiting for opponent...");

            /* accept player 2 (WHITE) */
            Socket s2 = serverSocket.accept();
            playerWhite = new ClientHandler(s2, this, GameLogic.WHITE);
            new Thread(playerWhite, "Player-WHITE").start();
            System.out.println("[Server] Player 2 (WHITE) connected.");

            /* both players are in — start the game */
            startNewGame();

            /* keep the main thread alive while game threads run */
            Thread.currentThread().join();

        } catch (IOException e) {
            System.err.println("[Server] Fatal error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("[Server] Server shutting down.");
        }
    }

    /* ========================================= */
    /* Initialize / reset and start a new game */
    /* ========================================= */
    private synchronized void startNewGame() {
        game.resetBoard();
        currentTurn = GameLogic.BLACK;
        consecutivePasses = 0;
        gameInProgress = true;
        blackWantsRestart = false;
        whiteWantsRestart = false;

        broadcast("GAME_STARTED");
        broadcastBoard();
        broadcastTurn();

        System.out.println("[Server] === New game started! ===");
    }

    /* ========================================= */
    /* Handle a MOVE request from a client */
    /* ========================================= */
    public synchronized void handleMove(ClientHandler sender, int r, int c) {
        /* reject if game is not in progress */
        if (!gameInProgress) {
            sender.sendMessage("INVALID_MOVE");
            return;
        }

        /* reject if it is not this player's turn */
        if (sender.getPlayerColor() != currentTurn) {
            sender.sendMessage("INVALID_MOVE");
            return;
        }

        /* validate and apply the move via GameLogic */
        boolean valid = game.placeStone(r, c, currentTurn);
        if (!valid) {
            sender.sendMessage("INVALID_MOVE");
            return;
        }

        /* move accepted — reset pass counter */
        consecutivePasses = 0;

        /* broadcast the move confirmation, updated board, and next turn */
        String colorName = (currentTurn == GameLogic.BLACK) ? "BLACK" : "WHITE";
        broadcast("MOVE_OK:" + r + "," + c + "," + colorName);
        broadcastBoard();
        advanceTurn();
        broadcastTurn();

        System.out.println("[Server] " + colorName + " placed stone at (" + r + "," + c + ")");
        game.printBoard();
    }

    /* ========================================= */
    /* Handle a PASS request from a client */
    /* ========================================= */
    public synchronized void handlePass(ClientHandler sender) {
        if (!gameInProgress) {
            return;
        }
        if (sender.getPlayerColor() != currentTurn) {
            sender.sendMessage("INVALID_MOVE");
            return;
        }

        consecutivePasses++;
        String colorName = sender.getPlayerName();
        broadcast("PASS_OK:" + colorName);

        System.out.println("[Server] " + colorName + " passed. Consecutive passes: " + consecutivePasses);

        /* two consecutive passes end the game */
        if (consecutivePasses >= 2) {
            endGame();
            return;
        }

        advanceTurn();
        broadcastTurn();
    }

    /* ========================================= */
    /* Handle a RESTART_REQ from a client */
    /* ========================================= */
    public synchronized void handleRestartRequest(ClientHandler sender) {
        if (sender.getPlayerColor() == GameLogic.BLACK) {
            blackWantsRestart = true;
        } else {
            whiteWantsRestart = true;
        }

        System.out.println("[Server] " + sender.getPlayerName() + " wants to restart.");

        /* if both players agree, start a new game */
        if (blackWantsRestart && whiteWantsRestart) {
            System.out.println("[Server] Both players agreed — restarting game.");
            startNewGame();
        } else {
            sender.sendMessage("MESSAGE:Waiting for opponent to accept rematch...");
        }
    }

    /* ========================================= */
    /* Handle a player disconnecting */
    /* ========================================= */
    public synchronized void handleDisconnect(ClientHandler sender) {
        gameInProgress = false;

        /* notify the other player */
        ClientHandler other = getOpponent(sender);
        if (other != null) {
            other.sendMessage("OPPONENT_DISCONNECTED");
        }

        System.out.println("[Server] " + sender.getPlayerName()
                + " disconnected. Match ended.");
    }

    /* ========================================= */
    /* End the game and determine the winner */
    /* ========================================= */
    private void endGame() {
        gameInProgress = false;

        int blackScore = game.countTerritory(GameLogic.BLACK);
        int whiteScore = game.countTerritory(GameLogic.WHITE);

        System.out.println("[Server] Game Over! BLACK=" + blackScore + " WHITE=" + whiteScore);

        String result;
        if (blackScore > whiteScore) {
            result = "BLACK";
        } else if (whiteScore > blackScore) {
            result = "WHITE";
        } else {
            result = "DRAW";
        }

        broadcast("GAME_OVER:" + result);
    }

    /* ========================================= */
    /* Helper — advance the turn to next player */
    /* ========================================= */
    private void advanceTurn() {
        currentTurn = (currentTurn == GameLogic.BLACK) ? GameLogic.WHITE : GameLogic.BLACK;
    }

    /* ========================================= */
    /* Helper — broadcast a message to both */
    /* ========================================= */
    private void broadcast(String msg) {
        if (playerBlack != null)
            playerBlack.sendMessage(msg);
        if (playerWhite != null)
            playerWhite.sendMessage(msg);
    }

    /* ========================================= */
    /* Helper — send current board to both */
    /* ========================================= */
    private void broadcastBoard() {
        broadcast("UPDATE:" + game.serializeBoard());
    }

    /* ========================================= */
    /* Helper — send current turn to both */
    /* ========================================= */
    private void broadcastTurn() {
        String turn = (currentTurn == GameLogic.BLACK) ? "BLACK" : "WHITE";
        broadcast("TURN:" + turn);
    }

    /* ========================================= */
    /* Helper — get the opponent of a player */
    /* ========================================= */
    private ClientHandler getOpponent(ClientHandler player) {
        if (player == playerBlack)
            return playerWhite;
        if (player == playerWhite)
            return playerBlack;
        return null;
    }
}
