package com.kriptoloji.crypto;

import java.util.Arrays;

public class ManualAesEngine implements AesEngine {

    private static final int BLOCK_SIZE = 16; // AES-128 blok boyutu: 16 byte (128 bit)
    private static final int NUM_ROUNDS = 10; // AES-128 tur sayısı: 10 round

    @Override
    public byte[] encrypt(byte[] plaintext, byte[] key, byte[] iv) throws Exception {
        validateKeyAndIv(key, iv);

       
        byte[] paddedData = addPkcs7Padding(plaintext);

        
        byte[] roundKeys = keyExpansion(key);

        byte[] ciphertext = new byte[paddedData.length];
        byte[] previousBlock = Arrays.copyOf(iv, BLOCK_SIZE);

        
        for (int i = 0; i < paddedData.length; i += BLOCK_SIZE) {
            byte[] block = Arrays.copyOfRange(paddedData, i, i + BLOCK_SIZE);

            
            for (int j = 0; j < BLOCK_SIZE; j++) {
                block[j] ^= previousBlock[j];
            }

            byte[] encryptedBlock = encryptBlock(block, roundKeys);
            System.arraycopy(encryptedBlock, 0, ciphertext, i, BLOCK_SIZE);
            previousBlock = encryptedBlock;
        }

        return ciphertext;
    }

    @Override
    public byte[] decrypt(byte[] ciphertext, byte[] key, byte[] iv) throws Exception {
        validateKeyAndIv(key, iv);

        if (ciphertext.length == 0 || ciphertext.length % BLOCK_SIZE != 0) {
            throw new IllegalArgumentException("Şifreli veri uzunluğu 16 byte'ın katı olmalıdır.");
        }

        byte[] roundKeys = keyExpansion(key);
        byte[] decryptedPadded = new byte[ciphertext.length];
        byte[] previousBlock = Arrays.copyOf(iv, BLOCK_SIZE);

      
        for (int i = 0; i < ciphertext.length; i += BLOCK_SIZE) {
            byte[] currentCipherBlock = Arrays.copyOfRange(ciphertext, i, i + BLOCK_SIZE);
            byte[] decryptedBlock = decryptBlock(currentCipherBlock, roundKeys);

           
            for (int j = 0; j < BLOCK_SIZE; j++) {
                decryptedBlock[j] ^= previousBlock[j];
            }

            System.arraycopy(decryptedBlock, 0, decryptedPadded, i, BLOCK_SIZE);
            previousBlock = currentCipherBlock;
        }

        
        return removePkcs7Padding(decryptedPadded);
    }

    private void validateKeyAndIv(byte[] key, byte[] iv) {
        if (key == null || key.length != BLOCK_SIZE) {
            throw new IllegalArgumentException("AES-128 anahtarı kesinlikle 16 byte (128 bit) olmalıdır.");
        }
        if (iv == null || iv.length != BLOCK_SIZE) {
            throw new IllegalArgumentException("IV (Initialization Vector) kesinlikle 16 byte olmalıdır.");
        }
    }

    // PKCS#7 PADDING

    private byte[] addPkcs7Padding(byte[] data) {
        int padLen = BLOCK_SIZE - (data.length % BLOCK_SIZE);
        byte[] padded = Arrays.copyOf(data, data.length + padLen);
        Arrays.fill(padded, data.length, padded.length, (byte) padLen);
        return padded;
    }

    private byte[] removePkcs7Padding(byte[] data) {
        int padLen = data[data.length - 1] & 0xFF;
        if (padLen < 1 || padLen > BLOCK_SIZE || padLen > data.length) {
            throw new IllegalArgumentException("Geçersiz PKCS#7 padding değeri (Anahtar veya IV hatalı olabilir).");
        }
        for (int i = data.length - padLen; i < data.length; i++) {
            if ((data[i] & 0xFF) != padLen) {
                throw new IllegalArgumentException("PKCS#7 padding doğrulaması başarısız.");
            }
        }
        return Arrays.copyOf(data, data.length - padLen);
    }

    // anahtar genişletme

    private byte[] keyExpansion(byte[] key) {
        byte[] expandedKeys = new byte[BLOCK_SIZE * (NUM_ROUNDS + 1)]; // 176 byte
        System.arraycopy(key, 0, expandedKeys, 0, BLOCK_SIZE);

        int bytesGenerated = BLOCK_SIZE;
        int rconIteration = 1;
        byte[] temp = new byte[4];

        while (bytesGenerated < expandedKeys.length) {
            System.arraycopy(expandedKeys, bytesGenerated - 4, temp, 0, 4);

            if (bytesGenerated % BLOCK_SIZE == 0) {
                // 1 byte sola dairesel kaydır
                byte t = temp[0];
                temp[0] = temp[1];
                temp[1] = temp[2];
                temp[2] = temp[3];
                temp[3] = t;

                // S-Box dönüşümü uygula
                for (int i = 0; i < 4; i++) {
                    temp[i] = (byte) AesConstants.SBOX[temp[i] & 0xFF];
                }

                // Rcon ile XOR
                temp[0] ^= (byte) AesConstants.RCON[rconIteration++];
            }

            for (int i = 0; i < 4; i++) {
                expandedKeys[bytesGenerated] = (byte) (expandedKeys[bytesGenerated - BLOCK_SIZE] ^ temp[i]);
                bytesGenerated++;
            }
        }
        return expandedKeys;
    }

    // tek blok şifreleme

    private byte[] encryptBlock(byte[] input, byte[] roundKeys) {
        int[][] state = bytesToState(input);

        
        addRoundKey(state, roundKeys, 0);

        
        for (int round = 1; round < NUM_ROUNDS; round++) {
            subBytes(state);
            shiftRows(state);
            mixColumns(state);
            addRoundKey(state, roundKeys, round);
        }

       
        subBytes(state);
        shiftRows(state);
        addRoundKey(state, roundKeys, NUM_ROUNDS);

        return stateToBytes(state);
    }

    // tek blok deşifreleme

    private byte[] decryptBlock(byte[] input, byte[] roundKeys) {
        int[][] state = bytesToState(input);

        // Tur 10'dan geriye başla
        addRoundKey(state, roundKeys, NUM_ROUNDS);

        
        for (int round = NUM_ROUNDS - 1; round >= 1; round--) {
            invShiftRows(state);
            invSubBytes(state);
            addRoundKey(state, roundKeys, round);
            invMixColumns(state);
        }

        
        invShiftRows(state);
        invSubBytes(state);
        addRoundKey(state, roundKeys, 0);

        return stateToBytes(state);
    }

    //aes matris dönüşümleri 

    private int[][] bytesToState(byte[] bytes) {
        int[][] state = new int[4][4];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                state[row][col] = bytes[col * 4 + row] & 0xFF;
            }
        }
        return state;
    }

    private byte[] stateToBytes(int[][] state) {
        byte[] bytes = new byte[BLOCK_SIZE];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                bytes[col * 4 + row] = (byte) state[row][col];
            }
        }
        return bytes;
    }

    private void addRoundKey(int[][] state, byte[] roundKeys, int round) {
        int offset = round * BLOCK_SIZE;
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                state[row][col] ^= (roundKeys[offset + col * 4 + row] & 0xFF);
            }
        }
    }

    private void subBytes(int[][] state) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                state[row][col] = AesConstants.SBOX[state[row][col]];
            }
        }
    }

    private void invSubBytes(int[][] state) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                state[row][col] = AesConstants.INV_SBOX[state[row][col]];
            }
        }
    }

    private void shiftRows(int[][] state) {
        // 1. satır 1 sola
        int temp = state[1][0];
        state[1][0] = state[1][1];
        state[1][1] = state[1][2];
        state[1][2] = state[1][3];
        state[1][3] = temp;

        // 2. satır 2 sola
        int t0 = state[2][0], t1 = state[2][1];
        state[2][0] = state[2][2];
        state[2][1] = state[2][3];
        state[2][2] = t0;
        state[2][3] = t1;

        // 3. satır 3 sola 
        temp = state[3][3];
        state[3][3] = state[3][2];
        state[3][2] = state[3][1];
        state[3][1] = state[3][0];
        state[3][0] = temp;
    }

    private void invShiftRows(int[][] state) {
        // 1. satır 1 sağa
        int temp = state[1][3];
        state[1][3] = state[1][2];
        state[1][2] = state[1][1];
        state[1][1] = state[1][0];
        state[1][0] = temp;

        // 2. satır 2 sağa
        int t0 = state[2][0], t1 = state[2][1];
        state[2][0] = state[2][2];
        state[2][1] = state[2][3];
        state[2][2] = t0;
        state[2][3] = t1;

        // 3. satır 3 sağa 
        temp = state[3][0];
        state[3][0] = state[3][1];
        state[3][1] = state[3][2];
        state[3][2] = state[3][3];
        state[3][3] = temp;
    }

    private void mixColumns(int[][] state) {
        for (int col = 0; col < 4; col++) {
            int s0 = state[0][col];
            int s1 = state[1][col];
            int s2 = state[2][col];
            int s3 = state[3][col];

            state[0][col] = AesConstants.gmul(0x02, s0) ^ AesConstants.gmul(0x03, s1) ^ s2 ^ s3;
            state[1][col] = s0 ^ AesConstants.gmul(0x02, s1) ^ AesConstants.gmul(0x03, s2) ^ s3;
            state[2][col] = s0 ^ s1 ^ AesConstants.gmul(0x02, s2) ^ AesConstants.gmul(0x03, s3);
            state[3][col] = AesConstants.gmul(0x03, s0) ^ s1 ^ s2 ^ AesConstants.gmul(0x02, s3);
        }
    }

    private void invMixColumns(int[][] state) {
        for (int col = 0; col < 4; col++) {
            int s0 = state[0][col];
            int s1 = state[1][col];
            int s2 = state[2][col];
            int s3 = state[3][col];

            state[0][col] = AesConstants.gmul(0x0e, s0) ^ AesConstants.gmul(0x0b, s1) ^ AesConstants.gmul(0x0d, s2) ^ AesConstants.gmul(0x09, s3);
            state[1][col] = AesConstants.gmul(0x09, s0) ^ AesConstants.gmul(0x0e, s1) ^ AesConstants.gmul(0x0b, s2) ^ AesConstants.gmul(0x0d, s3);
            state[2][col] = AesConstants.gmul(0x0d, s0) ^ AesConstants.gmul(0x09, s1) ^ AesConstants.gmul(0x0e, s2) ^ AesConstants.gmul(0x0b, s3);
            state[3][col] = AesConstants.gmul(0x0b, s0) ^ AesConstants.gmul(0x0d, s1) ^ AesConstants.gmul(0x09, s2) ^ AesConstants.gmul(0x0e, s3);
        }
    }
}