package com.kriptoloji.server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private static final int PORT = 5000;
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("[SUNUCU] Port " + PORT + " dinleniyor (GUVENSIZ MOD)...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("[HATA] Sunucu baslatilamadi: " + e.getMessage());
        }
    }

    static void forwardMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private String clientName;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
            ) {
                this.out = writer;
                this.clientName = in.readLine();
                System.out.println("[BAGLANDI] " + clientName + " (" + socket.getInetAddress().getHostAddress() + ")");

                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("[ILETILIYOR] " + clientName + ": " + msg);
                    forwardMessage(clientName + ": " + msg, this);
                }
            } catch (IOException e) {
                System.out.println("[AYRILDI] " + clientName);
            } finally {
                clients.remove(this);
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }
    }
}