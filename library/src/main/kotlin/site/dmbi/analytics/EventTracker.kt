package site.dmbi.analytics

import android.util.Log
import org.json.JSONObject
import site.dmbi.analytics.models.AnalyticsEvent
import java.util.*

/**
 * Core event tracking functionality
 */
internal class EventTracker(
    private val config: DMBIConfiguration,
    private val sessionManager: SessionManager,
    private val networkQueue: NetworkQueue
) {
    private var isLoggedIn: Boolean = false
    private var currentScreen: Triple<String, String, String?>? = null // name, url, title
    private var screenEntryTime: Long? = null

    // MARK: - User State

    fun setLoggedIn(loggedIn: Boolean) {
        isLoggedIn = loggedIn
    }

    // MARK: - Screen Tracking

    fun trackScreen(name: String, url: String, title: String?) {
        // Track exit from previous screen if any
        currentScreen?.let { (prevName, prevUrl, prevTitle) ->
            trackScreenExit(prevName, prevUrl, prevTitle)
        }

        // Record new screen
        currentScreen = Triple(name, url, title)
        screenEntryTime = System.currentTimeMillis()

        val event = createEvent(
            eventType = "screen_view",
            pageUrl = url,
            pageTitle = title,
            customData = mapOf("screen_name" to name)
        )
        enqueue(event)
    }

    private fun trackScreenExit(name: String, url: String, title: String?) {
        val entryTime = screenEntryTime ?: return
        val duration = ((System.currentTimeMillis() - entryTime) / 1000).toInt()

        val event = createEvent(
            eventType = "screen_exit",
            pageUrl = url,
            pageTitle = title,
            duration = duration,
            customData = mapOf("screen_name" to name)
        )
        enqueue(event)
    }

    // MARK: - App Lifecycle

    fun trackAppOpen(isNewSession: Boolean) {
        val event = createEvent(
            eventType = "app_open",
            pageUrl = currentScreen?.second ?: "app://launch",
            pageTitle = null,
            customData = mapOf("is_new_session" to isNewSession)
        )
        enqueue(event)
    }

    fun trackAppClose() {
        // Track screen exit for current screen
        currentScreen?.let { (name, url, title) ->
            trackScreenExit(name, url, title)
        }

        val event = createEvent(
            eventType = "app_close",
            pageUrl = currentScreen?.second ?: "app://close",
            pageTitle = null
        )
        enqueue(event)
    }

    // MARK: - Video Tracking

    fun trackVideoImpression(videoId: String, title: String?, duration: Float?) {
        val event = createEvent(
            eventType = "video_impression",
            pageUrl = currentScreen?.second ?: "app://video",
            pageTitle = currentScreen?.third,
            videoId = videoId,
            videoTitle = title,
            videoDuration = duration
        )
        enqueue(event)
    }

    fun trackVideoPlay(videoId: String, title: String?, duration: Float?, position: Float?) {
        val event = createEvent(
            eventType = "video_play",
            pageUrl = currentScreen?.second ?: "app://video",
            pageTitle = currentScreen?.third,
            videoId = videoId,
            videoTitle = title,
            videoDuration = duration,
            videoPosition = position
        )
        enqueue(event)
    }

    fun trackVideoProgress(videoId: String, duration: Float?, position: Float?, percent: Int) {
        val event = createEvent(
            eventType = "video_quartile",
            pageUrl = currentScreen?.second ?: "app://video",
            pageTitle = currentScreen?.third,
            videoId = videoId,
            videoDuration = duration,
            videoPosition = position,
            videoPercent = percent
        )
        enqueue(event)
    }

    fun trackVideoPause(videoId: String, position: Float?, percent: Int?) {
        val event = createEvent(
            eventType = "video_pause",
            pageUrl = currentScreen?.second ?: "app://video",
            pageTitle = currentScreen?.third,
            videoId = videoId,
            videoPosition = position,
            videoPercent = percent
        )
        enqueue(event)
    }

    fun trackVideoComplete(videoId: String, duration: Float?) {
        val event = createEvent(
            eventType = "video_complete",
            pageUrl = currentScreen?.second ?: "app://video",
            pageTitle = currentScreen?.third,
            videoId = videoId,
            videoDuration = duration,
            videoPercent = 100
        )
        enqueue(event)
    }

    // MARK: - Push Notification Tracking

    fun trackPushReceived(notificationId: String?, title: String?, campaign: String?) {
        val customData = mutableMapOf<String, Any>()
        notificationId?.let { customData["notification_id"] = it }
        campaign?.let { customData["campaign"] = it }

        val event = createEvent(
            eventType = "push_received",
            pageUrl = "app://push",
            pageTitle = title,
            customData = customData.takeIf { it.isNotEmpty() }
        )
        enqueue(event)
    }

    fun trackPushOpened(notificationId: String?, title: String?, campaign: String?) {
        val customData = mutableMapOf<String, Any>()
        notificationId?.let { customData["notification_id"] = it }
        campaign?.let { customData["campaign"] = it }

        val event = createEvent(
            eventType = "push_opened",
            pageUrl = "app://push",
            pageTitle = title,
            customData = customData.takeIf { it.isNotEmpty() }
        )
        enqueue(event)
    }

    // MARK: - Heartbeat

    fun trackHeartbeat() {
        val event = createEvent(
            eventType = "heartbeat",
            pageUrl = currentScreen?.second ?: "app://heartbeat",
            pageTitle = currentScreen?.third
        )
        enqueue(event)
    }

    // MARK: - Custom Events

    fun trackCustomEvent(name: String, properties: Map<String, Any>?) {
        val event = createEvent(
            eventType = name,
            pageUrl = currentScreen?.second ?: "app://custom",
            pageTitle = currentScreen?.third,
            customData = properties
        )
        enqueue(event)
    }

    // MARK: - Network

    fun flush() {
        networkQueue.flush()
    }

    fun retryOfflineEvents() {
        networkQueue.retryOfflineEvents()
    }

    // MARK: - Event Creation

    private fun createEvent(
        eventType: String,
        pageUrl: String,
        pageTitle: String?,
        duration: Int? = null,
        scrollDepth: Int? = null,
        customData: Map<String, Any>? = null,
        videoId: String? = null,
        videoTitle: String? = null,
        videoDuration: Float? = null,
        videoPosition: Float? = null,
        videoPercent: Int? = null
    ): AnalyticsEvent {
        sessionManager.updateActivity()

        val customDataString = customData?.let {
            JSONObject(it).toString()
        }

        return AnalyticsEvent(
            siteId = config.siteId,
            sessionId = sessionManager.sessionId,
            userId = sessionManager.userId,
            eventType = eventType,
            pageUrl = pageUrl,
            pageTitle = pageTitle,
            referrer = null,
            deviceType = sessionManager.deviceType,
            userAgent = sessionManager.userAgent,
            isLoggedIn = isLoggedIn,
            timestamp = Date(),
            duration = duration,
            scrollDepth = scrollDepth,
            customData = customDataString,
            videoId = videoId,
            videoTitle = videoTitle,
            videoDuration = videoDuration,
            videoPosition = videoPosition,
            videoPercent = videoPercent
        )
    }

    private fun enqueue(event: AnalyticsEvent) {
        networkQueue.enqueue(event)

        if (config.debugLogging) {
            Log.d(TAG, "Event: ${event.eventType} - ${event.pageUrl}")
        }
    }

    companion object {
        private const val TAG = "DMBIAnalytics"
    }
}
