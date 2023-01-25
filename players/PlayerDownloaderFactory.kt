package com.esn.platform.players

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.offline.Downloader
import com.google.android.exoplayer2.offline.DownloaderFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.util.Util
import java.util.concurrent.Executor

internal class PlayerDownloaderFactory(
    private val cacheDataSourceFactory: CacheDataSource.Factory,
    private val executor: Executor,
    private val length: Long
) : DownloaderFactory {

    override fun createDownloader(request: DownloadRequest): Downloader {
        val contentType = Util.inferContentTypeForUriAndMimeType(request.uri, request.mimeType)

        if (contentType != C.TYPE_OTHER) {
            throw IllegalArgumentException("Support only for progressive downloads")
        }

        return PartialProgressiveDownloader(
            length,
            MediaItem.Builder()
                .setUri(request.uri)
                .setCustomCacheKey(request.customCacheKey)
                .build(),
            cacheDataSourceFactory,
            executor
        )
    }
}
