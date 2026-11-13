package com.nuur.muslim.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.nuur.muslim.data.local.AudioDownloadDao
import com.nuur.muslim.data.local.AudioRepeatMode
import com.nuur.muslim.data.local.AyahEntity
import com.nuur.muslim.data.local.DownloadedAudioEntity
import com.nuur.muslim.data.local.SurahEntity
import com.nuur.muslim.domain.RecitersCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class PlaybackUiState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentSurahNumber: Int = 1,
    val currentSurahName: String = "",
    val currentAyahNumber: Int = 1,
    val currentGlobalAyah: Int = 1,
    val reciterId: String = "ar.alafasy",
    val repeatMode: AudioRepeatMode = AudioRepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null,
    val downloadingSurahNumber: Int? = null,
    val downloadProgress: Float = 0f,
    val lastFailedSurahNumber: Int? = null
)

class AudioPlaybackManager(
    private val context: Context,
    private val audioDao: AudioDownloadDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var tickerJob: Job? = null
    private var downloadJob: Job? = null
    @Volatile
    private var activeHttpCall: Call? = null

    private fun ensureController(onReady: (MediaController) -> Unit) {
        val existing = controller
        if (existing != null && existing.isConnected) {
            onReady(existing)
            return
        }
        val token = SessionToken(context, ComponentName(context, QuranPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                runCatching {
                    val mc = future.get()
                    attachListener(mc)
                    controller = mc
                    onReady(mc)
                }.onFailure { err ->
                    _state.update { it.copy(errorMessage = err.localizedMessage ?: "Audio service unavailable") }
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun attachListener(mc: MediaController) {
        mc.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startTicker() else stopTicker()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.update {
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        durationMs = mc.duration.coerceAtLeast(0L)
                    )
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val extras = mediaItem?.mediaMetadata?.extras
                val surahNum = extras?.getInt("surahNumber") ?: _state.value.currentSurahNumber
                val ayahNum = extras?.getInt("ayahNumber") ?: _state.value.currentAyahNumber
                val globalNum = extras?.getInt("globalNumber") ?: _state.value.currentGlobalAyah
                _state.update {
                    it.copy(
                        currentSurahNumber = surahNum,
                        currentAyahNumber = ayahNum,
                        currentGlobalAyah = globalNum,
                        positionMs = 0L,
                        durationMs = mc.duration.coerceAtLeast(0L),
                        errorMessage = null
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = error.localizedMessage ?: "Playback failed"
                    )
                }
            }
        })
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val mc = controller
                if (mc != null) {
                    _state.update {
                        it.copy(
                            positionMs = mc.currentPosition.coerceAtLeast(0L),
                            durationMs = mc.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun playSurahFromAyah(
        surah: SurahEntity,
        ayahs: List<AyahEntity>,
        startIndex: Int,
        reciterId: String,
        repeatMode: AudioRepeatMode,
        playbackSpeed: Float = _state.value.playbackSpeed
    ) {
        if (ayahs.isEmpty()) return
        val safeIdx = startIndex.coerceIn(0, ayahs.lastIndex)
        val reciter = RecitersCatalog.findById(reciterId)

        scope.launch {
            val mediaItems = withContext(Dispatchers.IO) {
                ayahs.map { ayah ->
                    val localEntry = audioDao.getDownloadedAyah(reciter.id, ayah.globalNumber)
                    val localFile = localEntry?.localPath?.let { File(it) }
                    val uri = if (localFile != null && localFile.exists() && localFile.length() > 0L) {
                        Uri.fromFile(localFile)
                    } else {
                        Uri.parse(RecitersCatalog.audioUrlForAyah(reciter.id, ayah.globalNumber))
                    }
                    val extras = Bundle().apply {
                        putInt("surahNumber", surah.number)
                        putInt("ayahNumber", ayah.numberInSurah)
                        putInt("globalNumber", ayah.globalNumber)
                    }
                    MediaItem.Builder()
                        .setMediaId("${reciter.id}:${ayah.globalNumber}")
                        .setUri(uri)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle("${surah.englishName} (${surah.number}:${ayah.numberInSurah})")
                                .setArtist(reciter.englishName)
                                .setExtras(extras)
                                .build()
                        )
                        .build()
                }
            }

            val clampedSpeed = playbackSpeed.coerceIn(0.75f, 1.5f)
            ensureController { mc ->
                mc.setMediaItems(mediaItems, safeIdx, 0L)
                applyRepeatMode(mc, repeatMode)
                mc.setPlaybackSpeed(clampedSpeed)
                mc.prepare()
                mc.play()
                val target = ayahs[safeIdx]
                _state.update {
                    it.copy(
                        isActive = true,
                        currentSurahNumber = surah.number,
                        currentSurahName = surah.englishName,
                        currentAyahNumber = target.numberInSurah,
                        currentGlobalAyah = target.globalNumber,
                        reciterId = reciter.id,
                        repeatMode = repeatMode,
                        playbackSpeed = clampedSpeed,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun togglePlayPause() {
        val mc = controller ?: return
        if (mc.isPlaying) {
            mc.pause()
        } else {
            mc.play()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
        _state.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun nextAyah() {
        val mc = controller ?: return
        if (mc.hasNextMediaItem()) mc.seekToNextMediaItem()
    }

    fun previousAyah() {
        val mc = controller ?: return
        if (mc.hasPreviousMediaItem()) mc.seekToPreviousMediaItem()
    }

    fun setRepeatMode(mode: AudioRepeatMode) {
        _state.update { it.copy(repeatMode = mode) }
        controller?.let { applyRepeatMode(it, mode) }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.75f, 1.5f)
        _state.update { it.copy(playbackSpeed = clamped) }
        controller?.setPlaybackSpeed(clamped)
    }

    private fun applyRepeatMode(mc: MediaController, mode: AudioRepeatMode) {
        mc.repeatMode = when (mode) {
            AudioRepeatMode.OFF -> Player.REPEAT_MODE_OFF
            AudioRepeatMode.AYAH -> Player.REPEAT_MODE_ONE
            AudioRepeatMode.SURAH -> Player.REPEAT_MODE_ALL
        }
    }

    fun stopAndDismiss() {
        controller?.stop()
        stopTicker()
        _state.update { it.copy(isActive = false, isPlaying = false, positionMs = 0L) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun downloadSurahAudio(
        surah: SurahEntity,
        ayahs: List<AyahEntity>,
        reciterId: String,
        onComplete: (Result<Unit>) -> Unit = {}
    ) {
        if (downloadJob?.isActive == true) return
        downloadJob = scope.launch {
            _state.update {
                it.copy(
                    downloadingSurahNumber = surah.number,
                    downloadProgress = 0f,
                    errorMessage = null,
                    lastFailedSurahNumber = null
                )
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val total = ayahs.size.coerceAtLeast(1)
                    ayahs.forEachIndexed { idx, ayah ->
                        coroutineContext.ensureActive()
                        val dest = RecitersCatalog.localAudioFile(context.filesDir, reciterId, ayah.globalNumber)
                        if (!dest.exists() || dest.length() == 0L) {
                            val url = RecitersCatalog.audioUrlForAyah(reciterId, ayah.globalNumber)
                            val req = Request.Builder().url(url).build()
                            val call = httpClient.newCall(req)
                            activeHttpCall = call
                            try {
                                call.execute().use { resp ->
                                    if (!resp.isSuccessful) error("HTTP ${resp.code} for ayah ${ayah.numberInSurah}")
                                    val bodyStream = resp.body?.byteStream() ?: error("Empty audio response")
                                    writeStreamAtomically(bodyStream, dest) { coroutineContext.ensureActive() }
                                }
                            } finally {
                                if (activeHttpCall === call) {
                                    activeHttpCall = null
                                }
                            }
                        }
                        audioDao.insertAudio(
                            DownloadedAudioEntity(
                                reciterId = reciterId,
                                globalAyahNumber = ayah.globalNumber,
                                surahNumber = surah.number,
                                ayahNumber = ayah.numberInSurah,
                                localPath = dest.absolutePath,
                                fileSizeBytes = dest.length()
                            )
                        )
                        _state.update {
                            it.copy(downloadProgress = (idx + 1).toFloat() / total.toFloat())
                        }
                    }
                }.onFailure { err ->
                    if (err is CancellationException) throw err
                }
            }
            _state.update {
                it.copy(
                    downloadingSurahNumber = null,
                    downloadProgress = 0f,
                    errorMessage = result.exceptionOrNull()?.localizedMessage,
                    lastFailedSurahNumber = if (result.isFailure) surah.number else null
                )
            }
            onComplete(result)
        }
    }

    fun cancelDownload() {
        activeHttpCall?.cancel()
        activeHttpCall = null
        downloadJob?.cancel()
        downloadJob = null
        _state.update {
            it.copy(downloadingSurahNumber = null, downloadProgress = 0f)
        }
    }

    fun deleteSurahAudio(surahNumber: Int, reciterId: String) {
        scope.launch(Dispatchers.IO) {
            val entries = audioDao.getDownloadedForSurah(reciterId, surahNumber)
            entries.forEach { entry ->
                runCatching { File(entry.localPath).delete() }
            }
            audioDao.deleteForSurah(reciterId, surahNumber)
        }
    }

    companion object {
        internal fun writeStreamAtomically(
            inputStream: InputStream,
            dest: File,
            checkActive: () -> Unit = {}
        ) {
            dest.parentFile?.mkdirs()
            val tmp = File(dest.parentFile, "${dest.name}.tmp")
            var completed = false
            try {
                inputStream.use { input ->
                    tmp.outputStream().buffered().use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            checkActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                    }
                }
                if (dest.exists()) {
                    dest.delete()
                }
                if (!tmp.renameTo(dest)) {
                    tmp.inputStream().use { src ->
                        dest.outputStream().use { dst ->
                            src.copyTo(dst)
                        }
                    }
                    tmp.delete()
                }
                completed = true
            } finally {
                if (!completed) {
                    tmp.delete()
                }
            }
        }
    }
}
