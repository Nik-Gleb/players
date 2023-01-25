package com.esn.platform.players

import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters
import com.google.android.exoplayer2.video.VideoSize

/**
 * Player Stub.
 *
 * This instance doing nothing.
 * This implementation is useful for some default-cases (edit mode in views, tests, empty states)
 */
@Suppress("CAST_NEVER_SUCCEEDS")
object PlayerStub : Player {
    override fun addListener(listener: Player.Listener) {}
    override fun removeListener(listener: Player.Listener) {}
    override fun setMediaItems(mediaItems: MutableList<MediaItem>) {}
    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {}
    override fun setMediaItems(mediaItems: MutableList<MediaItem>, index: Int, position: Long) {}
    override fun setMediaItem(mediaItem: MediaItem) {}
    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {}
    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {}
    override fun addMediaItem(mediaItem: MediaItem) {}
    override fun addMediaItem(index: Int, mediaItem: MediaItem) {}
    override fun addMediaItems(mediaItems: MutableList<MediaItem>) {}
    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {}
    override fun moveMediaItem(currentIndex: Int, newIndex: Int) {}
    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {}
    override fun removeMediaItem(index: Int) {}
    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {}
    override fun clearMediaItems() {}
    override fun isCommandAvailable(command: Int): Boolean = false
    override fun canAdvertiseSession(): Boolean = false
    override fun getAvailableCommands(): Player.Commands = Player.Commands.EMPTY
    override fun prepare() {}
    override fun getPlaybackState(): Int = Player.STATE_IDLE
    override fun getPlaybackSuppressionReason(): Int = Player.PLAYBACK_SUPPRESSION_REASON_NONE
    override fun isPlaying(): Boolean = false
    override fun getPlayerError(): PlaybackException? = null
    override fun play() {}
    override fun pause() {}
    override fun setPlayWhenReady(playWhenReady: Boolean) {}
    override fun getPlayWhenReady(): Boolean = false
    override fun setRepeatMode(repeatMode: Int) {}
    override fun getRepeatMode(): Int = Player.REPEAT_MODE_ALL
    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {}
    override fun getShuffleModeEnabled(): Boolean = false
    override fun isLoading(): Boolean = false
    override fun seekToDefaultPosition() {}
    override fun seekToDefaultPosition(mediaItemIndex: Int) {}
    override fun seekTo(positionMs: Long) {}
    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {}
    override fun getSeekBackIncrement(): Long = 0L
    override fun seekBack() {}
    override fun getSeekForwardIncrement(): Long = 0L
    override fun seekForward() {}
    override fun hasPrevious(): Boolean = false
    override fun hasPreviousWindow(): Boolean = false
    override fun hasPreviousMediaItem(): Boolean = false
    override fun previous() {}
    override fun seekToPreviousWindow() {}
    override fun seekToPreviousMediaItem() {}
    override fun getMaxSeekToPreviousPosition(): Long = 0L
    override fun seekToPrevious() {}
    override fun hasNext(): Boolean = false
    override fun hasNextWindow(): Boolean = false
    override fun hasNextMediaItem(): Boolean = false
    override fun next() {}
    override fun seekToNextWindow() {}
    override fun seekToNextMediaItem() {}
    override fun seekToNext() {}
    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {}
    override fun setPlaybackSpeed(speed: Float) {}
    override fun stop() {}
    override fun stop(reset: Boolean) {}
    override fun release() {}
    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {}
    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {}
    override fun getCurrentPeriodIndex(): Int = 0
    override fun getCurrentWindowIndex(): Int = 0
    override fun getCurrentMediaItemIndex(): Int = 0
    override fun getNextWindowIndex(): Int = 0
    override fun getNextMediaItemIndex(): Int = 0
    override fun getPreviousWindowIndex(): Int = 0
    override fun getPreviousMediaItemIndex(): Int = 0
    override fun getCurrentMediaItem(): MediaItem? = null
    override fun getMediaItemCount(): Int = 0
    override fun getDuration(): Long = 0L
    override fun getCurrentPosition(): Long = 0L
    override fun getBufferedPosition(): Long = 0
    override fun getBufferedPercentage(): Int = 0
    override fun getTotalBufferedDuration(): Long = 0L
    override fun isCurrentWindowDynamic(): Boolean = false
    override fun isCurrentMediaItemDynamic(): Boolean = false
    override fun isCurrentWindowLive(): Boolean = false
    override fun isCurrentMediaItemLive(): Boolean = false
    override fun getCurrentLiveOffset(): Long = 0L
    override fun isCurrentWindowSeekable(): Boolean = false
    override fun isCurrentMediaItemSeekable(): Boolean = false
    override fun isPlayingAd(): Boolean = false
    override fun getCurrentAdGroupIndex(): Int = 0
    override fun getCurrentAdIndexInAdGroup(): Int = 0
    override fun getContentDuration(): Long = 0L
    override fun getContentPosition(): Long = 0L
    override fun getContentBufferedPosition(): Long = 0L
    override fun setVolume(volume: Float) {}
    override fun getVolume(): Float = 0f
    override fun clearVideoSurface() {}
    override fun clearVideoSurface(surface: Surface?) {}
    override fun setVideoSurface(surface: Surface?) {}
    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {}
    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {}
    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {}
    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {}
    override fun setVideoTextureView(textureView: TextureView?) {}
    override fun clearVideoTextureView(textureView: TextureView?) {}
    override fun getDeviceVolume(): Int = 0
    override fun isDeviceMuted(): Boolean = false
    override fun setDeviceVolume(volume: Int) {}
    override fun increaseDeviceVolume() {}
    override fun decreaseDeviceVolume() {}
    override fun setDeviceMuted(muted: Boolean) {}
    override fun getApplicationLooper(): Looper = null as Looper
    override fun getPlaybackParameters(): PlaybackParameters = null as PlaybackParameters
    override fun getCurrentTrackGroups(): TrackGroupArray = null as TrackGroupArray
    override fun getCurrentTrackSelections(): TrackSelectionArray = null as TrackSelectionArray
    override fun getCurrentTracksInfo(): TracksInfo = null as TracksInfo
    override fun getTrackSelectionParameters(): TrackSelectionParameters =
        null as TrackSelectionParameters

    override fun getMediaMetadata(): MediaMetadata = null as MediaMetadata
    override fun getPlaylistMetadata(): MediaMetadata = null as MediaMetadata
    override fun getCurrentManifest(): Any? = null
    override fun getCurrentTimeline(): Timeline = Timeline.EMPTY
    override fun getMediaItemAt(index: Int): MediaItem = null as MediaItem
    override fun getAudioAttributes(): AudioAttributes = null as AudioAttributes
    override fun getVideoSize(): VideoSize = null as VideoSize
    override fun getCurrentCues(): MutableList<Cue> = null as MutableList<Cue>
    override fun getDeviceInfo(): DeviceInfo = null as DeviceInfo
}
