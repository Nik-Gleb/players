package com.esn.platform.players

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import com.esn.platform.xcore.createCoroutineScope
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.Closeable
import kotlin.math.min

/**
 * PlayerView.
 *
 * UI Widget for display videos via Players.
 *
 *  - Manages the playback lifecycle.
 *  - Coordinates the work of the player with video surface.
 *  - Provides correct rendering with keep aspect ratio.
 *  - Supports view transformations such as scale, translation, rotation.
 *  - Compatible and optimized for animations and RecyclerView.
 *  - Provides only 2 public scenario: play/pause, set data.
 *
 *  Motivation is a making Video Component for use such easy as possible.
 *  So that the developer could work with the video without hesitation as with many other simple
 *  Android Widgets: ImageView, TextView, etc.
 *
 *  Without limiting yourself and without thinking about any video nuances
 *
 *  @param context layout inflater context
 *  @param attr xml-extracted attributes
 *  @param attrs attribute theme based id
 */
// todo should not be open, change this when we'll manage with SeekPlayerView
open class PlayerView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    attrs: Int = 0
) : TextureView(context, attr, attrs), Closeable {

    private var scope: CoroutineScope? = null
    private var playlistPrefetchJob: Job? = null

    private var surface: Surface? = null
        set(value) {
            if (field != value) {
                field = value
                invalidatePlayer()
            }
        }

    private var data: String? = null
        set(value) {
            if (field != value) {
                field = null
                invalidatePlayer()
                if (value != null) {
                    field = value
                    invalidatePlayer()
                }
            }
        }

    // todo should be private, change visibility when we'll manage with SeekPlayerView
    protected open var player: Player? = null
        set(value) {
            if (field != value) {
                //field?.removeListener(events)
                field?.release()
                field = value
                //field?.addListener(events)
                invalidateActivated()
            }
        }

    private var visible = false
        set(value) {
            if (field != value) {
                field = value
                invalidateActivated()
            }
        }

    // todo add ability to listen player events
    var listener: Player.Listener? = null
        set(value) {
            if (field != value) {
                player?.let { _player ->
                    field?.let { _listener ->
                        _player.removeListener(_listener)
                    }
                }
                field = value
                invalidateActivated()
            }
        }

    /*private val events = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
            matrix.video(videoSize.width, videoSize.height)
        }
    }*/

    var onPause: (() -> Unit)? = null

    private val gestures = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
                //return super.onDown(e)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val part = width / 3
                if (e.x < part) player?.seekToPreviousMediaItem()
                else if (e.x > part * 2) player?.seekToNextMediaItem()
                else {
                    isActivated = !isActivated
                    onPause?.invoke()
                }
                return false
            }

            /*override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (velocityX > 500) {
                    player?.seekToPreviousMediaItem()
                    return true
                } else if (velocityX < -500) {
                    player?.seekToNextMediaItem()
                    return true
                } else return false;// return super.onFling(e1, e2, velocityX, velocityY)
            }*/
        })

    var onProgress: ((Int, Float) -> Unit)? = null

    var onFirstFrameRendered: (() -> Unit)? = null

    @Suppress("LeakingThis")
    private val matrix = VideoMatrix(
        width,
        height,
        getMatrix(),
        this::setTransform,
        true
    ).apply { video(720, 1280) }

    init {

        surfaceTextureListener = object : SurfaceTextureListener {

            private var index = -1
                set(value) {
                    if (field != value) {
                        field = value
                        invalidateProgress()
                    }
                }

            private var position = -1L
                set(value) {
                    if (field != value) {
                        field = value
                        invalidateProgress()
                    }
                }

            private var duration = 0L
                set(value) {
                    if (field != value) {
                        field = value
                        invalidateProgress()
                    }
                }

            private fun invalidateProgress() {
                if (duration == 0L || position == -1L || index == -1) return
                onProgress?.invoke(index, min(position.toFloat() / duration.toFloat(), 1f))
            }

            override fun onSurfaceTextureAvailable(texture: SurfaceTexture, w: Int, h: Int) {
                @SuppressLint("Recycle")
                surface = Surface(texture)
            }

            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                val last = surface
                surface = null
                last?.release()
                return false
            }

            override fun onSurfaceTextureUpdated(txt: SurfaceTexture) {
                onFirstFrameRendered?.invoke()
                onFirstFrameRendered = null
                player?.let { _player ->
                    index = _player.currentMediaItemIndex
                    position = _player.currentPosition
                    duration = _player.duration
                }
            }

            override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, w: Int, h: Int) = Unit
        }
        keepScreenOn = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent) =
        gestures.onTouchEvent(event) || super.onTouchEvent(event)

    override fun setAnimationMatrix(matrix: Matrix?) {
        super.setAnimationMatrix(matrix)
        this.matrix.post(getMatrix())
    }

    /*override fun setTranslationX(translationX: Float) {
        val update = getTranslationX() != translationX
        super.setTranslationX(translationX)
        if (update) matrix.post(getMatrix())
    }

    override fun setTranslationY(translationY: Float) {
        val update = getTranslationY() != translationY
        super.setTranslationY(translationY)
        if (update) matrix.post(getMatrix())
    }*/

    override fun setRotation(rotation: Float) {
        val update = getRotation() != rotation
        super.setRotation(rotation)
        if (update) matrix.post(getMatrix())
    }

    override fun setRotationX(rotationX: Float) {
        val update = getRotationX() != rotationX
        super.setRotationX(rotationX)
        if (update) matrix.post(getMatrix())
    }

    override fun setRotationY(rotationY: Float) {
        val update = getRotationY() != rotationY
        super.setRotationY(rotationY)
        if (update) matrix.post(getMatrix())
    }

    /*override fun setScaleX(scaleX: Float) {
        val update = getScaleX() != scaleX
        super.setScaleX(scaleX)
        if (update) matrix.post(getMatrix())
    }

    override fun setScaleY(scaleY: Float) {
        val update = getScaleY() != scaleY
        super.setScaleY(scaleY)
        if (update) matrix.post(getMatrix())
    }*/

    override fun setPivotX(pivotX: Float) {
        val update = getPivotX() != pivotX
        super.setPivotX(pivotX)
        if (update) matrix.post(getMatrix())
    }

    override fun setPivotY(pivotY: Float) {
        val update = getPivotY() != pivotY
        super.setPivotY(pivotY)
        if (update) matrix.post(getMatrix())
    }

    override fun resetPivot() {
        super.resetPivot()
        matrix.post(getMatrix())
    }

    override fun onSizeChanged(nw: Int, nh: Int, ow: Int, oh: Int) {
        super.onSizeChanged(nw, nh, ow, oh)
        if (nw != ow || nh != oh) matrix.view(nw, nh)
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        visible = isVisible
    }

    override fun dispatchSetActivated(activated: Boolean) {
        super.dispatchSetActivated(activated)
        invalidateActivated()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scope = createCoroutineScope()
    }

    override fun onDetachedFromWindow() {
        scope?.cancel()
        scope = null
        surfaceTexture?.let { surfaceTextureListener?.onSurfaceTextureDestroyed(it) }
        super.onDetachedFromWindow()
    }

    private fun invalidatePlayer() {
        val surface = this.surface
        val data = this.data
        player = if (surface != null && data != null) player(surface, data) else null
    }

    fun data(url: String? = null) {
        this.data = url
    }

    fun next() = player?.seekToNextMediaItem()

    fun prev() = player?.seekToPreviousMediaItem()

    fun first() = player?.seekTo(0, 0L)

    fun seek(index: Int) = player?.seekTo(index, 0L)

    private fun invalidateActivated() {
        if ((player?.mediaItemCount ?: -1) == 0) invalidatePlayer()

        playlistPrefetchJob?.cancel()

        val playWhenReady = player?.playWhenReady
        val canPlay = visible && isActivated

        player?.playWhenReady = canPlay

        if (playWhenReady == false && canPlay) {
            val playlistDownload = data?.fromPlaylist()?.drop(1) ?: emptyList()

            if (playlistDownload.isEmpty()) return

            playlistPrefetchJob = scope?.launch {
                cachePlayer(playlistDownload.toPlaylist())
            }
        }
    }

    override fun close() {
        surfaceTexture?.let { surfaceTextureListener?.onSurfaceTextureDestroyed(it) }
        surfaceTextureListener = null
        surfaceTexture?.releaseTexImage()
        surfaceTexture?.release()
        data = null
    }
}
