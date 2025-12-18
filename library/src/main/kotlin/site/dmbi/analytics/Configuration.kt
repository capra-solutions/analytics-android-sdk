package site.dmbi.analytics

/**
 * SDK configuration
 */
data class DMBIConfiguration(
    /** Site identifier (e.g., "hurriyet-android") */
    val siteId: String,
    /** Analytics endpoint URL */
    val endpoint: String,
    /** Secret key for request signing (required for security) */
    val secretKey: String = "",
    /** Heartbeat interval in milliseconds (default: 30 seconds) */
    val heartbeatInterval: Long = 30_000L,
    /** Maximum heartbeat interval when user is inactive (default: 120 seconds) */
    val maxHeartbeatInterval: Long = 120_000L,
    /** Time without interaction before increasing heartbeat interval (default: 30 seconds) */
    val inactivityThreshold: Long = 30_000L,
    /** Batch size for event sending (default: 10) */
    val batchSize: Int = 10,
    /** Flush interval in milliseconds (default: 30 seconds) */
    val flushInterval: Long = 30_000L,
    /** Maximum retry count for failed events (default: 3) */
    val maxRetryCount: Int = 3,
    /** Session timeout in milliseconds - new session after background (default: 30 minutes) */
    val sessionTimeout: Long = 30 * 60 * 1000L,
    /** Enable debug logging (default: false) */
    val debugLogging: Boolean = false,
    /** Maximum offline events to store (default: 1000) */
    val maxOfflineEvents: Int = 1000,
    /** Days to keep offline events (default: 7) */
    val offlineRetentionDays: Int = 7,
    /** Enable automatic scroll tracking when possible (default: true) */
    val autoScrollTracking: Boolean = true
) {
    class Builder(private val siteId: String, private val endpoint: String) {
        private var secretKey: String = ""
        private var heartbeatInterval: Long = 30_000L
        private var maxHeartbeatInterval: Long = 120_000L
        private var inactivityThreshold: Long = 30_000L
        private var batchSize: Int = 10
        private var flushInterval: Long = 30_000L
        private var maxRetryCount: Int = 3
        private var sessionTimeout: Long = 30 * 60 * 1000L
        private var debugLogging: Boolean = false
        private var maxOfflineEvents: Int = 1000
        private var offlineRetentionDays: Int = 7
        private var autoScrollTracking: Boolean = true

        fun secretKey(key: String) = apply { secretKey = key }
        fun heartbeatInterval(interval: Long) = apply { heartbeatInterval = interval }
        fun maxHeartbeatInterval(interval: Long) = apply { maxHeartbeatInterval = interval }
        fun inactivityThreshold(threshold: Long) = apply { inactivityThreshold = threshold }
        fun batchSize(size: Int) = apply { batchSize = size }
        fun flushInterval(interval: Long) = apply { flushInterval = interval }
        fun maxRetryCount(count: Int) = apply { maxRetryCount = count }
        fun sessionTimeout(timeout: Long) = apply { sessionTimeout = timeout }
        fun debugLogging(enabled: Boolean) = apply { debugLogging = enabled }
        fun maxOfflineEvents(count: Int) = apply { maxOfflineEvents = count }
        fun offlineRetentionDays(days: Int) = apply { offlineRetentionDays = days }
        fun autoScrollTracking(enabled: Boolean) = apply { autoScrollTracking = enabled }

        fun build() = DMBIConfiguration(
            siteId = siteId,
            endpoint = endpoint,
            secretKey = secretKey,
            heartbeatInterval = heartbeatInterval,
            maxHeartbeatInterval = maxHeartbeatInterval,
            inactivityThreshold = inactivityThreshold,
            batchSize = batchSize,
            flushInterval = flushInterval,
            maxRetryCount = maxRetryCount,
            sessionTimeout = sessionTimeout,
            debugLogging = debugLogging,
            maxOfflineEvents = maxOfflineEvents,
            offlineRetentionDays = offlineRetentionDays,
            autoScrollTracking = autoScrollTracking
        )
    }
}
