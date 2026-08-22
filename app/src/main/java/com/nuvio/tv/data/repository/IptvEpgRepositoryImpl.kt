package com.nuvio.tv.data.repository

import android.content.Context
import android.net.Uri
import com.nuvio.tv.data.iptv.epg.XmltvParser
import com.nuvio.tv.data.local.iptv.db.IptvDatabaseHelper
import com.nuvio.tv.domain.model.iptv.IptvChannel
import com.nuvio.tv.domain.model.iptv.IptvEpgProgram
import com.nuvio.tv.domain.model.iptv.IptvEpgSource
import com.nuvio.tv.domain.model.iptv.IptvEpgSourceKind
import com.nuvio.tv.domain.repository.IptvEpgRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IptvEpgRepositoryImpl @Inject constructor(
    private val dbHelper: IptvDatabaseHelper,
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) : IptvEpgRepository {
    private val importMutex = Mutex()

    override suspend fun refreshEpgForPlaylist(playlistId: String, sourceName: String, epgUrl: String): Result<Int> = withContext(Dispatchers.IO) {
        val source = IptvEpgSource(
            id = "playlist_$playlistId",
            name = sourceName,
            location = epgUrl,
            kind = IptvEpgSourceKind.PLAYLIST,
            playlistId = playlistId
        )
        refreshSource(source)
    }

    private suspend fun importEpg(ownerId: String, epgUrl: String): Result<Int> = importMutex.withLock {
        runCatching {
            val rawStream: InputStream = if (epgUrl.startsWith("http://") || epgUrl.startsWith("https://")) {
                val request = Request.Builder()
                    .url(epgUrl)
                    .header("Accept-Encoding", "gzip, deflate")
                    .build()
                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) throw IOException("Failed to download EPG: HTTP ${response.code}")
                val body = response.body ?: throw IOException("Empty EPG response")
                body.byteStream()
            } else if (epgUrl.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(epgUrl))
                    ?: throw IOException("Unable to open the selected EPG file")
            } else {
                val file = File(epgUrl)
                if (!file.exists()) throw IOException("Local EPG file not found: $epgUrl")
                FileInputStream(file)
            }

            wrapDecompressingStream(rawStream).use { stream ->
                val targetKeys = dbHelper.getAllChannelMatchKeys()
                val programs = XmltvParser.parse(stream, ownerId, targetKeys)
                if (programs.isEmpty()) throw IOException("No programmes found for saved channels in this XMLTV source")
                dbHelper.replaceEpgPrograms(ownerId, programs)
                dbHelper.removeEpgBefore(System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L)
                programs.size
            }
        }
    }

    private fun wrapDecompressingStream(rawStream: InputStream): InputStream {
        val buffered = BufferedInputStream(rawStream, 8192)
        buffered.mark(2)
        val b1 = buffered.read()
        val b2 = buffered.read()
        buffered.reset()
        return if (b1 == 0x1f && b2 == 0x8b) {
            GZIPInputStream(buffered)
        } else {
            buffered
        }
    }

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

    override fun getCurrentAndNextProgram(channel: IptvChannel): Flow<Pair<IptvEpgProgram?, IptvEpgProgram?>> {
        val keys = getMatchKeys(channel)
        return dbHelper.dbUpdates
            .onStart { emit(Unit) }
            .map {
                val nowMs = System.currentTimeMillis()
                val curr = dbHelper.getCurrentProgram(keys, nowMs)
                val next = dbHelper.getNextProgram(keys, nowMs)
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

    override fun getScheduleForChannel(
        channel: IptvChannel,
        windowStartEpochMs: Long,
        windowEndEpochMs: Long
    ): Flow<List<IptvEpgProgram>> {
        return dbHelper.dbUpdates
            .onStart { emit(Unit) }
            .map {
                val keys = getMatchKeys(channel)
                dbHelper.getSchedule(keys, windowStartEpochMs, windowEndEpochMs)
            }
            .flowOn(Dispatchers.IO)
    }

    override fun getSchedulesForWindow(
        windowStartEpochMs: Long,
        windowEndEpochMs: Long
    ): Flow<Map<String, List<IptvEpgProgram>>> {
        return dbHelper.dbUpdates
            .onStart { emit(Unit) }
            .map {
                dbHelper.getSchedulesForWindow(windowStartEpochMs, windowEndEpochMs)
            }
            .flowOn(Dispatchers.IO)
    }

    private fun getMatchKeys(channel: IptvChannel): List<String> {
        val keys = LinkedHashSet<String>()
        channel.tvgId?.trim()?.takeIf { it.isNotBlank() }?.let { id ->
            keys.add(id)
            keys.add(id.lowercase())
            val base = id.substringBeforeLast('.')
            if (base.isNotBlank()) {
                keys.add(base)
                keys.add(base.lowercase())
                val norm = XmltvParser.normalize(base)
                if (norm.isNotBlank()) keys.add(norm)
            }
        }
        channel.tvgName?.trim()?.takeIf { it.isNotBlank() }?.let { name ->
            keys.add(name)
            keys.add(name.lowercase())
            val norm = XmltvParser.normalize(name)
            if (norm.isNotBlank()) keys.add(norm)
        }
        channel.name.trim().takeIf { it.isNotBlank() }?.let { n ->
            keys.add(n)
            keys.add(n.lowercase())
            val norm = XmltvParser.normalize(n)
            if (norm.isNotBlank()) keys.add(norm)
        }
        return keys.toList()
    }

    override suspend fun clearEpgForPlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        dbHelper.clearEpgForPlaylist("playlist_$playlistId")
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
            dbHelper.getEpgSources().filter { it.kind == IptvEpgSourceKind.MANUAL }
                .sumOf { refreshSource(it).getOrElse { 0 } }
        }
    }

    override suspend fun refreshAllEpgSources(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            dbHelper.getEpgSources().sumOf { refreshSource(it).getOrElse { 0 } }
        }
    }

    override suspend fun clearAllEpg() = withContext(Dispatchers.IO) { dbHelper.clearAllEpg() }
}
