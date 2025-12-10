package site.dmbi.analytics

import kotlinx.coroutines.*

/**
 * Manages periodic heartbeat events for concurrent user tracking
 */
internal class HeartbeatManager(
    private val interval: Long
) {
    private var tracker: EventTracker? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var heartbeatJob: Job? = null

    fun setTracker(tracker: EventTracker) {
        this.tracker = tracker
    }

    /** Start sending heartbeats */
    fun start() {
        stop()

        heartbeatJob = scope.launch {
            while (isActive) {
                delay(interval)
                tracker?.trackHeartbeat()
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
        stop()
    }

    /** Resume heartbeats (when app returns to foreground) */
    fun resume() {
        start()
    }
}
