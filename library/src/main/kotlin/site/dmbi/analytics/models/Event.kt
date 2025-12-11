package site.dmbi.analytics.models

import android.net.Uri
import org.json.JSONArray
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
    val videoPercent: Int? = null,
    // Article metadata fields (matching web tracker)
    val creator: String? = null,
    val articleAuthor: String? = null,
    val articleSection: String? = null,
    val articleKeywords: List<String>? = null,
    val publishedDate: String? = null,
    val contentType: String? = null,
    // Navigation tracking
    val previousPageUrl: String? = null,
    val previousPageTitle: String? = null,
    // Screen dimensions
    val screenWidth: Int? = null,
    val screenHeight: Int? = null,
    // UTM Campaign parameters
    val utmSource: String? = null,
    val utmMedium: String? = null,
    val utmCampaign: String? = null,
    val utmContent: String? = null,
    val utmTerm: String? = null,
    // Engagement metrics
    val activeTimeSeconds: Int? = null,
    val pingCounter: Int? = null,
    // User classification
    val userType: String? = null,
    val userSegments: List<String>? = null
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
            // Video
            videoId?.let { put("video_id", it) }
            videoTitle?.let { put("video_title", it) }
            videoDuration?.let { put("video_duration", it) }
            videoPosition?.let { put("video_position", it) }
            videoPercent?.let { put("video_percent", it) }
            // Article metadata
            creator?.let { put("creator", it) }
            articleAuthor?.let { put("article_author", it) }
            articleSection?.let { put("article_section", it) }
            articleKeywords?.let { put("article_keywords", JSONArray(it)) }
            publishedDate?.let { put("published_date", it) }
            contentType?.let { put("content_type", it) }
            // Navigation
            previousPageUrl?.let { put("previous_page_url", it) }
            previousPageTitle?.let { put("previous_page_title", it) }
            // Screen
            screenWidth?.let { put("screen_width", it) }
            screenHeight?.let { put("screen_height", it) }
            // UTM
            utmSource?.let { put("utm_source", it) }
            utmMedium?.let { put("utm_medium", it) }
            utmCampaign?.let { put("utm_campaign", it) }
            utmContent?.let { put("utm_content", it) }
            utmTerm?.let { put("utm_term", it) }
            // Engagement
            activeTimeSeconds?.let { put("active_time_seconds", it) }
            pingCounter?.let { put("ping_counter", it) }
            // User
            userType?.let { put("user_type", it) }
            userSegments?.let { put("user_segments", JSONArray(it)) }
        }
    }
}

/**
 * Screen metadata for article/content tracking
 */
data class ScreenMetadata(
    /** Content creator/editor (e.g., "Dijital Haber Merkezi") */
    val creator: String? = null,
    /** Article authors (e.g., ["Ahmet Hakan", "Mehmet Yılmaz"]) */
    val authors: List<String>? = null,
    /** Content section/category (e.g., "Spor", "Ekonomi") */
    val section: String? = null,
    /** Content keywords/tags */
    val keywords: List<String>? = null,
    /** Publication date */
    val publishedDate: Date? = null,
    /** Content type (e.g., "article", "video", "gallery", "live") */
    val contentType: String? = null
)

/**
 * UTM parameters for campaign tracking (from deep links)
 */
data class UTMParameters(
    val source: String? = null,
    val medium: String? = null,
    val campaign: String? = null,
    val content: String? = null,
    val term: String? = null
) {
    companion object {
        /**
         * Parse UTM parameters from a URI
         */
        @JvmStatic
        fun from(uri: Uri): UTMParameters {
            return UTMParameters(
                source = uri.getQueryParameter("utm_source"),
                medium = uri.getQueryParameter("utm_medium"),
                campaign = uri.getQueryParameter("utm_campaign"),
                content = uri.getQueryParameter("utm_content"),
                term = uri.getQueryParameter("utm_term")
            )
        }

        /**
         * Parse UTM parameters from a URL string
         */
        @JvmStatic
        fun from(url: String): UTMParameters {
            return try {
                from(Uri.parse(url))
            } catch (e: Exception) {
                UTMParameters()
            }
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

/**
 * User type classification (similar to Marfeel)
 */
enum class UserType(val value: String) {
    ANONYMOUS("anonymous"),
    LOGGED_IN("logged"),
    SUBSCRIBER("subscriber"),
    PREMIUM("premium");

    companion object {
        @JvmStatic
        fun fromString(value: String): UserType {
            return values().find { it.value == value } ?: ANONYMOUS
        }
    }
}

/**
 * Conversion event for tracking user actions/goals
 */
data class Conversion(
    /** Unique conversion identifier */
    val id: String,
    /** Conversion type (e.g., "subscription", "registration", "purchase") */
    val type: String,
    /** Optional conversion value (e.g., revenue amount) */
    val value: Double? = null,
    /** Optional currency code (e.g., "TRY", "USD") */
    val currency: String? = null,
    /** Optional additional properties */
    val properties: Map<String, Any>? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("type", type)
            value?.let { put("value", it) }
            currency?.let { put("currency", it) }
            properties?.let { put("properties", JSONObject(it)) }
        }
    }
}
