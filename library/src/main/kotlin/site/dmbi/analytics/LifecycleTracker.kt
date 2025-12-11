package site.dmbi.analytics

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Tracks app lifecycle events (open, close, background, foreground)
 */
internal class LifecycleTracker(
    private val sessionTimeout: Long
) : DefaultLifecycleObserver {

    private var tracker: EventTracker? = null
    private var sessionManager: SessionManager? = null
    private var heartbeatManager: HeartbeatManager? = null

    private var backgroundTime: Long? = null

    fun configure(
        tracker: EventTracker,
        sessionManager: SessionManager,
        heartbeatManager: HeartbeatManager
    ) {
        this.tracker = tracker
        this.sessionManager = sessionManager
        this.heartbeatManager = heartbeatManager

        // Register for lifecycle events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // App came to foreground
        val shouldStartNewSession: Boolean

        val bgTime = backgroundTime
        if (bgTime != null) {
            // Check if we were in background longer than session timeout
            val backgroundDuration = System.currentTimeMillis() - bgTime
            shouldStartNewSession = backgroundDuration > sessionTimeout
        } else {
            // First launch
            shouldStartNewSession = true
        }

        if (shouldStartNewSession) {
            sessionManager?.startNewSession()
            // Reset heartbeat manager for new session
            heartbeatManager?.resetSession()
        }

        // Track app open
        tracker?.trackAppOpen(isNewSession = shouldStartNewSession)

        // Resume heartbeats
        heartbeatManager?.resume()

        // Retry offline events when coming back online
        tracker?.retryOfflineEvents()

        backgroundTime = null
    }

    override fun onStop(owner: LifecycleOwner) {
        // App went to background
        backgroundTime = System.currentTimeMillis()

        // Track app close
        tracker?.trackAppClose()

        // Pause heartbeats
        heartbeatManager?.pause()

        // Flush any pending events
        tracker?.flush()
    }
}
