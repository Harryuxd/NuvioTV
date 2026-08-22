package com.nuvio.tv.data.iptv.xtream

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.data.iptv.epg.XmltvParser
import com.nuvio.tv.domain.model.iptv.IptvChannel
import com.nuvio.tv.domain.model.iptv.IptvGroup
import com.nuvio.tv.domain.model.iptv.IptvStreamFormat
import com.nuvio.tv.domain.model.iptv.IptvVodCategory
import com.nuvio.tv.domain.model.iptv.IptvVodItem
import com.nuvio.tv.domain.model.iptv.IptvVodType
import com.nuvio.tv.domain.model.iptv.XtreamCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XtreamClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()

    suspend fun authenticate(credentials: XtreamCredentials): Result<XtreamAuthResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val base = credentials.serverUrl.trimEnd('/')
            val url = "$base/player_api.php?username=${credentials.username}&password=${credentials.password}"
            val request = Request.Builder().url(url).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP error ${response.code}")
                val body = response.body?.string() ?: throw IOException("Empty response body")
                val authResponse = gson.fromJson(body, XtreamAuthResponse::class.java)

                val authStatus = authResponse?.userInfo?.auth
                val status = authResponse?.userInfo?.status
                if (authStatus != 1 && status?.lowercase() != "active") {
                    throw IOException("Authentication failed: invalid credentials or account expired")
                }
                authResponse
            }
        }
    }

    suspend fun fetchLiveStreams(
        playlistId: String,
        credentials: XtreamCredentials
    ): Result<Pair<List<IptvGroup>, List<IptvChannel>>> = withContext(Dispatchers.IO) {
        runCatching {
            val base = credentials.serverUrl.trimEnd('/')
            val auth = authenticate(credentials).getOrThrow()
            val allowedFormats = auth.userInfo?.allowedOutputFormats.orEmpty().map { it.lowercase() }
            val container = if ("m3u8" in allowedFormats) "m3u8" else "ts"

            // 1. Fetch categories
            val categoriesUrl = "$base/player_api.php?username=${credentials.username}&password=${credentials.password}&action=get_live_categories"
            val categoriesRequest = Request.Builder().url(categoriesUrl).build()

            val categoriesMap = mutableMapOf<String, String>()
            val groups = mutableListOf<IptvGroup>()

            okHttpClient.newCall(categoriesRequest).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: "[]"
                    val type = object : TypeToken<List<XtreamCategory>>() {}.type
                    val catList: List<XtreamCategory>? = runCatching { gson.fromJson<List<XtreamCategory>>(body, type) }.getOrNull()
                    catList?.forEachIndexed { index, cat ->
                        categoriesMap[cat.categoryId] = cat.categoryName
                        groups.add(
                            IptvGroup(
                                id = "${playlistId}_xc_${cat.categoryId}",
                                playlistId = playlistId,
                                title = cat.categoryName,
                                orderIndex = index
                            )
                        )
                    }
                }
            }

            // 2. Fetch live streams
            val streamsUrl = "$base/player_api.php?username=${credentials.username}&password=${credentials.password}&action=get_live_streams"
            val streamsRequest = Request.Builder().url(streamsUrl).build()

            val channels = mutableListOf<IptvChannel>()

            okHttpClient.newCall(streamsRequest).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("Failed to fetch streams: HTTP ${resp.code}")
                val body = resp.body?.string() ?: throw IOException("Empty streams response")
                val type = object : TypeToken<List<XtreamStreamItem>>() {}.type
                val streamList: List<XtreamStreamItem> = gson.fromJson(body, type) ?: emptyList()

                streamList.forEachIndexed { index, item ->
                    val catId = item.categoryId ?: "0"
                    val groupTitle = categoriesMap[catId] ?: "Live TV"
                    val groupId = "${playlistId}_xc_$catId"

                    val streamUrl = "$base/live/${credentials.username}/${credentials.password}/${item.streamId}.$container"
                    val channelNumber = (item.num as? Number)?.toInt() ?: (index + 1)

                    channels.add(
                        IptvChannel(
                            id = "${playlistId}_xc_stream_${item.streamId}",
                            playlistId = playlistId,
                            groupId = groupId,
                            groupTitle = groupTitle,
                            name = item.name,
                            streamUrl = streamUrl,
                            logoUrl = item.streamIcon?.takeIf { it.isNotBlank() },
                            tvgId = item.epgChannelId?.takeIf { it.isNotBlank() },
                            tvgName = item.name,
                            channelNumber = channelNumber,
                            headers = mapOf("User-Agent" to "Nuvio/${BuildConfig.VERSION_NAME.ifBlank { "dev" }}"),
                            streamFormat = if (container == "m3u8") IptvStreamFormat.HLS else IptvStreamFormat.MPEGTS
                        )
                    )
                }
            }

            val counts = channels.groupingBy { it.groupId }.eachCount()
            val finalGroups = groups.map { grp ->
                grp.copy(channelCount = counts[grp.id] ?: 0)
            }

            Pair(finalGroups, channels)
        }
    }

    suspend fun fetchVodMovies(
        playlistId: String,
        credentials: XtreamCredentials
    ): Result<Pair<List<IptvVodCategory>, List<IptvVodItem>>> = withContext(Dispatchers.IO) {
        runCatching {
            val base = credentials.serverUrl.trimEnd('/')

            // 1. Fetch categories
            val categoriesUrl = "$base/player_api.php?username=${credentials.username}&password=${credentials.password}&action=get_vod_categories"
            val categoriesRequest = Request.Builder().url(categoriesUrl).build()
            val categoriesMap = mutableMapOf<String, String>()
            val categories = mutableListOf<IptvVodCategory>()

            okHttpClient.newCall(categoriesRequest).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: "[]"
                    val type = object : TypeToken<List<XtreamCategory>>() {}.type
                    val catList: List<XtreamCategory>? = runCatching { gson.fromJson<List<XtreamCategory>>(body, type) }.getOrNull()
                    catList?.forEach { cat ->
                        categoriesMap[cat.categoryId] = cat.categoryName
                        categories.add(
                            IptvVodCategory(
                                id = "${playlistId}_vod_cat_${cat.categoryId}",
                                playlistId = playlistId,
                                name = cat.categoryName,
                                type = IptvVodType.MOVIE
                            )
                        )
                    }
                }
            }

            // 2. Fetch VOD streams
            val vodUrl = "$base/player_api.php?username=${credentials.username}&password=${credentials.password}&action=get_vod_streams"
            val vodRequest = Request.Builder().url(vodUrl).build()
            val items = mutableListOf<IptvVodItem>()

            okHttpClient.newCall(vodRequest).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("Failed to fetch VOD streams: HTTP ${resp.code}")
                val body = resp.body?.string() ?: "[]"
                val type = object : TypeToken<List<XtreamVodItem>>() {}.type
                val vodList: List<XtreamVodItem> = runCatching { gson.fromJson<List<XtreamVodItem>>(body, type) }.getOrNull() ?: emptyList()

                vodList.forEach { item ->
                    val catId = item.categoryId ?: "0"
                    val catName = categoriesMap[catId] ?: "Movies"
                    val ext = item.containerExtension?.takeIf { it.isNotBlank() } ?: "mp4"
                    val streamUrl = "$base/movie/${credentials.username}/${credentials.password}/${item.streamId}.$ext"

                    val (cleanTitle, year) = parseTitleAndYear(item.name)
                    val normTitle = XmltvParser.normalize(cleanTitle)

                    items.add(
                        IptvVodItem(
                            id = "${playlistId}_vod_${item.streamId}",
                            playlistId = playlistId,
                            title = item.name,
                            normalizedTitle = normTitle,
                            year = year,
                            type = IptvVodType.MOVIE,
                            categoryId = "${playlistId}_vod_cat_$catId",
                            categoryName = catName,
                            streamUrl = streamUrl,
                            containerExtension = ext,
                            posterUrl = item.streamIcon?.takeIf { it.isNotBlank() },
                            rating = (item.rating as? Number)?.toDouble() ?: (item.rating5Based as? Number)?.toDouble()?.times(2),
                            headers = mapOf("User-Agent" to "Nuvio/${BuildConfig.VERSION_NAME.ifBlank { "dev" }}")
                        )
                    )
                }
            }

            val counts = items.groupingBy { it.categoryId }.eachCount()
            val finalCategories = categories.map { it.copy(itemCount = counts[it.id] ?: 0) }

            Pair(finalCategories, items)
        }
    }

    suspend fun fetchSeries(
        playlistId: String,
        credentials: XtreamCredentials
    ): Result<Pair<List<IptvVodCategory>, List<IptvVodItem>>> = withContext(Dispatchers.IO) {
        runCatching {
            val base = credentials.serverUrl.trimEnd('/')

            // 1. Series categories
            val categoriesUrl = "$base/player_api.php?username=${credentials.username}&password=${credentials.password}&action=get_series_categories"
            val categoriesRequest = Request.Builder().url(categoriesUrl).build()
            val categoriesMap = mutableMapOf<String, String>()
            val categories = mutableListOf<IptvVodCategory>()

            okHttpClient.newCall(categoriesRequest).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: "[]"
                    val type = object : TypeToken<List<XtreamCategory>>() {}.type
                    val catList: List<XtreamCategory>? = runCatching { gson.fromJson<List<XtreamCategory>>(body, type) }.getOrNull()
                    catList?.forEach { cat ->
                        categoriesMap[cat.categoryId] = cat.categoryName
                        categories.add(
                            IptvVodCategory(
                                id = "${playlistId}_series_cat_${cat.categoryId}",
                                playlistId = playlistId,
                                name = cat.categoryName,
                                type = IptvVodType.SERIES_EPISODE
                            )
                        )
                    }
                }
            }

            // 2. Series list
            val seriesUrl = "$base/player_api.php?username=${credentials.username}&password=${credentials.password}&action=get_series"
            val seriesRequest = Request.Builder().url(seriesUrl).build()
            val items = mutableListOf<IptvVodItem>()

            okHttpClient.newCall(seriesRequest).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: "[]"
                    val type = object : TypeToken<List<XtreamSeriesItem>>() {}.type
                    val seriesList: List<XtreamSeriesItem> = runCatching { gson.fromJson<List<XtreamSeriesItem>>(body, type) }.getOrNull() ?: emptyList()

                    seriesList.forEach { s ->
                        val catId = s.categoryId ?: "0"
                        val catName = categoriesMap[catId] ?: "Series"
                        val (cleanTitle, year) = parseTitleAndYear(s.name)
                        val normTitle = XmltvParser.normalize(cleanTitle)

                        items.add(
                            IptvVodItem(
                                id = "${playlistId}_series_${s.seriesId}",
                                playlistId = playlistId,
                                title = s.name,
                                normalizedTitle = normTitle,
                                year = year,
                                type = IptvVodType.SERIES_EPISODE,
                                categoryId = "${playlistId}_series_cat_$catId",
                                categoryName = catName,
                                seriesId = s.seriesId.toString(),
                                seriesName = s.name,
                                streamUrl = "$base/series/${credentials.username}/${credentials.password}/${s.seriesId}",
                                posterUrl = s.cover?.takeIf { it.isNotBlank() },
                                rating = (s.rating as? Number)?.toDouble() ?: (s.rating5Based as? Number)?.toDouble()?.times(2),
                                genre = s.genre,
                                releaseDate = s.releaseDate,
                                plot = s.plot,
                                headers = mapOf("User-Agent" to "Nuvio/${BuildConfig.VERSION_NAME.ifBlank { "dev" }}")
                            )
                        )
                    }
                }
            }

            val counts = items.groupingBy { it.categoryId }.eachCount()
            val finalCategories = categories.map { it.copy(itemCount = counts[it.id] ?: 0) }

            Pair(finalCategories, items)
        }
    }

    suspend fun getSeriesInfo(
        credentials: XtreamCredentials,
        seriesId: String
    ): Result<Map<String, List<XtreamEpisodeItem>>> = withContext(Dispatchers.IO) {
        runCatching {
            val base = credentials.serverUrl.trimEnd('/')
            val url = "$base/player_api.php?username=${credentials.username}&password=${credentials.password}&action=get_series_info&series_id=$seriesId"
            val request = Request.Builder().url(url).build()

            okHttpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                val body = resp.body?.string() ?: throw IOException("Empty series info")
                val json = JsonParser.parseString(body).asJsonObject
                val episodesObj = json.getAsJsonObject("episodes")
                val map = mutableMapOf<String, List<XtreamEpisodeItem>>()
                if (episodesObj != null) {
                    val epType = object : TypeToken<List<XtreamEpisodeItem>>() {}.type
                    for (entry in episodesObj.entrySet()) {
                        val seasonKey = entry.key
                        val epList: List<XtreamEpisodeItem> = runCatching {
                            gson.fromJson<List<XtreamEpisodeItem>>(entry.value, epType)
                        }.getOrNull() ?: emptyList()
                        map[seasonKey] = epList
                    }
                }
                map
            }
        }
    }

    companion object {
        fun parseTitleAndYear(raw: String): Pair<String, Int?> {
            val yearRegex = Regex("""[\(\[\s](\d{4})[\)\]\s]?.*$""")
            val match = yearRegex.find(raw)
            val year = match?.groupValues?.getOrNull(1)?.toIntOrNull()
            val cleanTitle = if (match != null) {
                raw.substring(0, match.range.first).trim()
            } else {
                raw.trim()
            }
            return Pair(cleanTitle.ifBlank { raw.trim() }, year)
        }
    }
}
