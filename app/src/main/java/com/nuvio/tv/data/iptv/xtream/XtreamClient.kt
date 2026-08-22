package com.nuvio.tv.data.iptv.xtream

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nuvio.tv.domain.model.iptv.IptvChannel
import com.nuvio.tv.domain.model.iptv.IptvGroup
import com.nuvio.tv.domain.model.iptv.IptvStreamFormat
import com.nuvio.tv.domain.model.iptv.XtreamCredentials
import com.nuvio.tv.BuildConfig
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
            // HLS avoids provider-specific MPEG-TS endpoint restrictions and is natively
            // supported by Media3. Fall back to TS when the account does not advertise HLS.
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
                            // Match the authenticated Xtream API request. Several providers reject
                            // a stream URL when its User-Agent changes after login (often as 458).
                            headers = mapOf("User-Agent" to "Nuvio/${BuildConfig.VERSION_NAME.ifBlank { "dev" }}"),
                            streamFormat = if (container == "m3u8") IptvStreamFormat.HLS else IptvStreamFormat.MPEGTS
                        )
                    )
                }
            }

            // Update channel counts
            val counts = channels.groupingBy { it.groupId }.eachCount()
            val finalGroups = groups.map { grp ->
                grp.copy(channelCount = counts[grp.id] ?: 0)
            }

            Pair(finalGroups, channels)
        }
    }
}
