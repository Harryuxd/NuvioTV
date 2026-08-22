package com.nuvio.tv.data.iptv.logo

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.nuvio.tv.domain.model.iptv.IptvChannel
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.text.Normalizer
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves high-quality TV channel artwork using multiple curated sources:
 * 1. TV-Logo Repository (10,000+ dark-background, transparent TV logos via jsDelivr CDN)
 * 2. IPTV-ORG Logo Catalog (PNG/SVG vector & high-res artwork)
 * 3. Provider Artwork Fallback (preserves original M3U logo if curated logo is missing)
 */
@Singleton
class IptvLogoResolver @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()
    private val iptvOrgFile get() = File(context.filesDir, "iptv-org-logos.json")
    private val tvLogosFile get() = File(context.filesDir, "tv-logos-tree.json")

    private var iptvOrgCatalog: List<LogoEntry>? = null
    private var tvLogosCatalog: Map<String, String>? = null

    fun resolve(
        channels: List<IptvChannel>,
        priorityOrder: List<String> = listOf("TV_LOGOS", "IPTV_ORG", "PROVIDER"),
        useProviderFallback: Boolean = true
    ): List<IptvChannel> {
        val iptvEntries = loadIptvOrgCatalog().orEmpty().filter(::isUsable)
        val tvLogosMap = loadTvLogosCatalog().orEmpty()

        val iptvByChannelId = iptvEntries.groupBy { it.channel.lowercase() }
        val iptvByChannelBaseName = iptvEntries.groupBy { it.channel.substringBeforeLast('.').lowercase() }
        val iptvByNormalizedName = iptvEntries.groupBy { normalize(it.channel.substringBeforeLast('.')) }

        val activePriorities = if (priorityOrder.isEmpty()) listOf("TV_LOGOS", "IPTV_ORG", "PROVIDER") else priorityOrder

        return channels.map { channel ->
            val rawTvgId = channel.tvgId?.trim()?.lowercase()
            val tvgBase = rawTvgId?.substringBeforeLast('.')
            val nameNormalized = normalize(channel.tvgName ?: channel.name)
            val tvgNormalized = rawTvgId?.let { normalize(it.substringBeforeLast('.')) }

            var resolvedLogo: String? = null

            for (source in activePriorities) {
                if (resolvedLogo != null) break

                when (source.uppercase()) {
                    "TV_LOGOS" -> {
                        resolvedLogo = (if (!rawTvgId.isNullOrBlank()) tvLogosMap[rawTvgId] else null)
                            ?: (if (!tvgBase.isNullOrBlank()) tvLogosMap[tvgBase] else null)
                            ?: (if (nameNormalized.isNotBlank()) tvLogosMap[nameNormalized] else null)
                            ?: (if (!tvgNormalized.isNullOrBlank()) tvLogosMap[tvgNormalized] else null)
                            ?: (if (nameNormalized.isNotBlank()) findFuzzyTvLogo(nameNormalized, tvLogosMap) else null)
                    }
                    "IPTV_ORG" -> {
                        val exactIptv = rawTvgId?.let { iptvByChannelId[it] }?.maxByOrNull { it.score }
                        val baseIptv = (if (exactIptv == null && !tvgBase.isNullOrBlank()) iptvByChannelBaseName[tvgBase] else null)?.maxByOrNull { it.score }
                        val nameIptv = (if (exactIptv == null && baseIptv == null && nameNormalized.isNotBlank()) iptvByNormalizedName[nameNormalized] else null)?.maxByOrNull { it.score }
                        val tvgIptv = (if (exactIptv == null && baseIptv == null && nameIptv == null && !tvgNormalized.isNullOrBlank()) iptvByNormalizedName[tvgNormalized] else null)?.maxByOrNull { it.score }
                        val fuzzyIptv = if (exactIptv == null && baseIptv == null && nameIptv == null && tvgIptv == null && nameNormalized.isNotBlank()) {
                            findFuzzyIptv(nameNormalized, iptvByNormalizedName)
                        } else null

                        resolvedLogo = (exactIptv ?: baseIptv ?: nameIptv ?: tvgIptv ?: fuzzyIptv)?.url
                    }
                    "PROVIDER" -> {
                        resolvedLogo = channel.providerLogoUrl?.takeIf { it.isNotBlank() }
                            ?: channel.logoUrl?.takeIf { it.isNotBlank() }
                    }
                }
            }

            if (resolvedLogo == null && useProviderFallback) {
                resolvedLogo = channel.providerLogoUrl ?: channel.logoUrl?.takeIf { it.isNotBlank() }
            }

            channel.copy(
                providerLogoUrl = channel.providerLogoUrl ?: channel.logoUrl,
                logoUrl = resolvedLogo
            )
        }
    }

    private fun findFuzzyTvLogo(query: String, map: Map<String, String>): String? {
        if (query.length < 3) return null
        return map.entries
            .filter { (key, _) -> key.length >= 3 && (query.startsWith(key) || key.startsWith(query)) }
            .maxByOrNull { it.key.length }
            ?.value
    }

    private fun findFuzzyIptv(query: String, map: Map<String, List<LogoEntry>>): LogoEntry? {
        if (query.length < 3) return null
        return map.entries
            .filter { (key, _) -> key.length >= 3 && (query.startsWith(key) || key.startsWith(query)) }
            .maxByOrNull { it.key.length }
            ?.value
            ?.maxByOrNull { it.score }
    }

    @Synchronized private fun loadIptvOrgCatalog(): List<LogoEntry>? {
        iptvOrgCatalog?.let { return it }
        val fresh = iptvOrgFile.exists() && System.currentTimeMillis() - iptvOrgFile.lastModified() < TTL_MS
        val raw = if (fresh) iptvOrgFile.readText() else downloadUrl(IPTV_ORG_URL, iptvOrgFile) ?: iptvOrgFile.takeIf(File::exists)?.readText()
        return raw?.let { gson.fromJson<List<LogoEntry>>(it, object : TypeToken<List<LogoEntry>>() {}.type) }?.also { iptvOrgCatalog = it }
    }

    @Synchronized private fun loadTvLogosCatalog(): Map<String, String>? {
        tvLogosCatalog?.let { return it }
        val fresh = tvLogosFile.exists() && System.currentTimeMillis() - tvLogosFile.lastModified() < TTL_MS
        val raw = if (fresh) tvLogosFile.readText() else downloadUrl(TV_LOGOS_TREE_URL, tvLogosFile) ?: tvLogosFile.takeIf(File::exists)?.readText()
        val treeResponse = raw?.let {
            runCatching { gson.fromJson(it, GitHubTreeResponse::class.java) }.getOrNull()
        } ?: return null

        val mapped = mutableMapOf<String, String>()
        for (item in treeResponse.tree) {
            if (item.type == "blob" && item.path.endsWith(".png", ignoreCase = true) && item.path.startsWith("countries/")) {
                val fileName = item.path.substringAfterLast('/').substringBeforeLast('.')
                val normalizedKey = normalize(fileName.replace('-', ' '))
                if (normalizedKey.isNotBlank() && !mapped.containsKey(normalizedKey)) {
                    mapped[normalizedKey] = "$TV_LOGOS_CDN_BASE${item.path}"
                }
            }
        }
        return mapped.also { tvLogosCatalog = it }
    }

    private fun downloadUrl(url: String, targetFile: File): String? = runCatching {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.string()?.also { targetFile.writeText(it) }
        }
    }.getOrNull()

    private fun isUsable(entry: LogoEntry): Boolean =
        entry.url.isNotBlank() &&
        (entry.url.startsWith("https://", ignoreCase = true) || entry.url.startsWith("http://", ignoreCase = true))

    fun normalize(raw: String): String {
        if (raw.isBlank()) return ""
        // 1. NFKD decomposition converts unicode superscript/small caps (e.g. ᴿᴬᵂ, ᵁᴴᴰ, ᴴᴰ, ³, ², ³⁸) to ascii (RAW, UHD, HD, 3, 2, 38)
        val decomposed = Normalizer.normalize(raw, Normalizer.Form.NFKD)
            .replace(Regex("""\p{M}"""), "")
            .lowercase()

        return decomposed
            // 2. Strip prefix tags (e.g. "NOW:", "VIP:", "UK:", "US -", "[US]", "(UK)", "24/7:", "4K:", "4K|", "UHD:")
            .replace(Regex("""^(\[[^\]]*\]|\([^)]*\)|[a-z0-9+/&.-]{1,15}\s*[:|/|-]\s*)"""), " ")
            // 3. Strip parentheses, brackets, braces content
            .replace(Regex("""\([^)]*\)"""), " ")
            .replace(Regex("""\[[^\]]*\]"""), " ")
            .replace(Regex("""\{[^}]*\}"""), " ")
            // 4. Strip technical/quality words, resolutions, stream formats, and feed noise
            .replace(Regex("""\b(uhd|fhd|hd|sd|4k|8k|hevc|h264|h265|2160p|3840p|4320p|1080p|1080i|720p|576p|480p|50fps|60fps|fps|raw|backup|back|alt|live|feed|stream|vip|now|direct|east|west|central|pacific|mountain|dolby|atmos|hdr|hdr10|sdr)\b"""), " ")
            // 5. Strip isolated numbers anywhere (e.g. "38", "3", "2", "1" from feed numbers or stray tokens)
            .replace(Regex("""\b\d+\b"""), " ")
            // 6. Strip country codes when isolated
            .replace(Regex("""\b(us|uk|ca|au|nz|ie|de|fr|es|it|nl|pt|br|mx|ar|in)\b"""), " ")
            // 7. Non-alphanumeric to spaces and collapse
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .trim()
            .replace(Regex("""\s+"""), " ")
    }

    private data class LogoEntry(
        val channel: String,
        val url: String,
        val width: Int? = null,
        val height: Int? = null,
        val format: String? = null,
        @field:SerializedName("in_use") val inUse: Boolean = false,
    ) {
        val area get() = (width ?: 0) * (height ?: 0)
        val score: Int
            get() {
                var s = 0
                if (inUse) s += 10_000
                when (format?.uppercase()) {
                    "SVG" -> s += 5_000
                    "PNG" -> s += 3_000
                    "WEBP" -> s += 2_000
                    "JPG", "JPEG" -> s += 1_000
                }
                if (url.startsWith("https://", ignoreCase = true)) s += 500
                s += area.coerceAtMost(5_000)
                return s
            }
    }

    private data class GitHubTreeResponse(
        val tree: List<GitHubTreeItem> = emptyList()
    )

    private data class GitHubTreeItem(
        val path: String,
        val type: String
    )

    private companion object {
        const val IPTV_ORG_URL = "https://iptv-org.github.io/api/logos.json"
        const val TV_LOGOS_TREE_URL = "https://api.github.com/repos/tv-logo/tv-logos/git/trees/main?recursive=1"
        const val TV_LOGOS_CDN_BASE = "https://cdn.jsdelivr.net/gh/tv-logo/tv-logos@main/"
        val TTL_MS = TimeUnit.DAYS.toMillis(7)
    }
}
