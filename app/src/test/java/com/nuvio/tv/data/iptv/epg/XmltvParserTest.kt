package com.nuvio.tv.data.iptv.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class XmltvParserTest {

    @Test
    fun parseXmltvDate_parsesTimezoneAndUtcDates() {
        val dateWithTz = "20260821120000 +0000"
        val timestamp = XmltvParser.parseXmltvDate(dateWithTz)
        assertNotNull(timestamp)

        val dateWithColonTz = "20260821120000 +00:00"
        val timestampColon = XmltvParser.parseXmltvDate(dateWithColonTz)
        assertNotNull(timestampColon)
        assertEquals(timestamp, timestampColon)

        val dateNoTz = "20260821120000"
        val timestampNoTz = XmltvParser.parseXmltvDate(dateNoTz)
        assertNotNull(timestampNoTz)
        assertEquals(timestamp, timestampNoTz)
    }

    @Test
    fun parse_validXmltv_extractsProgramsWithMetadataAndAliases() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
            <tv>
                <channel id="cnn.us">
                    <display-name>CNN USA</display-name>
                </channel>
                <programme start="20260821120000 +0000" stop="20260821130000 +0000" channel="cnn.us">
                    <title lang="en">CNN News Central</title>
                    <desc lang="en">Live rolling news coverage from around the world.</desc>
                    <category>News</category>
                    <icon src="https://img.com/cnn_news.png"/>
                </programme>
                <programme start="20260821130000 +0000" stop="20260821140000 +0000" channel="cnn.us">
                    <title lang="en">Inside Politics</title>
                    <desc lang="en">Political insights and panel debates.</desc>
                    <category>Politics</category>
                </programme>
            </tv>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8))
        val programs = XmltvParser.parse(inputStream, playlistId = "pl_1")

        assertTrue(programs.isNotEmpty())

        val prog1 = programs.first { it.channelTvgId == "cnn.us" && it.title == "CNN News Central" }
        assertEquals("CNN News Central", prog1.title)
        assertEquals("Live rolling news coverage from around the world.", prog1.description)
        assertEquals("News", prog1.category)
        assertEquals("https://img.com/cnn_news.png", prog1.posterUrl)
        assertTrue(prog1.endEpochMs > prog1.startEpochMs)

        // Verify normalized display name alias was created
        assertTrue(programs.any { it.channelTvgId == "cnn" || it.channelTvgId == "cnn usa" })
    }

    @Test
    fun parse_withTargetKeys_matchesByDisplayNameOrNormalizedKey() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
            <tv>
                <channel id="10101">
                    <display-name>Sky Sports Main Event</display-name>
                </channel>
                <programme start="20260821120000 +0000" stop="20260821140000 +0000" channel="10101">
                    <title>Premier League Live</title>
                </programme>
                <programme start="20260821120000 +0000" stop="20260821140000 +0000" channel="unrelated.channel">
                    <title>Unrelated Show</title>
                </programme>
            </tv>
        """.trimIndent()

        val targetKeys = setOf("sky sports main event")
        val inputStream = ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8))
        val programs = XmltvParser.parse(inputStream, playlistId = "pl_1", targetChannelKeys = targetKeys)

        assertTrue(programs.any { it.title == "Premier League Live" })
        assertTrue(programs.none { it.title == "Unrelated Show" })
    }
}
