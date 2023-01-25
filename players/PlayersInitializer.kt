package com.esn.platform.players

import android.content.Context
import android.view.Surface
import androidx.annotation.Keep
import androidx.startup.Initializer


private lateinit var playerDownload: PlayerDownload
private lateinit var players: Players

/**
 * Obtain PlayersProvider by context
 *
 * @param surface output surface
 * @param url media resource. Should always be unique, or provider will return last player that have been playing (he have file in cache)
 * requested resource, and you will have no idea why this happens, because you 100% changed the file
 *
 * @return the player
 */
fun player(surface: Surface, url: String) = players.provide(surface, url)

/**
 * Send suspend cache request for video playlist
 *
 * @param playlist playlist urls
 */
suspend fun cachePlayer(playlist: String) = playerDownload.download(playlist)

@Keep
@Suppress("unused")
class PlayersInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val dataSourceConfig = DataSourceConfig(context)
        Players(context, dataSourceConfig).also { players = it }
        PlayerDownload(context, dataSourceConfig).also { playerDownload =  it }
    }
    override fun dependencies() = emptyList<Class<Initializer<*>>>()
}
