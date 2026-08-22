package com.nuvio.tv.domain.repository

import com.nuvio.tv.domain.model.iptv.IptvChannel
import com.nuvio.tv.domain.model.iptv.IptvEpgProgram
import com.nuvio.tv.domain.model.iptv.IptvEpgSource
import kotlinx.coroutines.flow.Flow

interface IptvEpgRepository {
    suspend fun refreshEpgForPlaylist(playlistId: String, sourceName: String, epgUrl: String): Result<Int>
    fun getCurrentAndNextProgram(tvgId: String): Flow<Pair<IptvEpgProgram?, IptvEpgProgram?>>
    fun getCurrentAndNextProgram(channel: IptvChannel): Flow<Pair<IptvEpgProgram?, IptvEpgProgram?>>
    fun getScheduleForChannel(tvgId: String, windowStartEpochMs: Long, windowEndEpochMs: Long): Flow<List<IptvEpgProgram>>
    fun getScheduleForChannel(channel: IptvChannel, windowStartEpochMs: Long, windowEndEpochMs: Long): Flow<List<IptvEpgProgram>>
    fun getSchedulesForWindow(windowStartEpochMs: Long, windowEndEpochMs: Long): Flow<Map<String, List<IptvEpgProgram>>>
    suspend fun clearEpgForPlaylist(playlistId: String)
    fun getSources(): Flow<List<IptvEpgSource>>
    suspend fun saveManualSource(source: IptvEpgSource): Result<IptvEpgSource>
    suspend fun deleteManualSource(sourceId: String)
    suspend fun refreshSource(source: IptvEpgSource): Result<Int>
    suspend fun refreshManualSources(): Result<Int>
    suspend fun refreshAllEpgSources(): Result<Int>
    suspend fun clearAllEpg()
}
