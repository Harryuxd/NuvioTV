package com.nuvio.tv.data.iptv.logo

import android.content.Context
import io.mockk.mockk
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

class IptvLogoResolverTest {

    private val context: Context = mockk(relaxed = true)
    private val okHttpClient: OkHttpClient = mockk(relaxed = true)
    private val resolver = IptvLogoResolver(context, okHttpClient)

    @Test
    fun `test unicode superscript and quality marker normalization`() {
        val raw1 = "NOW: SKY SPORTS MAIN EVENT ᴿᴬᵂ"
        val raw2 = "NOW: SKY SPORTS MAIN EVENT ᵁᴴᴰ ³"
        val raw3 = "NOW: SKY SPORTS MAIN EVENT ᴴᴰ"
        val raw4 = "UK: Sky Sports Main Event (1080p) [RAW]"
        val raw5 = "Sky Sports Main Event"

        val norm1 = resolver.normalize(raw1)
        val norm2 = resolver.normalize(raw2)
        val norm3 = resolver.normalize(raw3)
        val norm4 = resolver.normalize(raw4)
        val norm5 = resolver.normalize(raw5)

        assertEquals("sky sports main event", norm1)
        assertEquals("sky sports main event", norm2)
        assertEquals("sky sports main event", norm3)
        assertEquals("sky sports main event", norm4)
        assertEquals("sky sports main event", norm5)
    }

    @Test
    fun `test prefix stripping and quality tokens`() {
        assertEquals("bbc one", resolver.normalize("UK: BBC ONE HD"))
        assertEquals("hbo", resolver.normalize("US | HBO EAST (BACKUP)"))
        assertEquals("supersport premier league", resolver.normalize("DSTV: SUPERSPORT PREMIER LEAGUE FHD"))
        assertEquals("sky sports main event", resolver.normalize("4K: SKY SPORTS MAIN EVENT 4K| UHD 3840P"))
        assertEquals("sky sports main event", resolver.normalize("NOW: SKY SPORTS MAIN EVENT UHD ³⁸"))
        assertEquals("sky sports main event", resolver.normalize("NOW: SKY SPORTS MAIN EVENT HD/RAW"))
        assertEquals("sky sports main event", resolver.normalize("UK: SKY SPORTS MAIN EVENT"))
    }
}
