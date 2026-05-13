/*
 * GoGameClient.java — Application entry point.
 * Launches the StartScreen so the player can enter
 * a server IP and connect to a Go game match.
 */
package com.mycompany.gogameclient;

/**
 *
 * @author abdel
 */
public class GoGameClient {

    public static void main(String[] args) {
        /* Launch the Start Screen on the Event Dispatch Thread */
        java.awt.EventQueue.invokeLater(() -> new StartScreen().setVisible(true));
    }
}
