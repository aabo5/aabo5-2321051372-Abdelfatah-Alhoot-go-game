package com.mycompany.gogameclient;

// Main entry point for the Go game client
public class GoGameClient {

    public static void main(String[] args) {
        // Start the game by showing the start screen
        java.awt.EventQueue.invokeLater(() -> new StartScreen().setVisible(true));
    }
}
