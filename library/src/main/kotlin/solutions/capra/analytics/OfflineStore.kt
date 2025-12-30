package solutions.capra.analytics

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import solutions.capra.analytics.models.AnalyticsEvent
import solutions.capra.analytics.models.StoredEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * File-based offline event storage
 */
internal class OfflineStore(
    context: Context,
    private val maxEvents: Int,
    private val retentionDays: Int,
    private val debugLogging: Boolean
) {
    private val file: File
    private val events = mutableListOf<StoredEvent>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        val dir = File(context.filesDir, "capra_analytics")
        if (!dir.exists()) dir.mkdirs()
        file = File(dir, "events.json")

        loadEvents()
        cleanupOldEvents()
    }

    /** Store an event for later sending */
    fun store(event: AnalyticsEvent) {
        scope.launch {
            synchronized(events) {
                events.add(StoredEvent(event = event))

                // Trim if over limit
                if (events.size > maxEvents) {
                    val excess = events.size - maxEvents
                    repeat(excess) { events.removeAt(0) }
                }

                saveEvents()
            }
        }
    }

    /** Fetch pending events for retry */
    fun fetchPendingEvents(): List<StoredEvent> {
        synchronized(events) {
            return events.toList()
        }
    }

    /** Delete events by IDs (after successful send) */
    fun delete(ids: List<String>) {
        scope.launch {
            synchronized(events) {
                events.removeAll { it.id in ids }
                saveEvents()
            }
        }
    }

    /** Increment retry count for an event */
    fun incrementRetry(id: String, maxRetries: Int) {
        scope.launch {
            synchronized(events) {
                val index = events.indexOfFirst { it.id == id }
                if (index >= 0) {
                    events[index].retryCount++

                    if (events[index].retryCount > maxRetries) {
                        events.removeAt(index)
                    }

                    saveEvents()
                }
            }
        }
    }

    private fun loadEvents() {
        if (!file.exists()) return

        try {
            val content = file.readText()
            val jsonArray = JSONArray(content)

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                val event = parseEvent(json.getJSONObject("event"))
                val stored = StoredEvent(
                    id = json.getString("id"),
                    event = event,
                    createdAt = json.getLong("createdAt"),
                    retryCount = json.optInt("retryCount", 0)
                )
                events.add(stored)
            }
        } catch (e: Exception) {
            if (debugLogging) {
                Log.e(TAG, "Failed to load events: ${e.message}")
            }
            events.clear()
        }
    }

    private fun saveEvents() {
        try {
            val jsonArray = JSONArray()

            events.forEach { stored ->
                val json = JSONObject().apply {
                    put("id", stored.id)
                    put("event", stored.event.toJson())
                    put("createdAt", stored.createdAt)
                    put("retryCount", stored.retryCount)
                }
                jsonArray.put(json)
            }

            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            if (debugLogging) {
                Log.e(TAG, "Failed to save events: ${e.message}")
            }
        }
    }

    private fun cleanupOldEvents() {
        scope.launch {
            synchronized(events) {
                val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
                val originalSize = events.size

                events.removeAll { it.createdAt < cutoff }

                if (events.size != originalSize) {
                    saveEvents()
                }
            }
        }
    }

    private fun parseEvent(json: JSONObject): AnalyticsEvent {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        return AnalyticsEvent(
            siteId = json.getString("site_id"),
            sessionId = json.getString("session_id"),
            userId = json.getString("user_id"),
            eventType = json.getString("event_type"),
            pageUrl = json.getString("page_url"),
            pageTitle = json.optString("page_title", null),
            referrer = json.optString("referrer", null),
            deviceType = json.getString("device_type"),
            userAgent = json.getString("user_agent"),
            isLoggedIn = json.optBoolean("is_logged_in", false),
            timestamp = try {
                dateFormat.parse(json.getString("timestamp")) ?: Date()
            } catch (e: Exception) {
                Date()
            },
            duration = if (json.has("duration")) json.getInt("duration") else null,
            scrollDepth = if (json.has("scroll_depth")) json.getInt("scroll_depth") else null,
            customData = json.optString("custom_data", null),
            videoId = json.optString("video_id", null),
            videoTitle = json.optString("video_title", null),
            videoDuration = if (json.has("video_duration")) json.getDouble("video_duration").toFloat() else null,
            videoPosition = if (json.has("video_position")) json.getDouble("video_position").toFloat() else null,
            videoPercent = if (json.has("video_percent")) json.getInt("video_percent") else null
        )
    }

    companion object {
        private const val TAG = "CapraAnalytics"
    }
}
