package com.example.crypto

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
    private const val AES_KEY_SIZE = 256
    private const val RSA_KEY_SIZE = 2048
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val PBKDF2_ITERATIONS = 100_000

    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    // --- AES-256 GCM ---

    fun generateAesKey(): ByteArray {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(AES_KEY_SIZE, SecureRandom())
        return keyGen.generateKey().encoded
    }

    data class AesGcmResult(
        val ciphertextHex: String,
        val ivHex: String
    )

    fun encryptAesGcm(plainData: ByteArray, keyBytes: ByteArray): AesGcmResult {
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertext = cipher.doFinal(plainData)
        return AesGcmResult(
            ciphertextHex = bytesToHex(ciphertext),
            ivHex = bytesToHex(iv)
        )
    }

    fun decryptAesGcm(ciphertextHex: String, keyBytes: ByteArray, ivHex: String): ByteArray {
        val ciphertext = hexToBytes(ciphertextHex)
        val iv = hexToBytes(ivHex)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        return cipher.doFinal(ciphertext)
    }

    // --- RSA KEY PAIR & KEY EXCHANGE ---

    data class RsaKeyPairPem(
        val publicKeyPem: String,
        val privateKeyPem: String,
        val fingerprintSha256: String
    )

    fun generateRsaKeyPair(): RsaKeyPairPem {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(RSA_KEY_SIZE, SecureRandom())
        val keyPair = kpg.genKeyPair()

        val pubEncoded = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        val privEncoded = Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP)

        val pubPem = "-----BEGIN PUBLIC KEY-----\n$pubEncoded\n-----END PUBLIC KEY-----"
        val privPem = "-----BEGIN PRIVATE KEY-----\n$privEncoded\n-----END PRIVATE KEY-----"

        val fingerprint = sha256(keyPair.public.encoded)

        return RsaKeyPairPem(pubPem, privPem, fingerprint)
    }

    private fun parsePublicKeyPem(pubPem: String): PublicKey {
        val cleanPem = pubPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .trim()
        val decoded = Base64.decode(cleanPem, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(decoded)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePublic(keySpec)
    }

    private fun parsePrivateKeyPem(privPem: String): PrivateKey {
        val cleanPem = privPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
            .trim()
        val decoded = Base64.decode(cleanPem, Base64.DEFAULT)
        val keySpec = PKCS8EncodedKeySpec(decoded)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(keySpec)
    }

    fun wrapAesKeyWithRsaPublic(aesKeyBytes: ByteArray, rsaPublicKeyPem: String): String {
        val publicKey = parsePublicKeyPem(rsaPublicKeyPem)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val wrapped = cipher.doFinal(aesKeyBytes)
        return bytesToHex(wrapped)
    }

    fun unwrapAesKeyWithRsaPrivate(wrappedAesKeyHex: String, rsaPrivateKeyPem: String): ByteArray {
        val wrappedBytes = hexToBytes(wrappedAesKeyHex)
        val privateKey = parsePrivateKeyPem(rsaPrivateKeyPem)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(wrappedBytes)
    }

    // --- PASSWORD PROTECTION (PBKDF2 Key Derivation + AES-CBC Wrap) ---

    data class PasswordWrapResult(
        val wrappedKeyHex: String,
        val saltHex: String,
        val ivHex: String
    )

    private fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    fun wrapAesKeyWithPassword(aesKeyBytes: ByteArray, password: String): PasswordWrapResult {
        val salt = ByteArray(16)
        val iv = ByteArray(16)
        SecureRandom().nextBytes(salt)
        SecureRandom().nextBytes(iv)

        val derivedKey = deriveKeyFromPassword(password, salt)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, derivedKey, IvParameterSpec(iv))

        val wrapped = cipher.doFinal(aesKeyBytes)

        return PasswordWrapResult(
            wrappedKeyHex = bytesToHex(wrapped),
            saltHex = bytesToHex(salt),
            ivHex = bytesToHex(iv)
        )
    }

    fun unwrapAesKeyWithPassword(
        wrappedKeyHex: String,
        password: String,
        saltHex: String,
        ivHex: String
    ): ByteArray {
        val wrappedBytes = hexToBytes(wrappedKeyHex)
        val salt = hexToBytes(saltHex)
        val iv = hexToBytes(ivHex)

        val derivedKey = deriveKeyFromPassword(password, salt)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, derivedKey, IvParameterSpec(iv))

        return cipher.doFinal(wrappedBytes)
    }

    // --- FINGERPRINTS & DIGEST ---

    fun sha256(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return bytesToHex(digest)
    }

    fun sha256(text: String): String {
        return sha256(text.toByteArray(Charsets.UTF_8))
    }

    // --- SECURE SHARE LINK TOKEN TOKENIZATION ---

    fun generateShareLink(fileId: String, shareToken: String): String {
        return "https://secureshare.enterprise.vault/v1/download/$fileId?token=$shareToken"
    }
}
