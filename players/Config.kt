package com.esn.platform.players

import android.content.ComponentCallbacks2
import android.content.Context
import android.net.Uri
import com.esn.platform.stdlib.http
import com.esn.platform.stdlib.intercept
import com.esn.platform.xcore.application
import com.esn.platform.xcore.onTrimMemory
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.*
import okhttp3.CacheControl
import okhttp3.Interceptor
import java.io.Closeable
import java.io.File

/**
 * ExoPlayer Configuration.
 *
 * Defines pre-configured exoplayer dependencies.
 *
 * @param context android context
 * @param dataSourceConfig configuration of data source
 */
internal class PlayerConfig constructor(val context: Context, dataSourceConfig: DataSourceConfig) {

    // val poolSize = 3 //1p0e
    //val poolSize = 5 // 1p1e
    val poolSize = 7
    //val poolSize = 9 // 2p1e
    // val poolSize = 11 // 2p1e for Profile Recycle

    val poolSync = false

    val sources: MediaSource.Factory = DefaultMediaSourceFactory(dataSourceConfig.cacheDataSourceFactory)

    val load: LoadControl = DefaultLoadControl.Builder()
        .setPrioritizeTimeOverSizeThresholds(false)
        .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBufferDurationsMs(
            DefaultLoadControl.DEFAULT_MIN_BUFFER_MS / 10,
            DefaultLoadControl.DEFAULT_MAX_BUFFER_MS / 10,
            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS / 100,
            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS / 50
        ).build()

    val renderers: RenderersFactory = DefaultRenderersFactory(context)

    val tracks = DefaultTrackSelector(context)

    val bandwidth = DefaultBandwidthMeter.getSingletonInstance(context)

    init {
        MediaCodecUtil.warmDecoderInfoCache("video/avc", false, false)
    }
}

internal class DataSourceConfig(context: Context) : Closeable {

    val cacheLength = 100 * 1024L

    private val cacheDir = File(context.cacheDir, "/video")

    private val httpCacheControl = CacheControl.Builder()
        .noCache()
        .noStore()
        .build()

    private val httpCacheInterceptor = Interceptor { chain ->
        val cacheRequest = chain.request()
            .newBuilder()
            .cacheControl(httpCacheControl)
            .build()

        chain.proceed(cacheRequest)
    }

    val dbProvider: DatabaseProvider = StandaloneDatabaseProvider(context)

    val cache: Cache = SimpleCache(
        cacheDir,
        // todo ExoPlayer team advise to use NoOp evictor, but they didn't answer why
        //LeastRecentlyUsedCacheEvictor(128 * 1024 * 1024),
        NoOpCacheEvictor(),
        dbProvider, null, false, false
    )

    private val upstreamFactory = DefaultDataSource.Factory(
        context,
        OkHttpDataSource.Factory(http.intercept { httpCacheInterceptor })
    )

    val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setCacheKeyFactory(PlayerCacheKeyFactory())
        .setCacheReadDataSourceFactory(FileDataSource.Factory())
        .setCacheWriteDataSinkFactory(
            CacheDataSink.Factory()
                .setCache(cache)
                .setFragmentSize(128 * 1024)
                .setBufferSize(CacheDataSink.DEFAULT_BUFFER_SIZE)
        )
        .setUpstreamDataSourceFactory(
            upstreamFactory
        )
        .setUpstreamPriority(C.PRIORITY_PLAYBACK)
        .setUpstreamPriorityTaskManager(null)
        .setFlags(0)

    init {
        context.application.onTrimMemory { level ->
            if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                close()
            }
        }
    }

    override fun close() {
        cache.keys.forEach { cache.removeResource(it) }
    }
}

/**
 * Receive cache key for media item
 */
private class PlayerCacheKeyFactory : CacheKeyFactory {

    override fun buildCacheKey(dataSpec: DataSpec): String {
        return dataSpec.uri.getCacheKey()
    }
}

/**
 * Receive cache key for media item
 */
internal fun Uri.getCacheKey() = lastPathSegment ?: path ?: ""
