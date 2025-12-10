package site.dmbi.analytics.models

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Analytics event model matching the backend schema
 */
data class AnalyticsEvent(
    val siteId: String,
    val sessionId: String,
    val userId: String,
    val eventType: String,
    val pageUrl: String,
    val pageTitle: String? = null,
    val referrer: String? = null,
    val deviceType: String,
    val userAgent: String,
    val isLoggedIn: Boolean = false,
    val timestamp: Date = Date(),
    val duration: Int? = null,
    val scrollDepth: Int? = null,
    val customData: String? = null,
    // Video fields
    val videoId: String? = null,
    val videoTitle: String? = null,
    val videoDuration: Float? = null,
    val videoPosition: Float? = null,
    val videoPercent: Int? = null
) {
    fun toJson(): JSONObject {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        return JSONObject().apply {
            put("site_id", siteId)
            put("session_id", sessionId)
            put("user_id", userId)
            put("event_type", eventType)
            put("page_url", pageUrl)
            pageTitle?.let { put("page_title", it) }
            referrer?.let { put("referrer", it) }
            put("device_type", deviceType)
            put("user_agent", userAgent)
            put("is_logged_in", isLoggedIn)
            put("timestamp", dateFormat.format(timestamp))
            duration?.let { put("duration", it) }
            scrollDepth?.let { put("scroll_depth", it) }
            customData?.let { put("custom_data", it) }
            videoId?.let { put("video_id", it) }
            videoTitle?.let { put("video_title", it) }
            videoDuration?.let { put("video_duration", it) }
            videoPosition?.let { put("video_position", it) }
            videoPercent?.let { put("video_percent", it) }
        }
    }
}

/**
 * Stored event for offline persistence
 */
data class StoredEvent(
    val id: String = UUID.randomUUID().toString(),
    val event: AnalyticsEvent,
    val createdAt: Long = System.currentTimeMillis(),
    var retryCount: Int = 0
)
