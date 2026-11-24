package com.kriptoloji.server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private static final int PORT = 5000;
    private static final byte TYPE_TEXT = 1;
    private static final byte TYPE_PDF = 2;
    private static final int MAX_PAYLOAD_SIZE = 50 * 1024 * 1024; // 50 MB sinir
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("[SUNUCU] Port " + PORT + " dinleniyor (GUVENSIZ MOD - METIN & PDF)...");
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

    static void forwardText(String senderName, byte[] payload, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendText(senderName, payload);
            }
        }
    }

    static void forwardPdf(String senderName, String fileName, byte[] payload, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendPdf(senderName, fileName, payload);
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private DataOutputStream out;
        private String clientName = "Bilinmeyen";

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))
            ) {
                this.out = dos;
                this.clientName = in.readUTF();
                System.out.println("[BAGLANDI] " + clientName + " (" + socket.getInetAddress().getHostAddress() + ")");

                while (true) {
                    byte type = in.readByte();

                    if (type == TYPE_TEXT) {
                        int length = in.readInt();
                        if (length < 0 || length > MAX_PAYLOAD_SIZE) break;

                        byte[] data = new byte[length];
                        in.readFully(data);

                        String message = new String(data, StandardCharsets.UTF_8);
                        System.out.println("[METIN] " + clientName + ": " + message + " (" + length + " byte)");
                        forwardText(clientName, data, this);

                    } else if (type == TYPE_PDF) {
                        String rawFileName = in.readUTF();
                        String safeFileName = Paths.get(rawFileName).getFileName().toString();
                        int length = in.readInt();
                        if (length < 0 || length > MAX_PAYLOAD_SIZE) break;

                        byte[] fileBytes = new byte[length];
                        in.readFully(fileBytes);

                        Path saveDir = Paths.get("server_files");
                        Files.createDirectories(saveDir);
                        Path savedPath = saveDir.resolve(System.currentTimeMillis() + "_" + safeFileName);
                        Files.write(savedPath, fileBytes);

                        System.out.println("[PDF] " + clientName + " -> " + safeFileName
                                + " (" + length + " byte) | Kaydedildi: " + savedPath);
                        forwardPdf(clientName, safeFileName, fileBytes, this);
                    }
                }
            } catch (EOFException e) {
                System.out.println("[AYRILDI] " + clientName);
            } catch (IOException e) {
                System.out.println("[BAGLANTI KOPTU] " + clientName + " (" + e.getMessage() + ")");
            } finally {
                clients.remove(this);
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        synchronized void sendText(String senderName, byte[] payload) {
            try {
                if (out == null) return;
                out.writeByte(TYPE_TEXT);
                out.writeUTF(senderName);
                out.writeInt(payload.length);
                out.write(payload);
                out.flush();
            } catch (IOException e) {
                System.err.println("[ILETIM HATASI] " + clientName + ": " + e.getMessage());
            }
        }

        synchronized void sendPdf(String senderName, String fileName, byte[] payload) {
            try {
                if (out == null) return;
                out.writeByte(TYPE_PDF);
                out.writeUTF(senderName);
                out.writeUTF(fileName);
                out.writeInt(payload.length);
                out.write(payload);
                out.flush();
            } catch (IOException e) {
                System.err.println("[PDF ILETIM HATASI] " + clientName + ": " + e.getMessage());
            }
        }
    }
}