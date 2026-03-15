package com.synex.mirarecorder.ui.recordings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.view.WindowManager
import android.widget.FrameLayout

class VideoPlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null

    companion object {
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_TITLE = "title"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: run {
            finish()
            return
        }

        player = ExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }

        val playerView = PlayerView(this).apply {
            this.player = this@VideoPlayerActivity.player
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }

        setContentView(playerView)
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}
