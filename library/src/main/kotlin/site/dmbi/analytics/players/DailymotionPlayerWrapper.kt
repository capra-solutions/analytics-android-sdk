package site.dmbi.analytics.players

import com.dailymotion.player.android.sdk.Dailymotion
import com.dailymotion.player.android.sdk.PlayerView
import com.dailymotion.player.android.sdk.listeners.PlayerListener
import site.dmbi.analytics.DMBIAnalytics

/**
 * Wrapper for Dailymotion Android Player that automatically tracks video analytics events.
 *
 * Usage:
 * ```kotlin
 * val playerView = findViewById<PlayerView>(R.id.dailymotionPlayer)
 *
 * Dailymotion.createPlayer(context, playerId = "your-player-id") { player ->
 *     playerView.setPlayer(player)
 *
 *     val wrapper = DailymotionPlayerWrapper(playerView)
 *     wrapper.attach(
 *         videoId = "x8abc123",
 *         title = "Video Title",
 *         duration = 180f
 *     )
 *
 *     player.loadContent(videoId = "x8abc123")
 * }
 * ```
 */
class DailymotionPlayerWrapper(private val playerView: PlayerView) {

    private var videoId: String? = null
    private var videoTitle: String? = null
    private var videoDuration: Float? = null
    private var lastReportedQuartile: Int = 0
    private var hasTrackedImpression: Boolean = false
    private var isPlaying: Boolean = false
    private var currentPosition: Double = 0.0
    private var totalDuration: Double = 0.0

    private val playerListener = object : PlayerListener {

        override fun onVideoStart() {
            if (!hasTrackedImpression) {
                trackImpression()
                hasTrackedImpression = true
            }
        }

        override fun onPlay() {
            if (!isPlaying) {
                trackPlay()
                isPlaying = true
            }
        }

        override fun onPause() {
            if (isPlaying) {
                trackPause()
                isPlaying = false
            }
        }

        override fun onVideoEnd() {
            trackComplete()
            isPlaying = false
        }

        override fun onTimeUpdate(time: Double) {
            currentPosition = time
            checkQuartileProgress()
        }

        override fun onDurationChange(duration: Double) {
            totalDuration = duration
        }

        // Other required overrides with empty implementations
        override fun onVideoChange(videoId: String) {}
        override fun onPlaylistItemChange(videoId: String, index: Int) {}
        override fun onPlaylistChange(playlist: List<String>) {}
        override fun onQualitiesAvailable(qualities: List<String>) {}
        override fun onQualityChange(quality: String) {}
        override fun onSeekStart(time: Double) {}
        override fun onSeekEnd(time: Double) {}
        override fun onVolumeChange(volume: Double) {}
        override fun onFullscreenChange(isFullscreen: Boolean) {}
        override fun onBuffering() {}
        override fun onPlaying() {}
        override fun onError(error: String) {}
    }

    /**
     * Attach analytics tracking to the Dailymotion player.
     *
     * @param videoId Dailymotion video ID
     * @param title Optional video title
     * @param duration Optional video duration in seconds
     */
    fun attach(videoId: String, title: String? = null, duration: Float? = null) {
        this.videoId = videoId
        this.videoTitle = title
        this.videoDuration = duration
        this.lastReportedQuartile = 0
        this.hasTrackedImpression = false
        this.isPlaying = false

        playerView.addListener(playerListener)
    }

    /**
     * Detach analytics tracking from the player.
     */
    fun detach() {
        playerView.removeListener(playerListener)
        videoId = null
        videoTitle = null
    }

    private fun trackImpression() {
        val id = videoId ?: return
        val duration = videoDuration ?: totalDuration.toFloat().takeIf { it > 0 }

        DMBIAnalytics.trackVideoImpression(
            videoId = id,
            title = videoTitle,
            duration = duration
        )
    }

    private fun trackPlay() {
        val id = videoId ?: return
        val duration = videoDuration ?: totalDuration.toFloat().takeIf { it > 0 }

        DMBIAnalytics.trackVideoPlay(
            videoId = id,
            title = videoTitle,
            duration = duration,
            position = currentPosition.toFloat()
        )
    }

    private fun trackPause() {
        val id = videoId ?: return
        val percent = calculatePercent()

        DMBIAnalytics.trackVideoPause(
            videoId = id,
            position = currentPosition.toFloat(),
            percent = percent
        )
    }

    private fun trackComplete() {
        val id = videoId ?: return
        val duration = videoDuration ?: totalDuration.toFloat().takeIf { it > 0 }

        DMBIAnalytics.trackVideoComplete(
            videoId = id,
            duration = duration
        )
    }

    private fun trackQuartile(percent: Int) {
        val id = videoId ?: return
        val duration = videoDuration ?: totalDuration.toFloat().takeIf { it > 0 }

        DMBIAnalytics.trackVideoProgress(
            videoId = id,
            duration = duration,
            position = currentPosition.toFloat(),
            percent = percent
        )
    }

    private fun calculatePercent(): Int {
        val duration = videoDuration ?: totalDuration.toFloat()
        if (duration <= 0) return 0
        return ((currentPosition.toFloat() / duration) * 100).toInt()
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
}
