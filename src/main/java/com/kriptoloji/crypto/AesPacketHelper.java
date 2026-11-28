package com.kriptoloji.crypto;

import java.security.SecureRandom;
import java.util.Arrays;

public class AesPacketHelper {

    private static final int IV_LENGTH = 16;


    public static byte[] encryptWithIv(AesEngine engine, byte[] plaintext, byte[] key) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        byte[] ciphertext = engine.encrypt(plaintext, key, iv);

        byte[] packet = new byte[IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, packet, 0, IV_LENGTH);
        System.arraycopy(ciphertext, 0, packet, IV_LENGTH, ciphertext.length);
        return packet;
    }


    public static byte[] decryptWithIv(AesEngine engine, byte[] packet, byte[] key) throws Exception {
        if (packet == null || packet.length <= IV_LENGTH) {
            throw new IllegalArgumentException("Gelen şifreli paket geçersiz veya eksik (IV bulunamadı).");
        }

        byte[] iv = Arrays.copyOfRange(packet, 0, IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(packet, IV_LENGTH, packet.length);

        return engine.decrypt(ciphertext, key, iv);
    }
}