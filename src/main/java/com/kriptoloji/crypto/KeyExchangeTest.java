package com.kriptoloji.crypto;

public class KeyExchangeTest {

    public static void main(String[] args) throws Exception {
        KeyExchangeService service = new KeyExchangeService();
        int iterations = 10;

        System.out.println("=== 4 YONTEM DOGRULAMA VE 10 OLCUM ORTALAMASI ===");
        System.out.printf("%-28s | %-16s | %-18s | %-15s%n",
                "Algoritma", "Anahtar Uret(ms)", "Anahtar Degis(ms)", "Paket Boyutu(B)");
        System.out.println("---------------------------------------------------------------------------------------");

        for (KeyExchangeAlgorithm algo : KeyExchangeAlgorithm.values()) {
            double totalKeyGen = 0;
            double totalExchange = 0;
            int packetSize = 0;
            String lastHexKey = "";

            for (int i = 0; i < iterations; i++) {
                KeyExchangeResult res = service.executeFullHandshake(algo);
                totalKeyGen += res.getKeyGenTimeMs();
                totalExchange += res.getExchangeTimeMs();
                packetSize = res.getPacketSizeBytes();
                lastHexKey = res.getAesKeyHex();
            }

            System.out.printf("%-28s | %16.3f | %18.3f | %15d  [AES: %s]%n",
                    algo.getDisplayName(),
                    totalKeyGen / iterations,
                    totalExchange / iterations,
                    packetSize,
                    lastHexKey);
        }
    }
}