/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gogameclient;

/**
 *
 * @author abdel
 */
import java.net.*;
import java.io.*;

public class test {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 5000);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Hello from client!");

            String response = in.readLine();
            System.out.println("Server says: " + response);

            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}