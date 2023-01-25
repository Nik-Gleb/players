package com.esn.platform.players

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.offline.Downloader
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheWriter
import com.google.android.exoplayer2.util.PriorityTaskManager
import com.google.android.exoplayer2.util.RunnableFutureTask
import com.google.android.exoplayer2.util.Util
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

/**
 * ProgressiveDownloader with ability to set custom DataSpec
 */
internal class PartialProgressiveDownloader(
    length: Long,
    mediaItem: MediaItem,
    cacheDataSourceFactory: CacheDataSource.Factory,
    private val executor: Executor = Executor { it.run() }
) : Downloader {

    private val dataSpec: DataSpec
    private val dataSource: CacheDataSource
    private val cacheWriter: CacheWriter
    private var priorityTaskManager: PriorityTaskManager? = null

    private var progressListener: Downloader.ProgressListener? = null

    @Volatile
    private var downloadRunnable: RunnableFutureTask<Unit, IOException>? = null

    @Volatile
    private var isCanceled = false

    init {
        val localConfiguration = mediaItem.localConfiguration
            ?: throw NullPointerException("Local Configuration of MediaItem can't be null")

        dataSpec = DataSpec.Builder()
            .setUri(localConfiguration.uri)
            .setLength(length)
            .setKey(localConfiguration.customCacheKey)
            .setFlags(DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION)
            .build()

        dataSource = cacheDataSourceFactory.createDataSourceForDownloading()
        cacheWriter = CacheWriter(dataSource, dataSpec, null
            ) { requestLength, bytesCached, newBytesCached ->
            onProgress(requestLength, bytesCached, newBytesCached)
        }
        priorityTaskManager = cacheDataSourceFactory.upstreamPriorityTaskManager
    }

    override fun download(progressListener: Downloader.ProgressListener?) {
        this.progressListener = progressListener

        downloadRunnable = object : RunnableFutureTask<Unit, IOException>() {
            override fun doWork() {
                cacheWriter.cache()
            }

            override fun cancelWork() {
                cacheWriter.cancel()
            }
        }

        priorityTaskManager?.add(C.PRIORITY_DOWNLOAD)

        try {
            var finished = false

            while (!finished && !isCanceled) {
                priorityTaskManager?.proceed(C.PRIORITY_DOWNLOAD);
                executor.execute(downloadRunnable);
                try {
                    downloadRunnable?.get()
                    finished = true
                } catch (e: ExecutionException) {
                    when (val cause = e.cause ?: Throwable("Original cause is null")) {
                        is PriorityTaskManager.PriorityTooLowException -> {
                            // The next loop iteration will block until the task is able to proceed.
                        }
                        is IOException -> throw cause
                        else -> Util.sneakyThrow(cause);
                    }
                }
            }
        } finally {
            // If the main download thread was interrupted as part of cancelation, then it's possible that
            // the runnable is still doing work. We need to wait until it's finished before returning.
            downloadRunnable?.blockUntilFinished();
            priorityTaskManager?.remove(C.PRIORITY_DOWNLOAD);
        }
    }

    override fun cancel() {
        isCanceled = true
        downloadRunnable?.cancel(true)
    }


    override fun remove() {
        dataSource.cache.removeResource(
            dataSource.cacheKeyFactory.buildCacheKey(dataSpec)
        )
    }

    private fun onProgress(contentLength: Long, bytesCached: Long, newBytesCached: Long) {
        if (progressListener == null) return

        val percentDownloaded =
            if (contentLength == C.LENGTH_UNSET.toLong() || contentLength == 0L) {
                C.PERCENTAGE_UNSET.toFloat()
            } else {
                bytesCached * 100f / contentLength
            }

        progressListener?.onProgress(contentLength, bytesCached, percentDownloaded)
    }
}
