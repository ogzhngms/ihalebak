package com.ogzhngms.ihalebak

import android.content.Context
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

// The collector publishes its files on GitHub Pages; see collector/build.py for their shape.
const val DATA_URL = "https://ogzhngms.github.io/ihalebak/data"

// Reads the published data, keeping the last copy of each file so the app still opens offline.
class Repository(context: Context, private val baseUrl: String = DATA_URL) {
    private val cacheDir = File(context.cacheDir, "data").apply { mkdirs() }

    suspend fun index(refresh: Boolean): Loaded<Index> = load("index.json", refresh) { Index.parse(it) }

    suspend fun province(slug: String, refresh: Boolean): Loaded<List<Tender>> =
        load("il/$slug.json", refresh) { Tender.parseList(it.getJSONArray("tenders")) }

    private suspend fun <T> load(path: String, refresh: Boolean, read: (JSONObject) -> T): Loaded<T> = withContext(Dispatchers.IO) {
        val cached = File(cacheDir, path.replace('/', '_'))
        if (!refresh && cached.exists()) {
            return@withContext Loaded(read(JSONObject(cached.readText())), fromCache = true)
        }
        try {
            val text = download("$baseUrl/$path")
            val value = read(JSONObject(text))
            cached.writeText(text)
            Loaded(value, fromCache = false)
        } catch (e: IOException) {
            if (cached.exists()) Loaded(read(JSONObject(cached.readText())), fromCache = true) else throw e
        }
    }

    private fun download(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        // Pages caches for ten minutes; ask for a fresh copy so a pull-to-refresh means it.
        connection.setRequestProperty("Cache-Control", "no-cache")
        try {
            if (connection.responseCode != 200) throw IOException("HTTP ${connection.responseCode} for $url")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

data class Loaded<T>(val value: T, val fromCache: Boolean)
