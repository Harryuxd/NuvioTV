package com.nuvio.tv.data.iptv.xtream

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nuvio.tv.domain.model.iptv.IptvStreamFormat
import com.nuvio.tv.domain.model.iptv.XtreamCredentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class XtreamClientTest {

    private val gson = Gson()

    @Test
    fun parse_xtreamAuthResponse_extractsUserInfo() {
        val json = """
            {
                "user_info": {
                    "username": "user123",
                    "password": "pass",
                    "status": "Active",
                    "exp_date": "1750000000",
                    "is_trial": "0",
                    "active_cons": "1",
                    "max_connections": "2",
                    "auth": 1
                },
                "server_info": {
                    "url": "http://stream.example.com",
                    "port": "8080",
                    "server_protocol": "http"
                }
            }
        """.trimIndent()

        val response = gson.fromJson(json, XtreamAuthResponse::class.java)
        assertNotNull(response)
        assertNotNull(response.userInfo)
        assertEquals("user123", response.userInfo?.username)
        assertEquals("Active", response.userInfo?.status)
        assertEquals(1, response.userInfo?.auth)
        assertEquals("8080", response.serverInfo?.port)
    }

    @Test
    fun parse_xtreamStreams_mapsToCategoriesAndStreamUrls() {
        val categoriesJson = """
            [
                {"category_id": "1", "category_name": "Sports"},
                {"category_id": "2", "category_name": "News"}
            ]
        """.trimIndent()

        val streamsJson = """
            [
                {
                    "num": 1,
                    "name": "Sky Sports Main Event",
                    "stream_type": "live",
                    "stream_id": 1001,
                    "stream_icon": "http://logo.com/sky.png",
                    "epg_channel_id": "sky.sports.main",
                    "category_id": "1"
                },
                {
                    "num": 2,
                    "name": "BBC News HD",
                    "stream_type": "live",
                    "stream_id": 1002,
                    "stream_icon": "http://logo.com/bbc.png",
                    "epg_channel_id": "bbc.news.hd",
                    "category_id": "2"
                }
            ]
        """.trimIndent()

        val catType = object : TypeToken<List<XtreamCategory>>() {}.type
        val categories: List<XtreamCategory> = gson.fromJson(categoriesJson, catType)
        assertEquals(2, categories.size)

        val streamType = object : TypeToken<List<XtreamStreamItem>>() {}.type
        val streams: List<XtreamStreamItem> = gson.fromJson(streamsJson, streamType)
        assertEquals(2, streams.size)

        val creds = XtreamCredentials("http://example.com:8080", "user", "pass")
        val stream1 = streams[0]
        val expectedStreamUrl = "${creds.serverUrl}/live/${creds.username}/${creds.password}/${stream1.streamId}.ts"

        assertEquals("http://example.com:8080/live/user/pass/1001.ts", expectedStreamUrl)
        assertEquals("Sky Sports Main Event", stream1.name)
        assertEquals("sky.sports.main", stream1.epgChannelId)
    }
}
