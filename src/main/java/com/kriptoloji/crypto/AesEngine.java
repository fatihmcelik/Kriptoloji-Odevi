package com.kriptoloji.crypto;

public interface AesEngine {
    byte[] encrypt(byte[] plaintext, byte[] key, byte[] iv) throws Exception;
    byte[] decrypt(byte[] ciphertext, byte[] key, byte[] iv) throws Exception;
}