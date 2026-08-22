package com.nuvio.tv.data.iptv.m3u

import com.nuvio.tv.domain.model.iptv.IptvStreamFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class M3uParserTest {

    @Test
    fun parse_validM3uWithHeadersAndAttributes_extractsCorrectMetadata() {
        val m3uContent = """
            #EXTM3U x-tvg-url="http://epg.example.com/epg.xml.gz"
            #EXTINF:-1 tvg-id="cnn.us" tvg-name="CNN HD" tvg-logo="https://logo.com/cnn.png" group-title="News" tvg-chno="101",CNN USA
            #EXTVLCOPT:http-user-agent=CustomAgent/1.0
            #EXTVLCOPT:http-referrer=https://stream.example.com
            http://stream.example.com/live/cnn.m3u8
            #EXTINF:-1 tvg-id="espn.us" tvg-name="ESPN HD" tvg-logo="https://logo.com/espn.png" group-title="Sports" tvg-chno="201",ESPN HD
            http://stream.example.com/live/espn.ts
        """.trimIndent()

        val inputStream = ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8))
        val result = M3uParser.parse(inputStream, playlistId = "playlist_1")

        assertEquals("http://epg.example.com/epg.xml.gz", result.detectedEpgUrl)
        assertEquals(2, result.groups.size)
        assertEquals(2, result.channels.size)

        val cnn = result.channels.first { it.name == "CNN USA" }
        assertEquals("cnn.us", cnn.tvgId)
        assertEquals("CNN HD", cnn.tvgName)
        assertEquals("https://logo.com/cnn.png", cnn.logoUrl)
        assertEquals("News", cnn.groupTitle)
        assertEquals(101, cnn.channelNumber)
        assertEquals("CustomAgent/1.0", cnn.headers["User-Agent"])
        assertEquals("https://stream.example.com", cnn.headers["Referer"])
        assertEquals(IptvStreamFormat.HLS, cnn.streamFormat)

        val espn = result.channels.first { it.name == "ESPN HD" }
        assertEquals("espn.us", espn.tvgId)
        assertEquals(201, espn.channelNumber)
        assertEquals("Sports", espn.groupTitle)
        assertEquals(IptvStreamFormat.MPEGTS, espn.streamFormat)
    }

    @Test
    fun parse_malformedLinesAndMissingAttributes_recoversGracefully() {
        val m3uContent = """
            #EXTM3U
            
            #EXTINF:-1,Sky Sports 1
            http://stream.example.com/skysports
            #EXTINF:-1 invalid-attr
            
            #EXTINF:-1 group-title="Documentary",Discovery Channel
            http://stream.example.com/discovery.mp4
        """.trimIndent()

        val inputStream = ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8))
        val result = M3uParser.parse(inputStream, playlistId = "playlist_2")

        assertEquals(2, result.channels.size)
        assertEquals("Sky Sports 1", result.channels[0].name)
        assertEquals("Discovery Channel", result.channels[1].name)
        assertEquals("Documentary", result.channels[1].groupTitle)
        assertEquals(IptvStreamFormat.MP4, result.channels[1].streamFormat)
    }
}
