package com.mycompany.gogameserver;

import com.mycompany.gogameclient.GameLogic;

// Represents a single active game between two players
public class GameSession {

    // Game logic — the single source of truth for this session
    private final GameLogic game = new GameLogic();

    // The two connected players in this session
    private final ClientHandler playerBlack;
    private final ClientHandler playerWhite;

    // Turn tracking
    private int currentTurn = GameLogic.BLACK;
    private int consecutivePasses = 0;
    private boolean gameInProgress = false;

    // Restart voting
    private boolean blackWantsRestart = false;
    private boolean whiteWantsRestart = false;

    public GameSession(ClientHandler playerBlack, ClientHandler playerWhite) {
        this.playerBlack = playerBlack;
        this.playerWhite = playerWhite;
    }

    // Initialize / reset and start a new game
    public synchronized void startNewGame() {
        game.resetBoard();
        currentTurn = GameLogic.BLACK;
        consecutivePasses = 0;
        gameInProgress = true;
        blackWantsRestart = false;
        whiteWantsRestart = false;

        broadcast("GAME_STARTED");
        broadcastBoard();
        broadcastTurn();

        System.out.println("[Session] === New game started! ===");
    }

    // Handle a MOVE request from a client
    public synchronized void handleMove(ClientHandler sender, int r, int c) {
        // reject if game is not in progress
        if (!gameInProgress) {
            sender.sendMessage("INVALID_MOVE");
            return;
        }

        // reject if it is not this player's turn
        if (sender.getPlayerColor() != currentTurn) {
            sender.sendMessage("INVALID_MOVE");
            return;
        }

        // validate and apply the move via GameLogic
        boolean valid = game.placeStone(r, c, currentTurn);
        if (!valid) {
            sender.sendMessage("INVALID_MOVE");
            return;
        }

        // move accepted — reset pass counter
        consecutivePasses = 0;

        // broadcast the move confirmation, updated board, and next turn
        String colorName = (currentTurn == GameLogic.BLACK) ? "BLACK" : "WHITE";
        broadcast("MOVE_OK:" + r + "," + c + "," + colorName);
        broadcastBoard();
        advanceTurn();
        broadcastTurn();

        System.out.println("[Session] " + colorName + " placed stone at (" + r + "," + c + ")");
        game.printBoard();
    }

    // Handle a PASS request from a client
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

        System.out.println("[Session] " + colorName + " passed. Consecutive passes: " + consecutivePasses);

        // two consecutive passes end the game
        if (consecutivePasses >= 2) {
            endGame();
            return;
        }

        advanceTurn();
        broadcastTurn();
    }

    // Handle a RESTART_REQ from a client
    public synchronized void handleRestartRequest(ClientHandler sender) {
        if (sender.getPlayerColor() == GameLogic.BLACK) {
            blackWantsRestart = true;
        } else {
            whiteWantsRestart = true;
        }

        System.out.println("[Session] " + sender.getPlayerName() + " wants to restart.");

        // if both players agree, start a new game
        if (blackWantsRestart && whiteWantsRestart) {
            System.out.println("[Session] Both players agreed — restarting game.");
            startNewGame();
        } else {
            sender.sendMessage("MESSAGE:Waiting for opponent to accept rematch...");
        }
    }

    // Handle a player disconnecting
    public synchronized void handleDisconnect(ClientHandler sender) {
        gameInProgress = false;

        // notify the other player
        ClientHandler other = getOpponent(sender);
        if (other != null) {
            other.sendMessage("OPPONENT_DISCONNECTED");
        }

        System.out.println("[Session] " + sender.getPlayerName() + " disconnected. Match ended.");
    }

    // End the game and determine the winner
    private void endGame() {
        gameInProgress = false;

        int blackScore = game.countTerritory(GameLogic.BLACK);
        int whiteScore = game.countTerritory(GameLogic.WHITE);

        System.out.println("[Session] Game Over! BLACK=" + blackScore + " WHITE=" + whiteScore);

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

    // Helper — advance the turn to next player
    private void advanceTurn() {
        currentTurn = (currentTurn == GameLogic.BLACK) ? GameLogic.WHITE : GameLogic.BLACK;
    }

    // Helper — broadcast a message to both
    private void broadcast(String msg) {
        if (playerBlack != null)
            playerBlack.sendMessage(msg);
        if (playerWhite != null)
            playerWhite.sendMessage(msg);
    }

    // Helper — send current board to both
    private void broadcastBoard() {
        broadcast("UPDATE:" + game.serializeBoard());
    }

    // Helper — send current turn to both
    private void broadcastTurn() {
        String turn = (currentTurn == GameLogic.BLACK) ? "BLACK" : "WHITE";
        broadcast("TURN:" + turn);
    }

    // Helper — get the opponent of a player
    private ClientHandler getOpponent(ClientHandler player) {
        if (player == playerBlack)
            return playerWhite;
        if (player == playerWhite)
            return playerBlack;
        return null;
    }
}
