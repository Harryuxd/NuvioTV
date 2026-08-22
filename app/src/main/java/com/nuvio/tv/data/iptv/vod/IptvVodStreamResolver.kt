package com.nuvio.tv.data.iptv.vod

import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.data.iptv.xtream.XtreamClient
import com.nuvio.tv.data.local.iptv.IptvCredentialStore
import com.nuvio.tv.data.local.iptv.db.IptvDatabaseHelper
import com.nuvio.tv.domain.model.AddonStreams
import com.nuvio.tv.domain.model.ProxyHeaders
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamBehaviorHints
import com.nuvio.tv.domain.model.iptv.IptvVodItem
import com.nuvio.tv.domain.repository.MetaRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IptvVodStreamResolver @Inject constructor(
    private val dbHelper: IptvDatabaseHelper,
    private val credentialStore: IptvCredentialStore,
    private val xtreamClient: XtreamClient,
    private val metaRepository: MetaRepository
) {
    suspend fun resolveVodStreams(
        type: String,
        videoId: String,
        season: Int?,
        episode: Int?
    ): AddonStreams? {
        val cleanType = type.lowercase()
        val baseId = videoId.substringBefore(':')

        // 1. Fetch title and release info from meta
        val metaResult = metaRepository.getMetaFromPrimaryAddon(cleanType, baseId).firstOrNull()
        val meta = (metaResult as? NetworkResult.Success)?.data ?: return null

        val title = meta.name
        val year = meta.releaseInfo?.filter { it.isDigit() }?.take(4)?.toIntOrNull()

        val streams = mutableListOf<Stream>()

        if (cleanType == "movie") {
            val vodItems = dbHelper.findVodStreamsForMovie(title, year)
            for (vod in vodItems) {
                val stream = buildMovieStream(vod)
                if (stream != null) streams.add(stream)
            }
        } else if (cleanType == "series") {
            val s = season ?: extractSeasonFromVideoId(videoId)
            val ep = episode ?: extractEpisodeFromVideoId(videoId)

            val seriesItems = dbHelper.findVodStreamsForSeries(title, s, ep)
            for (series in seriesItems) {
                val epStreams = resolveSeriesEpisodeStream(series, s, ep)
                streams.addAll(epStreams)
            }
        }

        if (streams.isEmpty()) return null

        return AddonStreams(
            addonName = "IPTV VOD",
            addonLogo = null,
            streams = streams
        )
    }

    private fun buildMovieStream(vod: IptvVodItem): Stream {
        val playlist = dbHelper.getPlaylistById(vod.playlistId)
        val providerName = playlist?.name ?: "IPTV"
        val quality = extractQuality(vod.title)

        return Stream(
            name = "[IPTV] $providerName",
            title = vod.title,
            description = "Direct VOD Stream • ${vod.containerExtension.uppercase()}" + (if (vod.rating != null) " • ⭐ ${String.format("%.1f", vod.rating)}" else ""),
            url = vod.streamUrl,
            ytId = null,
            infoHash = null,
            fileIdx = null,
            externalUrl = null,
            behaviorHints = StreamBehaviorHints(
                notWebReady = false,
                bingeGroup = null,
                countryWhitelist = null,
                proxyHeaders = if (vod.headers.isNotEmpty()) ProxyHeaders(request = vod.headers, response = null) else null
            ),
            addonName = "[IPTV] $providerName",
            addonLogo = null,
            quality = quality,
            qualityValue = getQualityValue(quality)
        )
    }

    private suspend fun resolveSeriesEpisodeStream(
        series: IptvVodItem,
        season: Int?,
        episode: Int?
    ): List<Stream> {
        val sNum = season ?: 1
        val epNum = episode ?: 1
        val creds = credentialStore.getCredentials(series.playlistId)
        val playlist = dbHelper.getPlaylistById(series.playlistId)
        val providerName = playlist?.name ?: "IPTV"

        if (creds != null && !series.seriesId.isNullOrBlank()) {
            val episodesMap = xtreamClient.getSeriesInfo(creds, series.seriesId).getOrNull()
            if (episodesMap != null) {
                val seasonEpisodes = episodesMap[sNum.toString()] ?: episodesMap.values.flatten()
                val targetEp = seasonEpisodes.firstOrNull {
                    val num = (it.episodeNum as? Number)?.toInt() ?: it.episodeNum?.toString()?.toIntOrNull()
                    num == epNum
                }

                if (targetEp != null) {
                    val ext = targetEp.containerExtension?.takeIf { it.isNotBlank() } ?: "mp4"
                    val epUrl = "${creds.serverUrl.trimEnd('/')}/series/${creds.username}/${creds.password}/${targetEp.id}.$ext"
                    val epTitle = targetEp.title?.takeIf { it.isNotBlank() } ?: "${series.title} S${sNum}E${epNum}"

                    return listOf(
                        Stream(
                            name = "[IPTV] $providerName",
                            title = epTitle,
                            description = "S${sNum}E${epNum} • Direct Series Stream • ${ext.uppercase()}",
                            url = epUrl,
                            ytId = null,
                            infoHash = null,
                            fileIdx = null,
                            externalUrl = null,
                            behaviorHints = StreamBehaviorHints(
                                notWebReady = false,
                                bingeGroup = "iptv_${series.seriesId}",
                                countryWhitelist = null,
                                proxyHeaders = if (series.headers.isNotEmpty()) ProxyHeaders(request = series.headers, response = null) else null
                            ),
                            addonName = "[IPTV] $providerName",
                            addonLogo = null,
                            quality = "1080p",
                            qualityValue = 1080
                        )
                    )
                }
            }
        }
        return emptyList()
    }

    private fun extractSeasonFromVideoId(videoId: String): Int? {
        val parts = videoId.split(':')
        return parts.getOrNull(1)?.toIntOrNull()
    }

    private fun extractEpisodeFromVideoId(videoId: String): Int? {
        val parts = videoId.split(':')
        return parts.getOrNull(2)?.toIntOrNull()
    }

    private fun extractQuality(title: String): String {
        val lower = title.lowercase()
        return when {
            "4k" in lower || "2160p" in lower || "uhd" in lower -> "4K"
            "1080p" in lower || "fhd" in lower -> "1080p"
            "720p" in lower || "hd" in lower -> "720p"
            "480p" in lower || "sd" in lower -> "480p"
            else -> "1080p"
        }
    }

    private fun getQualityValue(quality: String): Int = when (quality) {
        "4K" -> 2160
        "1080p" -> 1080
        "720p" -> 720
        "480p" -> 480
        else -> 1080
    }
}
