package com.isokovibe.musicplayer.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Fetches the site's app config.
 *
 * Uses plain [HttpURLConnection] rather than adding an HTTP client
 * dependency: this is one GET of a small JSON document, and OkHttp/Retrofit
 * would be a large addition to an app that otherwise touches the network
 * nowhere at all.
 *
 * Failures are returned rather than thrown. A phone with no signal, a site
 * that's down, or a plugin that isn't installed yet are all completely
 * normal states here, and none of them should be able to disturb playback —
 * callers fall back to the last cached config.
 */
class RemoteConfigRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun fetch(url: String): Result<RemoteConfig> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    error("HTTP ${connection.responseCode}")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString<RemoteConfig>(body)
            } finally {
                connection.disconnect()
            }
        }
    }
}

private const val TIMEOUT_MS = 10_000
