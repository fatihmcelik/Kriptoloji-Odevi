package com.kriptoloji.crypto;

import java.util.HexFormat;

public class KeyExchangeResult {
    private final KeyExchangeAlgorithm algorithm;
    private final byte[] aesKey;
    private final double keyGenTimeMs;
    private final double exchangeTimeMs;
    private final int packetSizeBytes;

    public KeyExchangeResult(
            KeyExchangeAlgorithm algorithm,
            byte[] aesKey,
            double keyGenTimeMs,
            double exchangeTimeMs,
            int packetSizeBytes) {
        this.algorithm = algorithm;
        this.aesKey = aesKey;
        this.keyGenTimeMs = keyGenTimeMs;
        this.exchangeTimeMs = exchangeTimeMs;
        this.packetSizeBytes = packetSizeBytes;
    }

    public KeyExchangeAlgorithm getAlgorithm() {
        return algorithm;
    }

    public byte[] getAesKey() {
        return aesKey;
    }

    public String getAesKeyHex() {
        return HexFormat.of().withUpperCase().formatHex(aesKey);
    }

    public double getKeyGenTimeMs() {
        return keyGenTimeMs;
    }

    public double getExchangeTimeMs() {
        return exchangeTimeMs;
    }

    public int getPacketSizeBytes() {
        return packetSizeBytes;
    }
}