package com.nuur.muslim.data.repository

import android.content.Context
import com.nuur.muslim.data.local.AdhkarItem
import com.nuur.muslim.data.local.DataManifest
import com.nuur.muslim.data.local.QuranDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.zip.GZIPInputStream

class AdhkarRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cachedList: List<AdhkarItem>? = null

    suspend fun loadAdhkar(): List<AdhkarItem> = withContext(Dispatchers.IO) {
        cachedList?.let { return@withContext it }
        val manifestBytes = context.assets.open("data_manifest.json").use { it.readBytes() }
        val manifest = json.decodeFromString<DataManifest>(manifestBytes.decodeToString())

        val uncompressed = context.assets.open("adhkar_bundle.bin").use { raw ->
            GZIPInputStream(raw).use { gz -> gz.readBytes() }
        }
        val sha = QuranDatabase.sha256Hex(uncompressed)
        check(sha == manifest.adhkarBundleUncompressedSha256) {
            "Adhkar dataset checksum mismatch!"
        }
        val items = json.decodeFromString<List<AdhkarItem>>(uncompressed.decodeToString())
        cachedList = items
        items
    }
}
