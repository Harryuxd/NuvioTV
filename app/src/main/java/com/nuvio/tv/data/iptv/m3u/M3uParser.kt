package com.nuvio.tv.data.iptv.m3u

import com.nuvio.tv.domain.model.iptv.IptvChannel
import com.nuvio.tv.domain.model.iptv.IptvGroup
import com.nuvio.tv.domain.model.iptv.IptvStreamFormat
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

data class ParsedM3uResult(
    val detectedEpgUrl: String? = null,
    val groups: List<IptvGroup>,
    val channels: List<IptvChannel>
)

object M3uParser {

    private val ATTRIBUTE_REGEX = Regex("""([a-zA-Z0-9_\-]+)=(?:"([^"]*)"|'([^']*)'|([^,\s]+))""")
    private val EXTM3U_EPG_REGEX = Regex("""(?:x-tvg-url|url-tvg)=(?:"([^"]*)"|'([^']*)'|([^,\s]+))""", RegexOption.IGNORE_CASE)

    fun parse(inputStream: InputStream, playlistId: String): ParsedM3uResult {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        var detectedEpgUrl: String? = null

        val groupsMap = LinkedHashMap<String, IptvGroup>()
        val channels = ArrayList<IptvChannel>()

        var currentExtInf: String? = null
        val currentHeaders = mutableMapOf<String, String>()

        var line: String? = reader.readLine()

        // Check first line for EXTM3U metadata
        if (line != null && line.startsWith("#EXTM3U", ignoreCase = true)) {
            val match = EXTM3U_EPG_REGEX.find(line)
            detectedEpgUrl = match?.let { it.groupValues[1].ifEmpty { it.groupValues[2].ifEmpty { it.groupValues[3] } } }
                ?.split(",")?.firstOrNull()?.trim()
            line = reader.readLine()
        }

        var channelIndex = 1

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                line = reader.readLine()
                continue
            }

            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                currentExtInf = trimmed
                currentHeaders.clear()
            } else if (trimmed.startsWith("#EXTVLCOPT:", ignoreCase = true)) {
                val opt = trimmed.removePrefix("#EXTVLCOPT:").trim()
                val parts = opt.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().lowercase()
                    val value = parts[1].trim()
                    when (key) {
                        "http-user-agent" -> currentHeaders["User-Agent"] = value
                        "http-referrer", "http-referer" -> currentHeaders["Referer"] = value
                    }
                }
            } else if (trimmed.startsWith("#KODIPROP:", ignoreCase = true)) {
                val prop = trimmed.removePrefix("#KODIPROP:").trim()
                if (prop.startsWith("inputstream.adaptive.manifest_headers=", ignoreCase = true)) {
                    val headersStr = prop.substringAfter("=").trim()
                    headersStr.split("&").forEach { pair ->
                        val p = pair.split("=", limit = 2)
                        if (p.size == 2) {
                            currentHeaders[p[0].trim()] = p[1].trim()
                        }
                    }
                }
            } else if (!trimmed.startsWith("#")) {
                // This is a stream URL line
                val streamUrl = trimmed
                if (currentExtInf != null) {
                    val rawName = if (currentExtInf.lastIndexOf(',') != -1) currentExtInf.substring(currentExtInf.lastIndexOf(',') + 1).trim() else ""
                    val isDecorativeHeader = isHeaderDivider(rawName, streamUrl)
                    if (!isDecorativeHeader) {
                        val channel = parseExtInfLine(
                            extInf = currentExtInf,
                            streamUrl = streamUrl,
                            headers = currentHeaders.toMap(),
                            playlistId = playlistId,
                            defaultChannelNumber = channelIndex,
                            groupsMap = groupsMap
                        )
                        channels.add(channel)
                        channelIndex++
                    }
                    currentExtInf = null
                    currentHeaders.clear()
                }
            }

            line = reader.readLine()
        }

        // Calculate channel count per group
        val groupCounts = channels.groupingBy { it.groupId }.eachCount()
        val finalGroups = groupsMap.values.map { grp ->
            grp.copy(channelCount = groupCounts[grp.id] ?: 0)
        }

        return ParsedM3uResult(
            detectedEpgUrl = detectedEpgUrl,
            groups = finalGroups,
            channels = channels
        )
    }

    private fun parseExtInfLine(
        extInf: String,
        streamUrl: String,
        headers: Map<String, String>,
        playlistId: String,
        defaultChannelNumber: Int,
        groupsMap: LinkedHashMap<String, IptvGroup>
    ): IptvChannel {
        val commaIndex = extInf.lastIndexOf(',')
        val metadataPart = if (commaIndex != -1) extInf.substring(0, commaIndex) else extInf
        val rawName = if (commaIndex != -1) extInf.substring(commaIndex + 1).trim() else "Channel $defaultChannelNumber"

        val attributes = mutableMapOf<String, String>()
        for (match in ATTRIBUTE_REGEX.findAll(metadataPart)) {
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2].ifEmpty {
                match.groupValues[3].ifEmpty {
                    match.groupValues[4]
                }
            }
            attributes[key] = value.trim()
        }

        val tvgId = attributes["tvg-id"]?.takeIf { it.isNotBlank() }
        val tvgName = attributes["tvg-name"]?.takeIf { it.isNotBlank() }
        val tvgLogo = attributes["tvg-logo"]?.takeIf { it.isNotBlank() }
        val tvgChno = attributes["tvg-chno"]?.toIntOrNull() ?: defaultChannelNumber

        val groupTitle = attributes["group-title"]?.takeIf { it.isNotBlank() } ?: "General"
        val groupKey = "${playlistId}_${groupTitle.lowercase()}"
        val group = groupsMap.getOrPut(groupKey) {
            IptvGroup(
                id = groupKey,
                playlistId = playlistId,
                title = groupTitle,
                orderIndex = groupsMap.size
            )
        }

        val streamFormat = when {
            streamUrl.contains(".m3u8", ignoreCase = true) || streamUrl.contains("m3u8", ignoreCase = true) -> IptvStreamFormat.HLS
            streamUrl.contains(".ts", ignoreCase = true) -> IptvStreamFormat.MPEGTS
            streamUrl.contains(".mp4", ignoreCase = true) -> IptvStreamFormat.MP4
            else -> IptvStreamFormat.HLS
        }

        val channelId = "${playlistId}_${tvgId ?: UUID.nameUUIDFromBytes("$streamUrl$rawName".toByteArray()).toString()}"

        return IptvChannel(
            id = channelId,
            playlistId = playlistId,
            groupId = group.id,
            groupTitle = group.title,
            name = rawName.ifBlank { tvgName ?: "Channel $defaultChannelNumber" },
            streamUrl = streamUrl,
            logoUrl = tvgLogo,
            tvgId = tvgId,
            tvgName = tvgName,
            channelNumber = tvgChno,
            headers = headers,
            streamFormat = streamFormat
        )
    }

    private fun isHeaderDivider(name: String, streamUrl: String): Boolean {
        val trimmedName = name.trim()
        val isDecorative = (trimmedName.startsWith("#") && trimmedName.endsWith("#")) ||
                (trimmedName.startsWith("===") && trimmedName.endsWith("===")) ||
                (trimmedName.startsWith("---") && trimmedName.endsWith("---")) ||
                (trimmedName.startsWith("***") && trimmedName.endsWith("***"))
        val isDummyUrl = streamUrl.isBlank() ||
                (streamUrl.endsWith(".mp4", ignoreCase = true) && streamUrl.contains("dummy", ignoreCase = true)) ||
                streamUrl.contains("placeholder", ignoreCase = true) ||
                streamUrl == "http://" || streamUrl == "https://"
        return isDecorative || isDummyUrl
    }
}
