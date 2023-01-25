package com.esn.platform.players

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.view.Surface
import androidx.collection.LruCache
import com.esn.platform.stdlib.CloseableLruCache
import com.esn.platform.xcore.CloseablePool
import com.esn.platform.xcore.createPool
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.analytics.DefaultAnalyticsCollector
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.util.Clock
import java.io.Closeable
import java.util.*


/**
 * Reusable Players Provider.
 *
 * @param configFactory factory of configuration
 */
internal class Players private constructor(configFactory: () -> PlayerConfig) : Closeable {

    constructor(
        context: Context,
        dataSourceConfig: DataSourceConfig
    ) : this({
        PlayerConfig(context, dataSourceConfig)
    })

    private val urls = IdentityHashMap<String, List<MediaItem>>()

    private val items = IdentityHashMap<List<MediaItem>, List<MediaSource>>()

    private val config = configFactory()

    private val pool = CloseablePool(
        poolFactory = { createPool(config.poolSize, config.poolSync) },
        creator = {
            PoolPlayer(
                config.context,
                config.renderers,
                config.sources,
                config.load,
                config.tracks,
                config.bandwidth,
                items
            ).cached()
        },
        destroyer = Player::release
    )

    private val cache = CloseableLruCache<List<MediaItem>, Player>(
        size = config.poolSize,
        creator = { pool.acquire().apply {
            setMediaItems(it, true)
        } },
        destroyer = {
            it.removeMediaItems(0, it.mediaItemCount)
            pool.release(it)
        }
    )

    fun provide(surface: Surface, url: String) =
        cache.player(urls.computeIfAbsent(url) { getMediaItems(url) }!!, surface)

    private fun getMediaItems(url: String): List<MediaItem> {
        return url.fromPlaylist().map { MediaItem.fromUri(it) }
    }

    override fun close() {
        cache.close()
        pool.close()
        items.clear()
        urls.clear()
    }
}

/**
 * ExoPlayer modification for efficient reuse.
 *
 * - Implements hot-swap media sources on started instance
 * - Caching media sources by media items
 *
 * @param player base exoPlayer instance
 * @param sources media sources factory
 * @param items media items/sources cache
 */
private class PoolPlayer private constructor(
    player: Player,
    private val sources: MediaSource.Factory,
    private val items: IdentityHashMap<List<MediaItem>, List<MediaSource>>
) : ForwardingPlayer(player) {

    /**
     * Constructs a new ReusablePlayer.
     *
     * @param context android context
     * @param renderers renderers factory
     * @param sources media sources factory
     * @param load load control
     * @param items media items cache
     */
    constructor(
        context: Context,
        renderers: RenderersFactory,
        sources: MediaSource.Factory,
        load: LoadControl,
        tracks: TrackSelector,
        bandwidth: BandwidthMeter,
        items: IdentityHashMap<List<MediaItem>, List<MediaSource>>
    ) : this(
        ExoPlayer.Builder(
            context, renderers, sources, tracks, load, bandwidth,
            @SuppressLint("MissingSuperCall")
            object : DefaultAnalyticsCollector(Clock.DEFAULT) {
                override fun addListener(listener: AnalyticsListener) {}
                override fun removeListener(listener: AnalyticsListener) {}
                override fun setPlayer(player: Player, looper: Looper) =
                    super.setPlayer(PlayerStub, looper)
            }
        ).build(),
        sources,
        items
    )

    init {
        wrapped().playWhenReady = false
        wrapped().setForegroundMode(true)
        repeatMode = REPEAT_MODE_ALL
        prepare()
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, position: Boolean) {
        wrapped().setMediaSources(
            items.computeIfAbsent(mediaItems) { items ->
                items.map { sources.createMediaSource(it) }
            }!!
        )
    }

    private fun wrapped() = wrappedPlayer as ExoPlayer
}

/**
 * ExoPlayer modification for caching.
 *
 * Owns the surface for resolve methods proxying
 *
 * @param player base exoPlayer instance
 */
private class CachedPlayer(player: Player) : ForwardingPlayer(player) {

    var surface: Surface? = null

    override fun setVideoSurface(surface: Surface?) {
        super.setVideoSurface(surface)
        this.surface = surface
    }

    override fun clearVideoSurface() {
        this.surface = null
        super.clearVideoSurface()
    }
}

/**
 * @receiver cached player instance
 *
 * @return surfaced player
 */
private fun LruCache<List<MediaItem>, Player>.player(item: List<MediaItem>, surface: Surface) =
    (get(item) ?: error("Player not in cache")).let {
        object : ForwardingPlayer(it) {

            init {
                setVideoSurface(surface)
            }

            override fun release() {
                if (wrappedPlayer != PlayerStub) remove(item)
                wrappedPlayer.clearVideoSurface()
            }

            override fun setPlayWhenReady(playWhenReady: Boolean) {

                if (wrappedPlayer.playWhenReady != playWhenReady)
                    wrappedPlayer.playWhenReady = playWhenReady
            }

            override fun getWrappedPlayer(): Player {
                val player = it as? CachedPlayer
                return if (player?.surface == surface)
                    super.getWrappedPlayer() else PlayerStub
            }
        }
    }

/**
 * @receiver non-cached player instance
 *
 * @return cached player instance
 */
private fun Player.cached(): Player = CachedPlayer(this)

private const val PLAYLIST_SEPARATOR = ";"
/**
 * Extensions for Player and Download data
 */
fun String.fromPlaylist() = split(PLAYLIST_SEPARATOR)

fun List<String>.toPlaylist() = joinToString(PLAYLIST_SEPARATOR) { it }
