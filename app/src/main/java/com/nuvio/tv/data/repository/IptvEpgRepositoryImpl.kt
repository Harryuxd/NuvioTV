package com.nuvio.tv.data.repository

import android.content.Context
import android.net.Uri
import com.nuvio.tv.data.iptv.epg.XmltvParser
import com.nuvio.tv.data.local.iptv.db.IptvDatabaseHelper
import com.nuvio.tv.domain.model.iptv.IptvEpgProgram
import com.nuvio.tv.domain.model.iptv.IptvEpgSource
import com.nuvio.tv.domain.model.iptv.IptvEpgSourceKind
import com.nuvio.tv.domain.repository.IptvEpgRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class IptvEpgRepositoryImpl @Inject constructor(
    private val dbHelper: IptvDatabaseHelper,
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) : IptvEpgRepository {
    private val importMutex = Mutex()

    override suspend fun refreshEpgForPlaylist(playlistId: String, sourceName: String, epgUrl: String): Result<Int> = withContext(Dispatchers.IO) {
        val source = IptvEpgSource(id = playlistId, name = sourceName, location = epgUrl, kind = IptvEpgSourceKind.PLAYLIST, playlistId = playlistId)
        refreshSource(source)
    }

    private suspend fun importEpg(ownerId: String, epgUrl: String): Result<Int> = importMutex.withLock { runCatching {
            val inputStream: InputStream = if (epgUrl.startsWith("http://") || epgUrl.startsWith("https://")) {
                val request = Request.Builder().url(epgUrl).build()
                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) throw IOException("Failed to download EPG: HTTP ${response.code}")
                val body = response.body ?: throw IOException("Empty EPG response")
                val stream = body.byteStream()
                if (epgUrl.endsWith(".gz", ignoreCase = true)) {
                    GZIPInputStream(stream)
                } else {
                    stream
                }
            } else if (epgUrl.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(epgUrl))
                    ?: throw IOException("Unable to open the selected EPG file")
            } else {
                val file = File(epgUrl)
                if (!file.exists()) throw IOException("Local EPG file not found: $epgUrl")
                val stream = FileInputStream(file)
                if (file.name.endsWith(".gz", ignoreCase = true)) {
                    GZIPInputStream(stream)
                } else {
                    stream
                }
            }

            inputStream.use { stream ->
                // Providers often ship 100k+ programmes. Keep only IDs that can be displayed
                // for the user's saved channels; this avoids a huge database and slow guide.
                val knownTvgIds = dbHelper.getAllChannelTvgIds()
                val programs = XmltvParser.parse(stream, ownerId).filter { it.channelTvgId in knownTvgIds }
                if (programs.isEmpty()) throw IOException("No programmes were found in this XMLTV source")
                dbHelper.replaceEpgPrograms(ownerId, programs)
                dbHelper.removeEpgBefore(System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L)
                programs.size
            }
    } }

    override fun getCurrentAndNextProgram(tvgId: String): Flow<Pair<IptvEpgProgram?, IptvEpgProgram?>> {
        return dbHelper.dbUpdates
            .onStart { emit(Unit) }
            .map {
                val nowMs = System.currentTimeMillis()
                val curr = dbHelper.getCurrentProgram(tvgId, nowMs)
                val next = dbHelper.getNextProgram(tvgId, nowMs)
                Pair(curr, next)
            }
    }

    override fun getScheduleForChannel(
        tvgId: String,
        windowStartEpochMs: Long,
        windowEndEpochMs: Long
    ): Flow<List<IptvEpgProgram>> {
        return dbHelper.dbUpdates
            .onStart { emit(Unit) }
            .map {
                dbHelper.getSchedule(tvgId, windowStartEpochMs, windowEndEpochMs)
            }
    }

    override suspend fun clearEpgForPlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        dbHelper.clearEpgForPlaylist(playlistId)
    }

    override fun getSources(): Flow<List<IptvEpgSource>> = dbHelper.getEpgSourcesFlow()

    override suspend fun saveManualSource(source: IptvEpgSource): Result<IptvEpgSource> = withContext(Dispatchers.IO) {
        runCatching {
            require(source.name.isNotBlank()) { "Enter a source name" }
            require(source.location.isNotBlank()) { "Enter an XMLTV URL or file path" }
            val saved = source.copy(
                id = source.id.ifBlank { UUID.randomUUID().toString() },
                kind = IptvEpgSourceKind.MANUAL,
                playlistId = null
            )
            dbHelper.upsertEpgSource(saved)
            saved
        }
    }

    override suspend fun deleteManualSource(sourceId: String) = withContext(Dispatchers.IO) {
        dbHelper.deleteEpgSource(sourceId)
    }

    override suspend fun refreshSource(source: IptvEpgSource): Result<Int> = withContext(Dispatchers.IO) {
        val result = importEpg(source.id, source.location)
        dbHelper.upsertEpgSource(source.copy(
            lastRefreshedEpochMs = if (result.isSuccess) System.currentTimeMillis() else source.lastRefreshedEpochMs,
            lastError = result.exceptionOrNull()?.message
        ))
        result
    }

    override suspend fun refreshManualSources(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            dbHelper.getEpgSources().sumOf { refreshSource(it).getOrElse { 0 } }
        }
    }

    override suspend fun clearAllEpg() = withContext(Dispatchers.IO) { dbHelper.clearAllEpg() }
}
