package site.dmbi.analytics.util

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Helper for generating HMAC-SHA256 signatures for request authentication.
 *
 * The signature is generated as:
 * HMAC-SHA256(secretKey, timestamp + SHA256(payload))
 *
 * This prevents:
 * - Request forgery (attacker doesn't know the secret key)
 * - Replay attacks (timestamp is validated server-side within 5 min window)
 * - Payload tampering (payload hash is included in signature)
 */
object SignatureHelper {

    private var secretKey: String = ""

    /**
     * Initialize with the secret key.
     * Should be called during SDK initialization.
     *
     * @param key The secret key (should be stored securely, e.g., in BuildConfig)
     */
    fun initialize(key: String) {
        secretKey = key
    }

    /**
     * Check if signature helper is initialized with a valid key.
     */
    fun isInitialized(): Boolean {
        return secretKey.isNotEmpty()
    }

    /**
     * Generate signature for a request.
     *
     * @param timestamp Unix timestamp in milliseconds
     * @param payload The JSON payload to be sent
     * @return The hex-encoded HMAC-SHA256 signature
     */
    fun sign(timestamp: Long, payload: String): String {
        if (secretKey.isEmpty()) {
            return ""
        }

        val payloadHash = sha256(payload)
        val message = "$timestamp$payloadHash"
        return hmacSha256(secretKey, message)
    }

    /**
     * Calculate SHA256 hash of data.
     */
    private fun sha256(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return hashBytes.toHex()
    }

    /**
     * Calculate HMAC-SHA256.
     */
    private fun hmacSha256(key: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        val hashBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hashBytes.toHex()
    }

    /**
     * Convert ByteArray to hex string.
     */
    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }
}
