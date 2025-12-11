package site.dmbi.analytics

import android.content.Context
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import org.json.JSONArray
import org.json.JSONObject
import site.dmbi.analytics.models.AnalyticsEvent
import site.dmbi.analytics.models.Conversion
import site.dmbi.analytics.models.ScreenMetadata
import site.dmbi.analytics.models.UTMParameters
import site.dmbi.analytics.models.UserType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Core event tracking functionality
 */
internal class EventTracker(
    private val context: Context,
    private val config: DMBIConfiguration,
    private val sessionManager: SessionManager,
    private val networkQueue: NetworkQueue
) {
    private var isLoggedIn: Boolean = false
    private var currentScreen: ScreenInfo? = null
    private var screenEntryTime: Long? = null

    // Previous screen tracking (like web's previous_page_url)
    private var previousScreenUrl: String? = null
    private var previousScreenTitle: String? = null

    // UTM parameters (from deep links)
    private var currentUTM: UTMParameters? = null

    // Referrer (deep link source, push notification, etc.)
    private var currentReferrer: String? = null

    // User classification
    private var userType: UserType = UserType.ANONYMOUS
    private val userSegments = mutableSetOf<String>()

    // Pending conversions (sent with next heartbeat like Marfeel)
    private val pendingConversions = mutableListOf<Conversion>()

    // Scroll tracker reference
    private var scrollTracker: ScrollTracker? = null

    // Heartbeat manager reference (for active time and interaction recording)
    private var heartbeatManager: HeartbeatManager? = null

    // Screen dimensions cache
    private val screenDimensions: Pair<Int, Int> by lazy {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(metrics)
        Pair(metrics.widthPixels, metrics.heightPixels)
    }

    private data class ScreenInfo(
        val name: String,
        val url: String,
        val title: String?,
        val metadata: ScreenMetadata?
    )

    // MARK: - Configuration

    fun setHeartbeatManager(manager: HeartbeatManager) {
        this.heartbeatManager = manager
    }

    fun setScrollTracker(tracker: ScrollTracker) {
        this.scrollTracker = tracker
    }

    // MARK: - User State

    fun setLoggedIn(loggedIn: Boolean) {
        isLoggedIn = loggedIn
        if (loggedIn && userType == UserType.ANONYMOUS) {
            userType = UserType.LOGGED_IN
        }
    }

    fun setUserType(type: UserType) {
        this.userType = type
        if (type != UserType.ANONYMOUS) {
            isLoggedIn = true
        }
    }

    // MARK: - User Segments

    fun addUserSegment(segment: String) {
        userSegments.add(segment)
    }

    fun removeUserSegment(segment: String) {
        userSegments.remove(segment)
    }

    fun setUserSegments(segments: Set<String>) {
        userSegments.clear()
        userSegments.addAll(segments)
    }

    fun clearUserSegments() {
        userSegments.clear()
    }

    fun getUserSegments(): Set<String> = userSegments.toSet()

    // MARK: - Conversions

    fun trackConversion(conversion: Conversion) {
        pendingConversions.add(conversion)

        // Also send as immediate event
        val customData = mutableMapOf<String, Any>(
            "conversion_id" to conversion.id,
            "conversion_type" to conversion.type
        )
        conversion.value?.let { customData["conversion_value"] = it }
        conversion.currency?.let { customData["conversion_currency"] = it }
        conversion.properties?.let { customData.putAll(it) }

        val event = createEvent(
            eventType = "conversion",
            pageUrl = currentScreen?.url ?: "app://conversion",
            pageTitle = currentScreen?.title,
            customData = customData
        )
        enqueue(event)
    }

    // MARK: - UTM & Referrer

    fun setUTMParameters(utm: UTMParameters) {
        this.currentUTM = utm
    }

    fun setReferrer(referrer: String) {
        this.currentReferrer = referrer
    }

    fun handleDeepLink(uri: Uri) {
        // Parse UTM parameters from deep link
        this.currentUTM = UTMParameters.from(uri)
        // Set referrer as the deep link scheme
        this.currentReferrer = uri.scheme ?: "deeplink"
    }

    // MARK: - User Interaction (for dynamic heartbeat)

    fun recordInteraction() {
        heartbeatManager?.recordInteraction()
    }

    // MARK: - Screen Tracking

    fun trackScreen(name: String, url: String, title: String?, metadata: ScreenMetadata? = null) {
        // Record interaction
        recordInteraction()

        // Track exit from previous screen if any
        currentScreen?.let { prev ->
            trackScreenExit(prev.name, prev.url, prev.title, prev.metadata)
        }

        // Save previous screen info BEFORE updating current
        previousScreenUrl = currentScreen?.url
        previousScreenTitle = currentScreen?.title

        // Record new screen
        currentScreen = ScreenInfo(name, url, title, metadata)
        screenEntryTime = System.currentTimeMillis()

        // Reset scroll tracking for new screen
        scrollTracker?.reset()

        // Format published date if present
        var publishedDateString: String? = null
        metadata?.publishedDate?.let { date ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            publishedDateString = dateFormat.format(date)
        }

        val event = createEvent(
            eventType = "screen_view",
            pageUrl = url,
            pageTitle = title,
            customData = mapOf("screen_name" to name),
            // Article metadata
            creator = metadata?.creator,
            articleAuthor = metadata?.authors?.joinToString(", "),
            articleSection = metadata?.section,
            articleKeywords = metadata?.keywords,
            publishedDate = publishedDateString,
            contentType = metadata?.contentType
        )
        enqueue(event)
    }

    private fun trackScreenExit(name: String, url: String, title: String?, metadata: ScreenMetadata?) {
        val entryTime = screenEntryTime ?: return
        val duration = ((System.currentTimeMillis() - entryTime) / 1000).toInt()
        val scrollDepth = scrollTracker?.maxScrollDepth

        val event = createEvent(
            eventType = "screen_exit",
            pageUrl = url,
            pageTitle = title,
            duration = duration,
            scrollDepth = scrollDepth,
            customData = mapOf("screen_name" to name)
        )
        enqueue(event)
    }

    // MARK: - Scroll Tracking

    fun reportScrollDepth(percent: Int) {
        scrollTracker?.reportScrollDepth(percent)
        recordInteraction()
    }

    fun getCurrentScrollDepth(): Int {
        return scrollTracker?.maxScrollDepth ?: 0
    }

    // MARK: - App Lifecycle

    fun trackAppOpen(isNewSession: Boolean) {
        val event = createEvent(
            eventType = "app_open",
            pageUrl = currentScreen?.url ?: "app://launch",
            pageTitle = null,
            customData = mapOf("is_new_session" to isNewSession)
        )
        enqueue(event)
    }

    fun trackAppClose() {
        // Track screen exit for current screen
        currentScreen?.let { current ->
            trackScreenExit(current.name, current.url, current.title, current.metadata)
        }

        val event = createEvent(
            eventType = "app_close",
            pageUrl = currentScreen?.url ?: "app://close",
            pageTitle = null
        )
        enqueue(event)
    }

    // MARK: - Video Tracking

    fun trackVideoImpression(videoId: String, title: String?, duration: Float?) {
        recordInteraction()
        val event = createEvent(
            eventType = "video_impression",
            pageUrl = currentScreen?.url ?: "app://video",
            pageTitle = currentScreen?.title,
            videoId = videoId,
            videoTitle = title,
            videoDuration = duration
        )
        enqueue(event)
    }

    fun trackVideoPlay(videoId: String, title: String?, duration: Float?, position: Float?) {
        recordInteraction()
        val event = createEvent(
            eventType = "video_play",
            pageUrl = currentScreen?.url ?: "app://video",
            pageTitle = currentScreen?.title,
            videoId = videoId,
            videoTitle = title,
            videoDuration = duration,
            videoPosition = position
        )
        enqueue(event)
    }

    fun trackVideoProgress(videoId: String, duration: Float?, position: Float?, percent: Int) {
        recordInteraction()
        val event = createEvent(
            eventType = "video_quartile",
            pageUrl = currentScreen?.url ?: "app://video",
            pageTitle = currentScreen?.title,
            videoId = videoId,
            videoDuration = duration,
            videoPosition = position,
            videoPercent = percent
        )
        enqueue(event)
    }

    fun trackVideoPause(videoId: String, position: Float?, percent: Int?) {
        recordInteraction()
        val event = createEvent(
            eventType = "video_pause",
            pageUrl = currentScreen?.url ?: "app://video",
            pageTitle = currentScreen?.title,
            videoId = videoId,
            videoPosition = position,
            videoPercent = percent
        )
        enqueue(event)
    }

    fun trackVideoComplete(videoId: String, duration: Float?) {
        recordInteraction()
        val event = createEvent(
            eventType = "video_complete",
            pageUrl = currentScreen?.url ?: "app://video",
            pageTitle = currentScreen?.title,
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
        // Set referrer when opening from push
        this.currentReferrer = "push_notification"

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

    fun trackHeartbeat(activeTimeSeconds: Int, pingCounter: Int) {
        // Include pending conversions in heartbeat (like Marfeel)
        val customData = mutableMapOf<String, Any>()

        if (pendingConversions.isNotEmpty()) {
            val conversionsJson = JSONArray()
            pendingConversions.forEach { conversionsJson.put(it.toJson()) }
            customData["pending_conversions"] = conversionsJson.toString()
            pendingConversions.clear()
        }

        val event = createEvent(
            eventType = "heartbeat",
            pageUrl = currentScreen?.url ?: "app://heartbeat",
            pageTitle = currentScreen?.title,
            scrollDepth = scrollTracker?.maxScrollDepth,
            customData = customData.takeIf { it.isNotEmpty() },
            activeTimeSeconds = activeTimeSeconds,
            pingCounter = pingCounter
        )
        enqueue(event)
    }

    // MARK: - Custom Events

    fun trackCustomEvent(name: String, properties: Map<String, Any>?) {
        recordInteraction()
        val event = createEvent(
            eventType = name,
            pageUrl = currentScreen?.url ?: "app://custom",
            pageTitle = currentScreen?.title,
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
        videoPercent: Int? = null,
        // Article metadata
        creator: String? = null,
        articleAuthor: String? = null,
        articleSection: String? = null,
        articleKeywords: List<String>? = null,
        publishedDate: String? = null,
        contentType: String? = null,
        // Engagement
        activeTimeSeconds: Int? = null,
        pingCounter: Int? = null
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
            referrer = currentReferrer,
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
            videoPercent = videoPercent,
            // Article metadata
            creator = creator,
            articleAuthor = articleAuthor,
            articleSection = articleSection,
            articleKeywords = articleKeywords,
            publishedDate = publishedDate,
            contentType = contentType,
            // Previous screen
            previousPageUrl = previousScreenUrl,
            previousPageTitle = previousScreenTitle,
            // Screen dimensions
            screenWidth = screenDimensions.first,
            screenHeight = screenDimensions.second,
            // UTM parameters
            utmSource = currentUTM?.source,
            utmMedium = currentUTM?.medium,
            utmCampaign = currentUTM?.campaign,
            utmContent = currentUTM?.content,
            utmTerm = currentUTM?.term,
            // Engagement
            activeTimeSeconds = activeTimeSeconds,
            pingCounter = pingCounter,
            // User
            userType = userType.value,
            userSegments = userSegments.toList().takeIf { it.isNotEmpty() }
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
