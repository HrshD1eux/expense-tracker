package com.hrshd1eux.expensetracker.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeystoreManager @Inject constructor() {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "ExpenseTrackerMasterKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 10000
        private const val PBKDF2_KEY_LENGTH = 256
    }

    private val secureRandom = SecureRandom()

    init {
        ensureMasterKeyExists()
    }

    private fun ensureMasterKeyExists() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .setInvalidatedByBiometricEnrollment(false)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(MASTER_KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypts plaintext bytes using hardware-backed AES-256-GCM.
     * Returns Base64-encoded string: IV + Ciphertext.
     */
    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getMasterKey())
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("KeystoreManager", "Encryption failed: ${e.message}", e)
            ""
        }
    }

    /**
     * Decrypts Base64-encoded IV + Ciphertext using hardware-backed AES-256-GCM.
     */
    fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) return ""

            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), spec)

            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            android.util.Log.e("KeystoreManager", "Decryption failed: ${e.message}", e)
            ""
        }
    }

    /**
     * Generates a cryptographically secure random salt.
     */
    fun generateSalt(): String = PinSecurityHelper.generateSalt()

    /**
     * Securely hashes a PIN with PBKDF2WithHmacSHA256 and salt.
     * Never stores raw PINs anywhere.
     */
    fun hashPin(pin: String, saltBase64: String): String = PinSecurityHelper.hashPin(pin, saltBase64)

    /**
     * Validates an entered PIN against stored salt and hash using constant-time comparison.
     */
    fun verifyPin(enteredPin: String, saltBase64: String, expectedHashBase64: String): Boolean =
        PinSecurityHelper.verifyPin(enteredPin, saltBase64, expectedHashBase64)

    private fun constantTimeEquals(a: String, b: String): Boolean = PinSecurityHelper.constantTimeEquals(a, b)

    /**
     * Returns the database passphrase as a ByteArray.
     *
     * On first call: generates 32 cryptographically-random bytes, encrypts them with
     * the hardware-backed Keystore key, and stores the ciphertext in [prefs].
     * On subsequent calls: decrypts and returns the stored passphrase.
     *
     * The caller MUST wipe the returned ByteArray from memory (fill with zeros)
     * as soon as it is no longer needed (after opening the database).
     *
     * @param prefs A SharedPreferences instance for storing the encrypted passphrase.
     *              Use app-private prefs (Context.MODE_PRIVATE).
     */
    fun getOrCreateDatabasePassphrase(prefs: android.content.SharedPreferences): ByteArray {
        val key = "db_passphrase_enc"
        val stored = prefs.getString(key, null)
        if (!stored.isNullOrEmpty()) {
            val decrypted = decrypt(stored)
            if (decrypted.isNotEmpty()) {
                try {
                    val decoded = android.util.Base64.decode(decrypted, android.util.Base64.NO_WRAP)
                    if (decoded.size == 32) {
                        return decoded
                    }
                } catch (e: Exception) {
                    android.util.Log.e("KeystoreManager", "Failed to decode passphrase: ${e.message}")
                }
            }
            // CRITICAL: Do NOT overwrite an existing passphrase if decryption failed.
            // Throwing here keeps the database file intact and prevents irreversible data loss.
            throw IllegalStateException("Database encryption passphrase exists but could not be decrypted. Halting to prevent data loss.")
        }

        // Generate a new random 32-byte passphrase ONLY on clean initial install
        val raw = ByteArray(32).also { secureRandom.nextBytes(it) }
        val rawBase64 = android.util.Base64.encodeToString(raw, android.util.Base64.NO_WRAP)
        val encrypted = encrypt(rawBase64)
        if (encrypted.isEmpty()) {
            throw IllegalStateException("Failed to encrypt initial database passphrase with Keystore.")
        }
        prefs.edit().putString(key, encrypted).commit()
        return raw
    }
}
