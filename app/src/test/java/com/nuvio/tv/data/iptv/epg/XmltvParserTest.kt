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

        val dateNoTz = "20260821120000"
        val timestampNoTz = XmltvParser.parseXmltvDate(dateNoTz)
        assertNotNull(timestampNoTz)
        assertEquals(timestamp, timestampNoTz)
    }

    @Test
    fun parse_validXmltv_extractsProgramsWithMetadata() {
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

        assertEquals(2, programs.size)

        val prog1 = programs[0]
        assertEquals("cnn.us", prog1.channelTvgId)
        assertEquals("CNN News Central", prog1.title)
        assertEquals("Live rolling news coverage from around the world.", prog1.description)
        assertEquals("News", prog1.category)
        assertEquals("https://img.com/cnn_news.png", prog1.posterUrl)
        assertTrue(prog1.endEpochMs > prog1.startEpochMs)

        val prog2 = programs[1]
        assertEquals("cnn.us", prog2.channelTvgId)
        assertEquals("Inside Politics", prog2.title)
    }
}
