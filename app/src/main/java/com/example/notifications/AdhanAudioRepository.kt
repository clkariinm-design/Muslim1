package com.nuur.muslim.notifications

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads the app-selected Adhan recording into private app storage.
 * There is no user-provided URI or shared-storage access path.
 */
object AdhanAudioRepository {
    private const val ADHAN_URL = "https://cdn.aladhan.com/audio/adhans/a1.mp3"
    private const val MAX_AUDIO_BYTES = 15L * 1024L * 1024L
    private const val MIN_AUDIO_BYTES = 32L * 1024L
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000

    fun cachedAdhanFile(context: Context): File =
        File(File(context.filesDir, "adhan"), "default_adhan_v1.mp3")

    suspend fun ensureDownloaded(context: Context): Boolean = withContext(Dispatchers.IO) {
        val target = cachedAdhanFile(context)
        if (isValidMp3(target)) return@withContext true

        val directory = target.parentFile ?: return@withContext false
        if (!directory.exists() && !directory.mkdirs()) return@withContext false
        val temporary = File(directory, "${target.name}.part")
        temporary.delete()

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(ADHAN_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "audio/mpeg, application/octet-stream")
                setRequestProperty("User-Agent", "NuurQuran-Android")
            }
            connection.connect()
            require(connection.responseCode in 200..299) {
                "Adhan server returned HTTP ${connection.responseCode}"
            }
            require(
                connection.url.protocol == "https" &&
                    connection.url.host.equals("cdn.aladhan.com", ignoreCase = true)
            ) { "Unexpected Adhan download host" }
            require(connection.contentLengthLong <= MAX_AUDIO_BYTES) {
                "Adhan file is too large"
            }

            BufferedInputStream(connection.inputStream).use { input ->
                BufferedOutputStream(temporary.outputStream()).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_AUDIO_BYTES) { "Adhan file is too large" }
                        output.write(buffer, 0, count)
                    }
                    output.flush()
                    require(total >= MIN_AUDIO_BYTES) { "Adhan file is incomplete" }
                }
            }

            require(isValidMp3(temporary)) { "Downloaded file is not a valid MP3" }
            if (target.exists() && !target.delete()) error("Could not replace cached Adhan")
            check(temporary.renameTo(target)) { "Could not finalize cached Adhan" }
            true
        } catch (_: Exception) {
            temporary.delete()
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun isValidMp3(file: File): Boolean {
        if (!file.isFile || file.length() < MIN_AUDIO_BYTES || file.length() > MAX_AUDIO_BYTES) {
            return false
        }
        return runCatching {
            file.inputStream().buffered().use { input ->
                val header = ByteArray(3)
                if (input.read(header) != 3) return@use false
                val hasId3 = header[0] == 'I'.code.toByte() &&
                    header[1] == 'D'.code.toByte() &&
                    header[2] == '3'.code.toByte()
                val frameSync = header[0] == 0xFF.toByte() &&
                    (header[1].toInt() and 0xE0) == 0xE0
                hasId3 || frameSync
            }
        }.getOrDefault(false)
    }
}
