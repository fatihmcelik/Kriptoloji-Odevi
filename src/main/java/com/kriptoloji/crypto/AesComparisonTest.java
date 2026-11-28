package com.kriptoloji.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public class AesComparisonTest {

    public static void main(String[] args) throws Exception {
        AesEngine manualAes = new ManualAesEngine();
        AesEngine standardAes = new StandardAesEngine();

        // 16 byte (128-bit) sabit test anahtarı ve 16 byte IV
        byte[] key = "Kriptoloji2026!!".getBytes(StandardCharsets.UTF_8); // 16 byte
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        String sampleText = "KTU Of Teknoloji Fakultesi - Kriptoloji Projesi AES-128 Test Mesaji (Turkce karakterler: ğüşiöç)";
        byte[] plaintext = sampleText.getBytes(StandardCharsets.UTF_8);

        System.out.println("=================================================================");
        System.out.println("1. DOĞRULUK TESTİ (MANUEL AES-128 vs JAVA JCA KÜTÜPHANELİ AES)");
        System.out.println("=================================================================");
        System.out.println("Orijinal Metin Uzunluğu : " + plaintext.length + " byte");
        System.out.println("AES Anahtarı (Hex)      : " + bytesToHex(key));
        System.out.println("IV Vektörü (Hex)        : " + bytesToHex(iv));

        // Her iki motorla şifreleme
        byte[] manualCipher = manualAes.encrypt(plaintext, key, iv);
        byte[] standardCipher = standardAes.encrypt(plaintext, key, iv);

        System.out.println("\nManuel AES Şifreli (Hex)     : " + bytesToHex(manualCipher));
        System.out.println("Kütüphaneli AES Şifreli (Hex): " + bytesToHex(standardCipher));

        boolean isCipherIdentical = Arrays.equals(manualCipher, standardCipher);
        System.out.println("\n[SONUÇ 1] Şifreli çıktılar byte-byte birebir aynı mı? -> " + (isCipherIdentical ? "EVET (BAŞARILI)" : "HAYIR (HATA!)"));

        // Çapraz Deşifreleme Testi (Manuel şifrelediğini Kütüphaneli çözer mi, Kütüphaneli şifrelediğini Manuel çözer mi?)
        byte[] decryptedByStandard = standardAes.decrypt(manualCipher, key, iv);
        byte[] decryptedByManual = manualAes.decrypt(standardCipher, key, iv);

        boolean isCrossDecryptOk = Arrays.equals(plaintext, decryptedByStandard) && Arrays.equals(plaintext, decryptedByManual);
        System.out.println("[SONUÇ 2] Çapraz deşifreleme (Birbirinin şifresini çözme) -> " + (isCrossDecryptOk ? "BAŞARILI" : "HATA!"));
        System.out.println("Çözülen Metin: " + new String(decryptedByManual, StandardCharsets.UTF_8));

        System.out.println("\n=================================================================");
        System.out.println("2. FARK ANALİZİ: PADDING, ÇALIŞMA MODU (ECB vs CBC) VE IV ETKİSİ");
        System.out.println("=================================================================");

        // A) Padding Etkisi
        System.out.println("A) PKCS#7 Padding Boyut Değişimi:");
        System.out.println("   - Ham veri boyutu     : " + plaintext.length + " byte");
        System.out.println("   - Şifreli veri boyutu : " + manualCipher.length + " byte (16'nın en yakın üst katına tamamlandı)");

        // B) IV Etkisi (Aynı metin, aynı anahtar, farklı IV)
        byte[] iv2 = new byte[16];
        new SecureRandom().nextBytes(iv2);
        byte[] manualCipherDiffIv = manualAes.encrypt(plaintext, key, iv2);
        System.out.println("\nB) Farklı IV Kullanımı (Aynı metin + Aynı anahtar + Farklı IV):");
        System.out.println("   - IV-1 ile ilk 16 byte: " + bytesToHex(Arrays.copyOf(manualCipher, 16)));
        System.out.println("   - IV-2 ile ilk 16 byte: " + bytesToHex(Arrays.copyOf(manualCipherDiffIv, 16)));
        System.out.println("   - Çıktılar farklı mı?  : " + (!Arrays.equals(manualCipher, manualCipherDiffIv) ? "EVET (Tekrar saldırılarına karşı korumalı)" : "HAYIR"));

        // C) Çalışma Modu Farkı: ECB vs CBC (Tekrarlayan bloklarda örüntü sızıntısı)
        byte[] repeatingBlockData = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBB".getBytes(StandardCharsets.UTF_8); // 32 byte 'A', 16 byte 'B'
        Cipher ecbCipher = Cipher.getInstance("AES/ECB/NoPadding");
        ecbCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        byte[] ecbResult = ecbCipher.doFinal(repeatingBlockData);
        byte[] cbcResult = manualAes.encrypt(repeatingBlockData, key, iv);

        System.out.println("\nC) Çalışma Modu Karşılaştırması (32 byte aynı 'A' karakteri için):");
        System.out.println("   - ECB Blok 1 (0-16)   : " + bytesToHex(Arrays.copyOfRange(ecbResult, 0, 16)));
        System.out.println("   - ECB Blok 2 (16-32)  : " + bytesToHex(Arrays.copyOfRange(ecbResult, 16, 32)) + "  <-- AYNI! (Örüntü sızdırır)");
        System.out.println("   - CBC Blok 1 (0-16)   : " + bytesToHex(Arrays.copyOfRange(cbcResult, 0, 16)));
        System.out.println("   - CBC Blok 2 (16-32)  : " + bytesToHex(Arrays.copyOfRange(cbcResult, 16, 32)) + "  <-- TAMAMEN FARKLI! (Güvenli)");

        System.out.println("\n=================================================================");
        System.out.println("3. PERFORMANS KARŞILAŞTIRMASI (1 MB PDF SİMÜLASYONU - 10 ÖLÇÜM ORTALAMASI)");
        System.out.println("=================================================================");
        byte[] largeData = new byte[1024 * 1024]; // 1 MB test verisi
        new SecureRandom().nextBytes(largeData);

        // JVM Isınma (Warm-up)
        manualAes.encrypt(largeData, key, iv);
        standardAes.encrypt(largeData, key, iv);

        long manualTotalNs = 0;
        long standardTotalNs = 0;
        int iterations = 10;

        for (int i = 0; i < iterations; i++) {
            long t1 = System.nanoTime();
            manualAes.encrypt(largeData, key, iv);
            long t2 = System.nanoTime();
            manualTotalNs += (t2 - t1);

            long t3 = System.nanoTime();
            standardAes.encrypt(largeData, key, iv);
            long t4 = System.nanoTime();
            standardTotalNs += (t4 - t3);
        }

        double manualAvgMs = (manualTotalNs / (double) iterations) / 1_000_000.0;
        double standardAvgMs = (standardTotalNs / (double) iterations) / 1_000_000.0;

        System.out.printf("Manuel AES-128 Ortalama Süre (1 MB)      : %.3f ms%n", manualAvgMs);
        System.out.printf("Kütüphaneli AES-128 Ortalama Süre (1 MB) : %.3f ms%n", standardAvgMs);
        System.out.printf("Hız Farkı Oranı                          : Kütüphaneli AES yaklaşık %.1f kat daha hızlı (AES-NI donanım desteği sayesinde)%n", (manualAvgMs / standardAvgMs));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}