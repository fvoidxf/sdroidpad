package com.secnote.pad

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    const val ALGO_AES256 = "AES-256"
    const val ALGO_KUZNECHIK = "Kuznechik"
    const val ALGO_MAGMA = "Magma"

    private const val PBKDF2_ITERATIONS = 100_000
    private const val SALT_SIZE = 16
    private const val AES_IV_SIZE = 12
    private const val KUZNECHIK_IV_SIZE = 16
    private const val MAGMA_IV_SIZE = 8
    private const val GCM_TAG_BITS = 128
    private const val HMAC_SIZE = 32
    private const val KEY_BITS = 256

    init {
        val bc = BouncyCastleProvider()
        val existing = Security.getProvider(bc.name)
        if (existing == null) {
            Security.insertProviderAt(bc, 1)
        } else if (existing.javaClass != bc.javaClass) {
            Security.removeProvider(bc.name)
            Security.insertProviderAt(bc, 1)
        }
    }

    fun encrypt(plaintext: ByteArray, password: String, algorithm: String): ByteArray {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val encKey = deriveKey(password, salt, "enc", algorithm)
        val macKey = deriveKey(password, salt, "mac", algorithm)

        return when (algorithm) {
            ALGO_AES256 -> encryptAesGcm(plaintext, encKey, salt)
            ALGO_KUZNECHIK, ALGO_MAGMA -> encryptGostCtrHmac(plaintext, encKey, macKey, salt, algorithm)
            else -> throw IllegalArgumentException("Unknown algorithm: $algorithm")
        }
    }

    fun decrypt(data: ByteArray, password: String, algorithm: String): ByteArray {
        require(data.size >= SALT_SIZE) { "Data too short" }
        val salt = data.copyOfRange(0, SALT_SIZE)
        val encKey = deriveKey(password, salt, "enc", algorithm)
        val macKey = deriveKey(password, salt, "mac", algorithm)

        return when (algorithm) {
            ALGO_AES256 -> decryptAesGcm(data, encKey)
            ALGO_KUZNECHIK, ALGO_MAGMA -> decryptGostCtrHmac(data, encKey, macKey, algorithm)
            else -> throw IllegalArgumentException("Unknown algorithm: $algorithm")
        }
    }

    fun secureDelete(data: ByteArray, passes: Int = 3): ByteArray {
        val random = SecureRandom()
        val result = data.copyOf()
        for (i in 0 until passes) {
            random.nextBytes(result)
        }
        result.fill(0)
        return result
    }

    private fun encryptAesGcm(plaintext: ByteArray, key: SecretKeySpec, salt: ByteArray): ByteArray {
        val iv = ByteArray(AES_IV_SIZE).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return salt + iv + ciphertext
    }

    private fun decryptAesGcm(data: ByteArray, key: SecretKeySpec): ByteArray {
        require(data.size >= SALT_SIZE + AES_IV_SIZE) { "Data too short" }
        val iv = data.copyOfRange(SALT_SIZE, SALT_SIZE + AES_IV_SIZE)
        val ciphertext = data.copyOfRange(SALT_SIZE + AES_IV_SIZE, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun encryptGostCtrHmac(
        plaintext: ByteArray, encKey: SecretKeySpec, macKey: SecretKeySpec,
        salt: ByteArray, algorithm: String
    ): ByteArray {
        val ivSize = getGostIvSize(algorithm)
        val iv = ByteArray(ivSize).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(getGostTransformation(algorithm))
        cipher.init(Cipher.ENCRYPT_MODE, encKey, IvParameterSpec(iv))
        val ciphertext = cipher.doFinal(plaintext)

        val hmac = computeHmac(macKey, salt, iv, ciphertext)
        return salt + iv + hmac + ciphertext
    }

    private fun decryptGostCtrHmac(
        data: ByteArray, encKey: SecretKeySpec, macKey: SecretKeySpec, algorithm: String
    ): ByteArray {
        val ivSize = getGostIvSize(algorithm)
        val minSize = SALT_SIZE + ivSize + HMAC_SIZE
        require(data.size >= minSize) { "Data too short" }
        val iv = data.copyOfRange(SALT_SIZE, SALT_SIZE + ivSize)
        val hmac = data.copyOfRange(SALT_SIZE + ivSize, SALT_SIZE + ivSize + HMAC_SIZE)
        val ciphertext = data.copyOfRange(SALT_SIZE + ivSize + HMAC_SIZE, data.size)

        val salt = data.copyOfRange(0, SALT_SIZE)
        val expectedHmac = computeHmac(macKey, salt, iv, ciphertext)
        if (!hmac.contentEquals(expectedHmac)) {
            throw SecurityException("HMAC verification failed")
        }

        val cipher = Cipher.getInstance(getGostTransformation(algorithm))
        cipher.init(Cipher.DECRYPT_MODE, encKey, IvParameterSpec(iv))
        return cipher.doFinal(ciphertext)
    }

    private fun computeHmac(key: SecretKeySpec, vararg parts: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        for (part in parts) mac.update(part)
        return mac.doFinal()
    }

    private fun deriveKey(password: String, salt: ByteArray, purpose: String, algorithm: String): SecretKeySpec {
        val purposeBytes = purpose.toByteArray(Charsets.US_ASCII)
        val saltWithPurpose = salt + purposeBytes
        val spec = PBEKeySpec(password.toCharArray(), saltWithPurpose, PBKDF2_ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        val keyAlgo = when (algorithm) {
            ALGO_AES256 -> "AES"
            ALGO_KUZNECHIK -> "GOST3412-2015"
            ALGO_MAGMA -> "GOST28147"
            else -> throw IllegalArgumentException("Unknown algorithm: $algorithm")
        }
        return SecretKeySpec(keyBytes, keyAlgo)
    }

    private fun getGostTransformation(algorithm: String): String = when (algorithm) {
        ALGO_KUZNECHIK -> "GOST3412-2015/CTR/NoPadding"
        ALGO_MAGMA -> "GOST28147/CTR/NoPadding"
        else -> throw IllegalArgumentException("Not a GOST algorithm: $algorithm")
    }

    private fun getGostIvSize(algorithm: String): Int = when (algorithm) {
        ALGO_KUZNECHIK -> KUZNECHIK_IV_SIZE
        ALGO_MAGMA -> MAGMA_IV_SIZE
        else -> throw IllegalArgumentException("Not a GOST algorithm: $algorithm")
    }
}
