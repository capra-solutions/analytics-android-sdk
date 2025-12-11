package site.dmbi.analytics

import kotlinx.coroutines.*

/**
 * Manages periodic heartbeat events for concurrent user tracking
 * Features:
 * - Dynamic interval: increases when user is inactive
 * - Active time tracking: only counts foreground time
 * - Pause/resume on app background
 */
internal class HeartbeatManager(
    private val baseInterval: Long,
    private val maxInterval: Long = baseInterval * 4,
    private val inactivityThreshold: Long = 30_000L // 30 seconds without interaction
) {
    private var tracker: EventTracker? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var heartbeatJob: Job? = null

    // Active time tracking
    private var sessionStartTime: Long = 0L
    private var totalActiveTime: Long = 0L
    private var lastPauseTime: Long? = null

    // Dynamic interval
    private var lastInteractionTime: Long = System.currentTimeMillis()
    private var currentInterval: Long = baseInterval

    // Heartbeat counter
    private var pingCounter: Int = 0

    /**
     * Total active time in seconds (excluding background time)
     */
    val activeTimeSeconds: Int
        get() {
            val currentSessionTime = if (lastPauseTime == null && sessionStartTime > 0) {
                System.currentTimeMillis() - sessionStartTime
            } else {
                0L
            }
            return ((totalActiveTime + currentSessionTime) / 1000).toInt()
        }

    /**
     * Current ping counter
     */
    val currentPingCounter: Int
        get() = pingCounter

    fun setTracker(tracker: EventTracker) {
        this.tracker = tracker
    }

    /** Record user interaction to reset inactivity timer */
    fun recordInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        // Reset to base interval on interaction
        currentInterval = baseInterval
    }

    /** Start sending heartbeats */
    fun start() {
        stop()
        sessionStartTime = System.currentTimeMillis()
        lastPauseTime = null
        pingCounter = 0

        heartbeatJob = scope.launch {
            while (isActive) {
                delay(currentInterval)

                // Calculate dynamic interval based on inactivity
                val timeSinceInteraction = System.currentTimeMillis() - lastInteractionTime
                currentInterval = if (timeSinceInteraction > inactivityThreshold) {
                    // Gradually increase interval when inactive (up to maxInterval)
                    (currentInterval * 1.5).toLong().coerceAtMost(maxInterval)
                } else {
                    baseInterval
                }

                pingCounter++
                tracker?.trackHeartbeat(activeTimeSeconds, pingCounter)
            }
        }
    }

    /** Stop sending heartbeats */
    fun stop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    /** Pause heartbeats (when app goes to background) */
    fun pause() {
        // Record active time before pausing
        if (sessionStartTime > 0 && lastPauseTime == null) {
            totalActiveTime += System.currentTimeMillis() - sessionStartTime
        }
        lastPauseTime = System.currentTimeMillis()
        stop()
    }

    /** Resume heartbeats (when app returns to foreground) */
    fun resume() {
        sessionStartTime = System.currentTimeMillis()
        lastPauseTime = null
        // Don't reset pingCounter or totalActiveTime - continue from where we left
        start()
    }

    /** Reset all tracking (for new session) */
    fun resetSession() {
        totalActiveTime = 0L
        sessionStartTime = System.currentTimeMillis()
        lastPauseTime = null
        pingCounter = 0
        currentInterval = baseInterval
        lastInteractionTime = System.currentTimeMillis()
    }
}
