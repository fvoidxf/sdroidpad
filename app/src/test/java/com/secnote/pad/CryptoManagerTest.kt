package com.secnote.pad

import org.junit.Assert.*
import org.junit.Test

class CryptoManagerTest {

    private val testPassword = "TestP@ssw0rd!2024"
    private val testContent = "Hello, this is a secret note content with UTF-8: Привет мир!"

    @Test
    fun `encrypt and decrypt AES-256 roundtrip`() {
        val encrypted = CryptoManager.encrypt(testContent.toByteArray(Charsets.UTF_8), testPassword, CryptoManager.ALGO_AES256)
        val decrypted = CryptoManager.decrypt(encrypted, testPassword, CryptoManager.ALGO_AES256)
        assertEquals(testContent, String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun `encrypt and decrypt Kuznechik roundtrip`() {
        val encrypted = CryptoManager.encrypt(testContent.toByteArray(Charsets.UTF_8), testPassword, CryptoManager.ALGO_KUZNECHIK)
        val decrypted = CryptoManager.decrypt(encrypted, testPassword, CryptoManager.ALGO_KUZNECHIK)
        assertEquals(testContent, String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun `encrypt and decrypt Magma roundtrip`() {
        val encrypted = CryptoManager.encrypt(testContent.toByteArray(Charsets.UTF_8), testPassword, CryptoManager.ALGO_MAGMA)
        val decrypted = CryptoManager.decrypt(encrypted, testPassword, CryptoManager.ALGO_MAGMA)
        assertEquals(testContent, String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun `wrong password fails decryption`() {
        val encrypted = CryptoManager.encrypt(testContent.toByteArray(Charsets.UTF_8), testPassword, CryptoManager.ALGO_AES256)
        val result = try {
            CryptoManager.decrypt(encrypted, "wrong_password", CryptoManager.ALGO_AES256)
            false
        } catch (e: Exception) {
            true
        }
        assertTrue("Decryption with wrong password should fail", result)
    }

    @Test
    fun `encrypted data differs from plaintext`() {
        val encrypted = CryptoManager.encrypt(testContent.toByteArray(Charsets.UTF_8), testPassword, CryptoManager.ALGO_AES256)
        val plaintextBytes = testContent.toByteArray(Charsets.UTF_8)
        assertFalse(plaintextBytes.contentEquals(encrypted))
    }

    @Test
    fun `two encryptions produce different ciphertexts`() {
        val enc1 = CryptoManager.encrypt(testContent.toByteArray(Charsets.UTF_8), testPassword, CryptoManager.ALGO_AES256)
        val enc2 = CryptoManager.encrypt(testContent.toByteArray(Charsets.UTF_8), testPassword, CryptoManager.ALGO_AES256)
        assertFalse("Same plaintext encrypted twice should differ (random salt/IV)", enc1.contentEquals(enc2))
    }

    @Test
    fun `encrypted data has correct structure`() {
        val encrypted = CryptoManager.encrypt(testContent.toByteArray(Charsets.UTF_8), testPassword, CryptoManager.ALGO_AES256)
        // salt(16) + iv(12) + ciphertext+tag
        assertTrue("Encrypted data should be at least 28 bytes", encrypted.size >= 28)
    }

    @Test
    fun `secure delete zeroes out data`() {
        val original = ByteArray(64).also { java.security.SecureRandom().nextBytes(it) }
        val wiped = CryptoManager.secureDelete(original)
        assertTrue("All bytes should be zero after secure delete", wiped.all { it == 0.toByte() })
    }

    @Test
    fun `empty content encrypts and decrypts`() {
        val empty = ByteArray(0)
        val encrypted = CryptoManager.encrypt(empty, testPassword, CryptoManager.ALGO_AES256)
        val decrypted = CryptoManager.decrypt(encrypted, testPassword, CryptoManager.ALGO_AES256)
        assertEquals(0, decrypted.size)
    }

    @Test
    fun `large content encrypts and decrypts`() {
        val large = "A".repeat(100_000)
        val encrypted = CryptoManager.encrypt(large.toByteArray(Charsets.UTF_8), testPassword, CryptoManager.ALGO_AES256)
        val decrypted = CryptoManager.decrypt(encrypted, testPassword, CryptoManager.ALGO_AES256)
        assertEquals(large, String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun `unknown algorithm throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            CryptoManager.encrypt(testContent.toByteArray(Charsets.UTF_8), testPassword, "UnknownAlgo")
        }
    }

    @Test
    fun `data too short throws on decrypt`() {
        assertThrows(IllegalArgumentException::class.java) {
            CryptoManager.decrypt(ByteArray(5), testPassword, CryptoManager.ALGO_AES256)
        }
    }
}
