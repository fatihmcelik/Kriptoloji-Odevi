package com.kriptoloji.client;

import java.nio.charset.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 5000;
    private static final byte TYPE_TEXT = 1;
    private static final byte TYPE_PDF = 2;

    public static void main(String[] args) {
      Charset consoleCharset = (System.console() != null)
        ? System.console().charset()
        : Charset.defaultCharset();

try (Scanner scanner = new Scanner(System.in, consoleCharset)) {
        System.out.print("Istemci adi girin (orn. Istemci1): ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) name = "Istemci";
        final String clientName = name;

        try (
            Socket socket = new Socket(SERVER_IP, PORT);
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))
        ) {
            out.writeUTF(clientName);
            out.flush();

            System.out.println("[BAGLANDI] Sunucuya baglanildi.");
            System.out.println("Komutlar: Metin yazip Enter'a basin | PDF icin: /pdf C:\\yol\\dosya.pdf | Cikis: exit");

            Thread listener = new Thread(() -> {
                try {
                    while (true) {
                        byte type = in.readByte();
                        String sender = in.readUTF();

                        if (type == TYPE_TEXT) {
                            int len = in.readInt();
                            byte[] data = new byte[len];
                            in.readFully(data);
                            String msg = new String(data, StandardCharsets.UTF_8);
                            System.out.println("\n<< [" + sender + "]: " + msg);
                            System.out.print(">> ");

                        } else if (type == TYPE_PDF) {
                            String fileName = in.readUTF();
                            int len = in.readInt();
                            byte[] fileBytes = new byte[len];
                            in.readFully(fileBytes);

                            Path outDir = Paths.get("received_" + clientName);
                            Files.createDirectories(outDir);
                            Path outFile = outDir.resolve(fileName);
                            Files.write(outFile, fileBytes);

                            System.out.println("\n<< [" + sender + "] PDF gonderdi: " + fileName
                                    + " (" + len + " byte) -> Kaydedildi: " + outFile.toAbsolutePath());
                            System.out.print(">> ");
                        }
                    }
                } catch (IOException e) {
                    System.out.println("\n[SUNUCU BAGLANTISI KESILDI]");
                }
            });
            listener.setDaemon(true);
            listener.start();

            while (true) {
                System.out.print(">> ");
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input.trim())) break;

                if (input.startsWith("/pdf ")) {
                    String rawPath = input.substring(5).trim().replace("\"", "");
                    Path pdfPath = Paths.get(rawPath);

                    if (!Files.exists(pdfPath) || !Files.isRegularFile(pdfPath)) {
                        System.out.println("[UYARI] Dosya bulunamadi: " + pdfPath);
                        continue;
                    }
                    if (!pdfPath.getFileName().toString().toLowerCase().endsWith(".pdf")) {
                        System.out.println("[UYARI] Sadece .pdf dosyalari gonderilebilir.");
                        continue;
                    }

                    byte[] fileBytes = Files.readAllBytes(pdfPath);
                    out.writeByte(TYPE_PDF);
                    out.writeUTF(pdfPath.getFileName().toString());
                    out.writeInt(fileBytes.length);
                    out.write(fileBytes);
                    out.flush();
                    System.out.println("[GONDERILDI] PDF: " + pdfPath.getFileName() + " (" + fileBytes.length + " byte)");
                } else {
                    byte[] msgBytes = input.getBytes(StandardCharsets.UTF_8);
                    out.writeByte(TYPE_TEXT);
                    out.writeInt(msgBytes.length);
                    out.write(msgBytes);
                    out.flush();
                }
            }
        } catch (IOException e) {
            System.err.println("[HATA] Sunucuya baglanilamadi: " + e.getMessage());
        }
    }
}
}