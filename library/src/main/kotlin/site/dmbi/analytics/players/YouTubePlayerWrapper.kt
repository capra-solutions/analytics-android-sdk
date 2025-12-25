package site.dmbi.analytics.players

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import site.dmbi.analytics.DMBIAnalytics

/**
 * Wrapper for YouTube Android Player that automatically tracks video analytics events.
 *
 * Usage:
 * ```kotlin
 * youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
 *     override fun onReady(youTubePlayer: YouTubePlayer) {
 *         val wrapper = YouTubePlayerWrapper(youTubePlayer)
 *         wrapper.attach(
 *             videoId = "dQw4w9WgXcQ",
 *             title = "Video Title"
 *         )
 *         youTubePlayer.loadVideo("dQw4w9WgXcQ", 0f)
 *     }
 * })
 * ```
 */
class YouTubePlayerWrapper(private val player: YouTubePlayer) {

    private var videoId: String? = null
    private var videoTitle: String? = null
    private var videoDuration: Float = 0f
    private var lastReportedQuartile: Int = 0
    private var hasTrackedImpression: Boolean = false
    private var currentPosition: Float = 0f

    private val playerListener = object : AbstractYouTubePlayerListener() {

        override fun onReady(youTubePlayer: YouTubePlayer) {
            if (!hasTrackedImpression) {
                trackImpression()
                hasTrackedImpression = true
            }
        }

        override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
            when (state) {
                PlayerConstants.PlayerState.PLAYING -> trackPlay()
                PlayerConstants.PlayerState.PAUSED -> trackPause()
                PlayerConstants.PlayerState.ENDED -> trackComplete()
                else -> {}
            }
        }

        override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
            videoDuration = duration
        }

        override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
            currentPosition = second
            checkQuartileProgress()
        }
    }

    /**
     * Attach analytics tracking to the YouTube player.
     *
     * @param videoId YouTube video ID
     * @param title Optional video title
     */
    fun attach(videoId: String, title: String? = null) {
        this.videoId = videoId
        this.videoTitle = title
        this.lastReportedQuartile = 0
        this.hasTrackedImpression = false

        player.addListener(playerListener)
    }

    /**
     * Detach analytics tracking from the player.
     */
    fun detach() {
        player.removeListener(playerListener)
        videoId = null
        videoTitle = null
    }

    private fun trackImpression() {
        val id = videoId ?: return

        DMBIAnalytics.trackVideoImpression(
            videoId = id,
            title = videoTitle,
            duration = if (videoDuration > 0) videoDuration else null
        )
    }

    private fun trackPlay() {
        val id = videoId ?: return

        DMBIAnalytics.trackVideoPlay(
            videoId = id,
            title = videoTitle,
            duration = if (videoDuration > 0) videoDuration else null,
            position = currentPosition
        )
    }

    private fun trackPause() {
        val id = videoId ?: return
        val percent = calculatePercent()

        DMBIAnalytics.trackVideoPause(
            videoId = id,
            position = currentPosition,
            percent = percent
        )
    }

    private fun trackComplete() {
        val id = videoId ?: return

        DMBIAnalytics.trackVideoComplete(
            videoId = id,
            duration = if (videoDuration > 0) videoDuration else null
        )
    }

    private fun trackQuartile(percent: Int) {
        val id = videoId ?: return

        DMBIAnalytics.trackVideoProgress(
            videoId = id,
            duration = if (videoDuration > 0) videoDuration else null,
            position = currentPosition,
            percent = percent
        )
    }

    private fun calculatePercent(): Int {
        if (videoDuration <= 0) return 0
        return ((currentPosition / videoDuration) * 100).toInt()
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
