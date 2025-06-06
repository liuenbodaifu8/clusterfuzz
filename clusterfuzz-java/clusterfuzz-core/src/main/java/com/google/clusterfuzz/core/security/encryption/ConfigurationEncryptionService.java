package com.google.clusterfuzz.core.security.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive configuration values.
 */
@Service
@Slf4j
public class ConfigurationEncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public ConfigurationEncryptionService(
            @Value("${clusterfuzz.security.encryption.key:}") String encryptionKey) {
        
        this.secureRandom = new SecureRandom();
        
        if (encryptionKey != null && !encryptionKey.trim().isEmpty()) {
            // Use provided key
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        } else {
            // Generate a new key (for development/testing)
            this.secretKey = generateKey();
            log.warn("No encryption key provided, generated a new one. " +
                    "This should not happen in production!");
        }
    }

    /**
     * Encrypts a plaintext string.
     */
    public String encrypt(String plaintext) throws EncryptionException {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV and ciphertext
            byte[] encryptedData = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);

            // Encode to Base64
            return Base64.getEncoder().encodeToString(encryptedData);

        } catch (Exception e) {
            log.error("Failed to encrypt data", e);
            throw new EncryptionException("Encryption failed", e);
        }
    }

    /**
     * Decrypts an encrypted string.
     */
    public String decrypt(String encryptedData) throws EncryptionException {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }

        try {
            // Decode from Base64
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);

            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[decodedData.length - GCM_IV_LENGTH];
            System.arraycopy(decodedData, 0, iv, 0, iv.length);
            System.arraycopy(decodedData, iv.length, ciphertext, 0, ciphertext.length);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Failed to decrypt data", e);
            throw new EncryptionException("Decryption failed", e);
        }
    }

    /**
     * Generates a new AES key.
     */
    private SecretKey generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256); // AES-256
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }

    /**
     * Gets the Base64-encoded key for configuration.
     */
    public String getEncodedKey() {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    /**
     * Validates if a string is encrypted (Base64 format check).
     */
    public boolean isEncrypted(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(data);
            return decoded.length > GCM_IV_LENGTH; // Must have at least IV + some data
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Re-encrypts data with a new key (for key rotation).
     */
    public String reEncrypt(String encryptedData, SecretKey oldKey) throws EncryptionException {
        // First decrypt with old key
        String plaintext = decryptWithKey(encryptedData, oldKey);
        
        // Then encrypt with current key
        return encrypt(plaintext);
    }

    /**
     * Decrypts data with a specific key.
     */
    private String decryptWithKey(String encryptedData, SecretKey key) throws EncryptionException {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }

        try {
            // Decode from Base64
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);

            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[decodedData.length - GCM_IV_LENGTH];
            System.arraycopy(decodedData, 0, iv, 0, iv.length);
            System.arraycopy(decodedData, iv.length, ciphertext, 0, ciphertext.length);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Failed to decrypt data with provided key", e);
            throw new EncryptionException("Decryption failed", e);
        }
    }

    /**
     * Exception for encryption/decryption errors.
     */
    public static class EncryptionException extends Exception {
        public EncryptionException(String message) {
            super(message);
        }

        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}