package com.kriptoloji.client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 5000;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        System.out.print("Istemci adi girin (orn. Istemci1): ");
        String name = scanner.nextLine();

        try (
            Socket socket = new Socket(SERVER_IP, PORT);
            PrintWriter out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            out.println(name);
            System.out.println("[BAGLANDI] Sunucuya baglanildi. Mesaj yazabilirsiniz (cikmak icin 'exit'):");

            Thread listener = new Thread(() -> {
                try {
                    String incoming;
                    while ((incoming = in.readLine()) != null) {
                        System.out.println("\n<< " + incoming);
                        System.out.print(">> ");
                    }
                } catch (IOException e) {
                    System.out.println("\n[SUNUCU BAGLANTISI KESILDI]");
                }
            });
            listener.setDaemon(true);
            listener.start();

            while (true) {
                System.out.print(">> ");
                String text = scanner.nextLine();
                if ("exit".equalsIgnoreCase(text)) break;
                out.println(text);
            }
        } catch (IOException e) {
            System.err.println("[HATA] Sunucuya baglanilamadi: " + e.getMessage());
        }
    }
}