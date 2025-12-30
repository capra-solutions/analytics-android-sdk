package solutions.capra.analytics.players

import com.dailymotion.player.android.sdk.PlayerView
import com.dailymotion.player.android.sdk.listeners.VideoListener
import com.dailymotion.player.android.sdk.listeners.PlayerListener
import solutions.capra.analytics.CapraAnalytics

/**
 * Wrapper for Dailymotion Android Player that automatically tracks video analytics events.
 *
 * Tracks: video impression, play, pause, and complete events.
 * Note: Quartile progress tracking is not supported due to SDK limitations.
 *
 * Usage:
 * ```kotlin
 * // Create player with wrapper listeners
 * Dailymotion.createPlayer(
 *     context = context,
 *     playerId = "YOUR_PLAYER_ID",
 *     videoListener = wrapper.videoListener,
 *     playerListener = wrapper.playerListener
 * ) { player ->
 *     playerView.setPlayer(player)
 *     wrapper.attach(videoId = "x8abc123", title = "Video Title")
 *     player.loadContent(videoId = "x8abc123")
 * }
 * ```
 */
class DailymotionPlayerWrapper {

    private var playerView: PlayerView? = null
    private var videoId: String? = null
    private var videoTitle: String? = null
    private var videoDuration: Float? = null
    private var hasTrackedImpression: Boolean = false
    private var isPlaying: Boolean = false

    /**
     * VideoListener to be passed to Dailymotion.createPlayer()
     */
    val videoListener = object : VideoListener {
        override fun onVideoStart(playerView: PlayerView) {
            // Check for video change (including auto-play)
            checkForVideoChange(playerView)

            if (!hasTrackedImpression) {
                trackImpression()
                hasTrackedImpression = true
            }
        }

        override fun onVideoEnd(playerView: PlayerView) {
            trackComplete()
            isPlaying = false
        }

        override fun onVideoPlay(playerView: PlayerView) {
            if (!isPlaying) {
                trackPlay()
                isPlaying = true
            }
        }

        override fun onVideoPause(playerView: PlayerView) {
            if (isPlaying) {
                trackPause()
                isPlaying = false
            }
        }
    }

    /**
     * PlayerListener to be passed to Dailymotion.createPlayer()
     */
    val playerListener = object : PlayerListener {
        override fun onPlayerEnd(playerView: PlayerView) {
            // Player ended
        }
    }

    /**
     * Attach analytics tracking with video metadata.
     *
     * @param videoId Dailymotion video ID
     * @param title Optional video title
     * @param duration Optional video duration in seconds
     */
    fun attach(videoId: String, title: String? = null, duration: Float? = null) {
        this.videoId = videoId
        this.videoTitle = title
        this.videoDuration = duration
        this.hasTrackedImpression = false
        this.isPlaying = false
    }

    /**
     * Detach analytics tracking.
     */
    fun detach() {
        videoId = null
        videoTitle = null
    }

    /**
     * Check if video has changed (e.g., auto-play next video).
     * Uses getState() to get current video ID from player.
     */
    private fun checkForVideoChange(playerView: PlayerView) {
        this.playerView = playerView
        playerView.getState(object : PlayerView.PlayerStateCallback {
            override fun onStateReceived(state: PlayerView.PlayerState) {
                val newVideoId = state.videoId
                if (!newVideoId.isNullOrEmpty() && newVideoId != videoId) {
                    handleVideoChange(newVideoId, state.videoTitle)
                }
            }
        })
    }

    /**
     * Handle video change (e.g., auto-play next video).
     * Resets tracking state and updates video ID.
     */
    private fun handleVideoChange(newVideoId: String, newTitle: String?) {
        videoId = newVideoId
        videoTitle = newTitle
        videoDuration = null
        hasTrackedImpression = false
        isPlaying = false
    }

    private fun trackImpression() {
        val id = videoId ?: return

        CapraAnalytics.trackVideoImpression(
            videoId = id,
            title = videoTitle,
            duration = videoDuration
        )
    }

    private fun trackPlay() {
        val id = videoId ?: return

        CapraAnalytics.trackVideoPlay(
            videoId = id,
            title = videoTitle,
            duration = videoDuration,
            position = 0f
        )
    }

    private fun trackPause() {
        val id = videoId ?: return

        CapraAnalytics.trackVideoPause(
            videoId = id,
            position = 0f,
            percent = 0
        )
    }

    private fun trackComplete() {
        val id = videoId ?: return

        CapraAnalytics.trackVideoComplete(
            videoId = id,
            duration = videoDuration
        )
    }
}
