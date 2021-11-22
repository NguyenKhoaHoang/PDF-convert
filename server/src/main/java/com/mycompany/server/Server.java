/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author ACER
 */
public class Server {
    
    private final int SERVER_PORT = 9004;
    private ServerSocket serverSocket = null;
    
    private void listen() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server is listening on port: " + SERVER_PORT);
            while(true) {
                System.out.println("Waiting for client");
                Socket socket = serverSocket.accept();
                // Need a thread here
                PdfConverterServerThread pdfConverterServer = new PdfConverterServerThread(socket);
                pdfConverterServer.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot create socket server on port: " + SERVER_PORT);
        }
    }
    
    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.listen();
    }
    
}
