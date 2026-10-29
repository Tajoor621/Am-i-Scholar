package com.amischolar.app.feature

import android.util.Base64
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** UTF-8-compatible encrypted JSON envelope shared with the browser implementation. */
object EncryptedBackup {
    const val FORMAT = "am-i-scholar-encrypted"
    const val VERSION = 1
    const val ITERATIONS = 310000
    const val MAX_BYTES = 10 * 1024 * 1024
    private const val KDF = "PBKDF2-HMAC-SHA-256"
    private const val CIPHER = "AES-256-GCM"
    private const val TAG_BITS = 128
    private val random = SecureRandom()

    fun isEnvelope(bytes: ByteArray): Boolean = runCatching {
        JSONObject(bytes.toString(UTF_8)).optString("format") == FORMAT
    }.getOrDefault(false)

    fun encrypt(plainJson: ByteArray, passphrase: String): ByteArray {
        require(plainJson.isNotEmpty() && plainJson.size <= MAX_BYTES) { "Backup size is invalid" }
        require(passphrase.length in 12..1024) { "Use a passphrase with at least 12 characters" }
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val salt64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val iv64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val header = header(salt64, iv64)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derive(passphrase, salt), "AES"), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(header.toByteArray(UTF_8))
        val encrypted = cipher.doFinal(plainJson)
        val envelope = JSONObject()
            .put("format", FORMAT).put("version", VERSION).put("kdf", KDF)
            .put("iterations", ITERATIONS).put("cipher", CIPHER)
            .put("salt", salt64).put("iv", iv64)
            .put("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .toString().toByteArray(UTF_8)
        require(envelope.size <= MAX_BYTES) { "Encrypted backup exceeds 10 MB" }
        return envelope
    }

    fun decrypt(envelopeBytes: ByteArray, passphrase: String): ByteArray {
        require(envelopeBytes.size in 1..MAX_BYTES) { "Backup size is invalid" }
        require(passphrase.length in 12..1024) { "Passphrase length is invalid" }
        val envelope = JSONObject(envelopeBytes.toString(UTF_8))
        require(envelope.optString("format") == FORMAT && envelope.optInt("version") == VERSION) { "Unsupported encrypted backup format" }
        require(envelope.optString("kdf") == KDF && envelope.optInt("iterations") == ITERATIONS &&
            envelope.optString("cipher") == CIPHER) { "Unsupported encryption parameters" }
        val salt64 = envelope.getString("salt")
        val iv64 = envelope.getString("iv")
        val salt = Base64.decode(salt64, Base64.DEFAULT)
        val iv = Base64.decode(iv64, Base64.DEFAULT)
        val encrypted = Base64.decode(envelope.getString("ciphertext"), Base64.DEFAULT)
        require(salt.size == 16 && iv.size == 12 && encrypted.size in 17..MAX_BYTES) { "Invalid encrypted backup fields" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derive(passphrase, salt), "AES"), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(header(salt64, iv64).toByteArray(UTF_8))
        return cipher.doFinal(encrypted).also { require(it.size <= MAX_BYTES) { "Decrypted backup exceeds 10 MB" } }
    }

    private fun header(salt: String, iv: String) =
        "{\"format\":\"$FORMAT\",\"version\":$VERSION,\"kdf\":\"$KDF\",\"iterations\":$ITERATIONS,\"cipher\":\"$CIPHER\",\"salt\":\"$salt\",\"iv\":\"$iv\"}"

    /** PBKDF2-HMAC-SHA-256 with UTF-8 passphrase bytes to match Web Crypto exactly. */
    private fun derive(passphrase: String, salt: ByteArray): ByteArray {
        val password = passphrase.toByteArray(UTF_8)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(password, "HmacSHA256"))
        val block = mac.doFinal(salt + ByteBuffer.allocate(4).putInt(1).array())
        val result = block.copyOf()
        var u = block
        for (round in 1 until ITERATIONS) {
            u = mac.doFinal(u)
            for (index in result.indices) result[index] = (result[index].toInt() xor u[index].toInt()).toByte()
        }
        password.fill(0)
        block.fill(0)
        u.fill(0)
        return result
    }
}
