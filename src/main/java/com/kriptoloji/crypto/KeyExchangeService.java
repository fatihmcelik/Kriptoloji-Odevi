package com.kriptoloji.crypto;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberKEMExtractor;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberKEMGenerator;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberKeyPairGenerator;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberParameters;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberPublicKeyParameters;

public class KeyExchangeService {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final SecureRandom secureRandom = new SecureRandom();

    public static class ReceiverSession {
        public final KeyExchangeAlgorithm algorithm;
        public final byte[] publicBytes;
        public final Object privateState;
        public final double keyGenTimeMs;

        public ReceiverSession(
                KeyExchangeAlgorithm algorithm,
                byte[] publicBytes,
                Object privateState,
                double keyGenTimeMs) {
            this.algorithm = algorithm;
            this.publicBytes = publicBytes;
            this.privateState = privateState;
            this.keyGenTimeMs = keyGenTimeMs;
        }
    }

    public static class SenderResponse {
        public final byte[] payloadBytes;
        public final byte[] aesKey;
        public final double exchangeTimeMs;

        public SenderResponse(byte[] payloadBytes, byte[] aesKey, double exchangeTimeMs) {
            this.payloadBytes = payloadBytes;
            this.aesKey = aesKey;
            this.exchangeTimeMs = exchangeTimeMs;
        }
    }

    public ReceiverSession initReceiver(KeyExchangeAlgorithm algorithm) throws Exception {
        long start = System.nanoTime();
        switch (algorithm) {
            case RSA_2048 -> {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048, secureRandom);
                KeyPair kp = kpg.generateKeyPair();
                double elapsed = (System.nanoTime() - start) / 1_000_000.0;
                return new ReceiverSession(algorithm, kp.getPublic().getEncoded(), kp.getPrivate(), elapsed);
            }
            case DH_2048 -> {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
                kpg.initialize(2048, secureRandom);
                KeyPair kp = kpg.generateKeyPair();
                double elapsed = (System.nanoTime() - start) / 1_000_000.0;
                return new ReceiverSession(algorithm, kp.getPublic().getEncoded(), kp.getPrivate(), elapsed);
            }
            case ECDH -> {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
                kpg.initialize(new ECGenParameterSpec("secp256r1"), secureRandom);
                KeyPair kp = kpg.generateKeyPair();
                double elapsed = (System.nanoTime() - start) / 1_000_000.0;
                return new ReceiverSession(algorithm, kp.getPublic().getEncoded(), kp.getPrivate(), elapsed);
            }
            case KYBER_768 -> {
                KyberKeyPairGenerator kpg = new KyberKeyPairGenerator();
                kpg.init(new KyberKeyGenerationParameters(secureRandom, KyberParameters.kyber768));
                AsymmetricCipherKeyPair kp = kpg.generateKeyPair();
                KyberPublicKeyParameters pubParams = (KyberPublicKeyParameters) kp.getPublic();
                double elapsed = (System.nanoTime() - start) / 1_000_000.0;
                return new ReceiverSession(algorithm, pubParams.getEncoded(), kp.getPrivate(), elapsed);
            }
            default -> throw new IllegalArgumentException("Desteklenmeyen algoritma: " + algorithm);
        }
    }

    public SenderResponse processSender(KeyExchangeAlgorithm algorithm, byte[] receiverPublicBytes) throws Exception {
        long start = System.nanoTime();
        switch (algorithm) {
            case RSA_2048 -> {
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PublicKey receiverPub = kf.generatePublic(new X509EncodedKeySpec(receiverPublicBytes));

                byte[] aesKey = new byte[16];
                secureRandom.nextBytes(aesKey);

                Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, receiverPub, secureRandom);
                byte[] encryptedKey = cipher.doFinal(aesKey);

                double elapsed = (System.nanoTime() - start) / 1_000_000.0;
                return new SenderResponse(encryptedKey, aesKey, elapsed);
            }
            case DH_2048 -> {
                KeyFactory kf = KeyFactory.getInstance("DH");
                PublicKey receiverPub = kf.generatePublic(new X509EncodedKeySpec(receiverPublicBytes));
                DHParameterSpec dhSpec = ((DHPublicKey) receiverPub).getParams();

                KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
                kpg.initialize(dhSpec, secureRandom);
                KeyPair senderKp = kpg.generateKeyPair();

                KeyAgreement ka = KeyAgreement.getInstance("DH");
                ka.init(senderKp.getPrivate());
                ka.doPhase(receiverPub, true);
                byte[] aesKey = deriveAes128Key(ka.generateSecret());

                double elapsed = (System.nanoTime() - start) / 1_000_000.0;
                return new SenderResponse(senderKp.getPublic().getEncoded(), aesKey, elapsed);
            }
            case ECDH -> {
                KeyFactory kf = KeyFactory.getInstance("EC");
                PublicKey receiverPub = kf.generatePublic(new X509EncodedKeySpec(receiverPublicBytes));
                ECParameterSpec ecSpec = ((ECPublicKey) receiverPub).getParams();

                KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
                kpg.initialize(ecSpec, secureRandom);
                KeyPair senderKp = kpg.generateKeyPair();

                KeyAgreement ka = KeyAgreement.getInstance("ECDH");
                ka.init(senderKp.getPrivate());
                ka.doPhase(receiverPub, true);
                byte[] aesKey = deriveAes128Key(ka.generateSecret());

                double elapsed = (System.nanoTime() - start) / 1_000_000.0;
                return new SenderResponse(senderKp.getPublic().getEncoded(), aesKey, elapsed);
            }
            case KYBER_768 -> {
                KyberPublicKeyParameters pubParams =
                        new KyberPublicKeyParameters(KyberParameters.kyber768, receiverPublicBytes);
                KyberKEMGenerator generator = new KyberKEMGenerator(secureRandom);
                SecretWithEncapsulation encapsulated = generator.generateEncapsulated(pubParams);

                byte[] aesKey = deriveAes128Key(encapsulated.getSecret());
                byte[] cipherText = encapsulated.getEncapsulation();

                double elapsed = (System.nanoTime() - start) / 1_000_000.0;
                return new SenderResponse(cipherText, aesKey, elapsed);
            }
            default -> throw new IllegalArgumentException("Desteklenmeyen algoritma: " + algorithm);
        }
    }

    public byte[] finalizeReceiver(ReceiverSession session, byte[] senderPayloadBytes) throws Exception {
        switch (session.algorithm) {
            case RSA_2048 -> {
                Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
                cipher.init(Cipher.DECRYPT_MODE, (PrivateKey) session.privateState);
                return cipher.doFinal(senderPayloadBytes);
            }
            case DH_2048 -> {
                KeyFactory kf = KeyFactory.getInstance("DH");
                PublicKey senderPub = kf.generatePublic(new X509EncodedKeySpec(senderPayloadBytes));
                KeyAgreement ka = KeyAgreement.getInstance("DH");
                ka.init((PrivateKey) session.privateState);
                ka.doPhase(senderPub, true);
                return deriveAes128Key(ka.generateSecret());
            }
            case ECDH -> {
                KeyFactory kf = KeyFactory.getInstance("EC");
                PublicKey senderPub = kf.generatePublic(new X509EncodedKeySpec(senderPayloadBytes));
                KeyAgreement ka = KeyAgreement.getInstance("ECDH");
                ka.init((PrivateKey) session.privateState);
                ka.doPhase(senderPub, true);
                return deriveAes128Key(ka.generateSecret());
            }
            case KYBER_768 -> {
                KyberKEMExtractor extractor = new KyberKEMExtractor((KyberPrivateKeyParameters) session.privateState);
                byte[] sharedSecret = extractor.extractSecret(senderPayloadBytes);
                return deriveAes128Key(sharedSecret);
            }
            default -> throw new IllegalArgumentException("Desteklenmeyen algoritma: " + session.algorithm);
        }
    }

    public KeyExchangeResult executeFullHandshake(KeyExchangeAlgorithm algorithm) throws Exception {
        ReceiverSession receiverSession = initReceiver(algorithm);
        SenderResponse senderResponse = processSender(algorithm, receiverSession.publicBytes);

        long finishStart = System.nanoTime();
        byte[] receiverAesKey = finalizeReceiver(receiverSession, senderResponse.payloadBytes);
        double finishTimeMs = (System.nanoTime() - finishStart) / 1_000_000.0;

        if (!Arrays.equals(senderResponse.aesKey, receiverAesKey)) {
            throw new IllegalStateException(algorithm + " anahtar uyusmazligi olustu!");
        }

        int totalPacketBytes = receiverSession.publicBytes.length + senderResponse.payloadBytes.length;
        double totalExchangeMs = senderResponse.exchangeTimeMs + finishTimeMs;

        return new KeyExchangeResult(
                algorithm,
                receiverAesKey,
                receiverSession.keyGenTimeMs,
                totalExchangeMs,
                totalPacketBytes);
    }

    private byte[] deriveAes128Key(byte[] sharedSecret) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] digest = sha256.digest(sharedSecret);
        return Arrays.copyOf(digest, 16);
    }
}