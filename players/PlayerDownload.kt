package com.esn.platform.players

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.esn.platform.stdlib.SchedulersX
import com.google.android.exoplayer2.offline.DefaultDownloadIndex
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.ContentMetadata
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class PlayerDownload(
    context: Context,
    private val dataSourceConfig: DataSourceConfig
) {

    private val downloadManager: DownloadManager
    private val downloadUri = mutableMapOf<String, Set<Uri>>()
    private val downloadListeners = mutableMapOf<String, DownloadManager.Listener>()

    init {
        downloadManager = with(dataSourceConfig) {
            DownloadManager(
                context,
                DefaultDownloadIndex(dbProvider),
                PlayerDownloaderFactory(
                    dataSourceConfig.cacheDataSourceFactory,
                    SchedulersX.IO,
                    dataSourceConfig.cacheLength
                ))
        }

        downloadManager.maxParallelDownloads = Integer.MAX_VALUE
    }

    suspend fun download(playlist: String) =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                downloadListeners[playlist]?.let { downloadManager.removeListener(it) }
                clearDownloads(playlist)
            }

            if (downloadUri.containsKey(playlist)) {
                clearDownloads(playlist)
                continuation.resume(Unit)
                return@suspendCancellableCoroutine
            }

            val playlistUri = getPlaylistUri(playlist).also { downloadUri[playlist] = it }
            val playlistDownload = getPlaylistDownload(playlistUri)

            fun onItemMoved(): Boolean {
                if (playlistDownload.isNotEmpty()) return false
                downloadListeners[playlist]?.let { downloadManager.removeListener(it) }
                continuation.resume(Unit)
                return true
            }

            if (onItemMoved()) return@suspendCancellableCoroutine

            downloadListeners[playlist] = DownloadListener(
                listOf(
                    Download.STATE_COMPLETED, Download.STATE_FAILED, Download.STATE_REMOVING
                )) { downloadUri ->
                playlistDownload.removeIf { playlistUri -> playlistUri == downloadUri }
                onItemMoved()
            }.also { downloadManager.addListener(it) }

            playlistDownload.forEach { downloadRequest(it) }
            downloadManager.resumeDownloads()
        }

    private fun getPlaylistUri(playlist: String): Set<Uri> =
        playlist.fromPlaylist().map { it.toUri() }.toSet()

    private fun getPlaylistDownload(playlistUri: Collection<Uri>) =
        playlistUri
            .filter { !dataSourceConfig.cache.isAvailable(it) }
            .filter { uri -> !downloadManager.currentDownloads.any { uri == it.request.uri } }
            .toMutableList()

    private fun clearDownloads(playlist: String) {
        if (playlist.isEmpty()) {
            removeAllDownloads()
            return
        }

        downloadUri.remove(playlist)?.let {
            it.removeIntersections(downloadUri.values.flatten().toSet())
            it.removeNotCompletedDownloads()
        }
    }

    private fun removeAllDownloads() {
        downloadManager.currentDownloads.forEach {
            downloadManager.removeDownload(it.request.id)
        }
    }

    private fun Set<Uri>.removeIntersections(source: Set<Uri>) {
        val intersections = intersect(source)
        if (intersections.isNotEmpty()) toMutableSet().removeAll(intersections)
    }

    private fun Set<Uri>.removeNotCompletedDownloads() {
        downloadManager.currentDownloads
            .asSequence()
            .filter { contains(it.request.uri) }
            .filter { it.state != Download.STATE_COMPLETED }
            .forEach { downloadManager.removeDownload(it.request.id) }
    }

    private fun downloadRequest(requestUri: Uri) {
        val key = requestUri.getCacheKey()
        downloadManager.addDownload(
            DownloadRequest.Builder(key, requestUri)
                .setCustomCacheKey(key)
                .build()
        )
    }

    private class DownloadListener(
        private val checkList: List<@Download.State Int>,
        private val onChecked: (Uri) -> Unit
    ) : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: java.lang.Exception?
        ) {
            if (checkList.contains(download.state)) {
                onChecked(download.request.uri)
            }
        }
    }
}

/**
 * Check for cache availability
 */
private fun Cache.isAvailable(uri: Uri) = keys.contains(uri.getCacheKey())

/**
 * Check for cache completeness
 */
// todo check isCached(String key, long position, long length) method
private fun Cache.isFullyCached(uri: Uri): Boolean {
    val key = uri.getCacheKey()

    val contentLength = ContentMetadata.getContentLength(getContentMetadata(key))

    if (contentLength < 0) {
        return false
    }

    return contentLength == getCachedBytes(key, 0L, contentLength)
}
