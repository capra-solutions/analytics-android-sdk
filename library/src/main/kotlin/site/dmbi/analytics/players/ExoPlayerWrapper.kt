package site.dmbi.analytics.players

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import site.dmbi.analytics.DMBIAnalytics

/**
 * Wrapper for ExoPlayer that automatically tracks video analytics events.
 *
 * Usage:
 * ```kotlin
 * val exoPlayer = ExoPlayer.Builder(context).build()
 * val wrapper = ExoPlayerWrapper(exoPlayer)
 * wrapper.attach(
 *     videoId = "video-123",
 *     title = "My Video Title",
 *     duration = 180f // optional, auto-detected if not provided
 * )
 *
 * // When done:
 * wrapper.detach()
 * ```
 */
class ExoPlayerWrapper(private val player: ExoPlayer) {

    private var videoId: String? = null
    private var videoTitle: String? = null
    private var videoDuration: Float? = null
    private var lastReportedQuartile: Int = 0
    private var hasTrackedImpression: Boolean = false
    private var isPlaying: Boolean = false

    private val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    if (!hasTrackedImpression) {
                        trackImpression()
                        hasTrackedImpression = true
                    }
                }
                Player.STATE_ENDED -> {
                    trackComplete()
                }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (player.playbackState == Player.STATE_READY) {
                if (playWhenReady && !isPlaying) {
                    trackPlay()
                    isPlaying = true
                } else if (!playWhenReady && isPlaying) {
                    trackPause()
                    isPlaying = false
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            // User seeked - check for quartile progress
            checkQuartileProgress()
        }
    }

    /**
     * Attach analytics tracking to the player.
     *
     * @param videoId Unique identifier for the video
     * @param title Optional video title
     * @param duration Optional video duration in seconds (auto-detected if not provided)
     */
    fun attach(videoId: String, title: String? = null, duration: Float? = null) {
        this.videoId = videoId
        this.videoTitle = title
        this.videoDuration = duration
        this.lastReportedQuartile = 0
        this.hasTrackedImpression = false
        this.isPlaying = false

        player.addListener(playerListener)

        // Start progress monitoring
        startProgressMonitoring()
    }

    /**
     * Detach analytics tracking from the player.
     * Call this when the player is being released.
     */
    fun detach() {
        player.removeListener(playerListener)
        videoId = null
        videoTitle = null
        videoDuration = null
    }

    private fun trackImpression() {
        val id = videoId ?: return
        val duration = videoDuration ?: (player.duration / 1000f).takeIf { it > 0 }

        DMBIAnalytics.trackVideoImpression(
            videoId = id,
            title = videoTitle,
            duration = duration
        )
    }

    private fun trackPlay() {
        val id = videoId ?: return
        val duration = videoDuration ?: (player.duration / 1000f).takeIf { it > 0 }
        val position = player.currentPosition / 1000f

        DMBIAnalytics.trackVideoPlay(
            videoId = id,
            title = videoTitle,
            duration = duration,
            position = position
        )
    }

    private fun trackPause() {
        val id = videoId ?: return
        val position = player.currentPosition / 1000f
        val percent = calculatePercent()

        DMBIAnalytics.trackVideoPause(
            videoId = id,
            position = position,
            percent = percent
        )
    }

    private fun trackComplete() {
        val id = videoId ?: return
        val duration = videoDuration ?: (player.duration / 1000f).takeIf { it > 0 }

        DMBIAnalytics.trackVideoComplete(
            videoId = id,
            duration = duration
        )
    }

    private fun trackQuartile(percent: Int) {
        val id = videoId ?: return
        val duration = videoDuration ?: (player.duration / 1000f).takeIf { it > 0 }
        val position = player.currentPosition / 1000f

        DMBIAnalytics.trackVideoProgress(
            videoId = id,
            duration = duration,
            position = position,
            percent = percent
        )
    }

    private fun calculatePercent(): Int {
        val duration = player.duration
        if (duration <= 0) return 0
        return ((player.currentPosition.toFloat() / duration) * 100).toInt()
    }

    private fun checkQuartileProgress() {
        val percent = calculatePercent()
        val quartiles = listOf(25, 50, 75, 100)

        for (quartile in quartiles) {
            if (percent >= quartile && lastReportedQuartile < quartile) {
                trackQuartile(quartile)
                lastReportedQuartile = quartile
            }
        }
    }

    private fun startProgressMonitoring() {
        // Use a handler to periodically check progress
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (videoId != null && isPlaying) {
                    checkQuartileProgress()
                    handler.postDelayed(this, 1000) // Check every second
                }
            }
        }
        handler.post(runnable)
    }
}
