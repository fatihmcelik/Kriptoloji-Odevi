package com.kriptoloji.crypto;

public enum KeyExchangeAlgorithm {
    RSA_2048("RSA-2048"),
    DH_2048("Diffie-Hellman (2048-bit)"),
    ECDH("ECC (ECDH - secp256r1)"),
    KYBER_768("Kyber (ML-KEM-768)");

    private final String displayName;

    KeyExchangeAlgorithm(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}