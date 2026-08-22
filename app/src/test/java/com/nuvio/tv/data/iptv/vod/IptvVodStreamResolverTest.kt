package com.nuvio.tv.data.iptv.vod

import com.nuvio.tv.data.iptv.xtream.XtreamClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IptvVodStreamResolverTest {

    @Test
    fun parseTitleAndYear_extractsCleanTitleAndYear() {
        val (title1, year1) = XtreamClient.parseTitleAndYear("Inception (2010)")
        assertEquals("Inception", title1)
        assertEquals(2010, year1)

        val (title2, year2) = XtreamClient.parseTitleAndYear("Dune Part Two 2024 1080p HEVC")
        assertEquals("Dune Part Two", title2)
        assertEquals(2024, year2)

        val (title3, year3) = XtreamClient.parseTitleAndYear("Avatar: The Way of Water [2022] 4K")
        assertEquals("Avatar: The Way of Water", title3)
        assertEquals(2022, year3)

        val (title4, year4) = XtreamClient.parseTitleAndYear("The Dark Knight")
        assertEquals("The Dark Knight", title4)
        assertNull(year4)
    }
}
