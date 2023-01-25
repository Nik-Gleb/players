package com.esn.platform.players

import android.content.Context
import android.util.AttributeSet
import com.google.android.exoplayer2.Player

class SeekPlayerView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    attrs: Int = 0
) : PlayerView(context, attr, attrs) {

    private val positionListener = object : Player.Listener {

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            seekCallback?.onSeek()
        }
    }

    var hasPlayerCallback: HasPlayerCallback? = null
    var seekCallback: SeekCallback? = null

    override var player: Player?
        get() = super.player
        set(value) {
            super.player = value
            if (value != null) {
                value.removeListener(positionListener)
                value.addListener(positionListener)
            }
            if (player != null) {
                hasPlayerCallback?.onPlayerAdded()
            }
        }

    fun setVolume(volume: Float) {
        player?.volume = volume
    }

    fun play() {
        isActivated = true
    }

    fun pause() {
        isActivated = false
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs) ?: error("player is null")
    }

    fun getCurrentPosition(): Long {
        return player?.currentPosition ?: 0L
    }

    fun getDuration(): Long {
        return player?.duration ?: 0L
    }

    interface HasPlayerCallback {
        fun onPlayerAdded()
    }

    interface SeekCallback {
        fun onSeek()
    }
}
