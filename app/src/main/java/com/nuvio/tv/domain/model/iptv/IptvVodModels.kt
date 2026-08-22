package com.nuvio.tv.domain.model.iptv

enum class IptvVodType {
    MOVIE,
    SERIES_EPISODE
}

data class IptvVodItem(
    val id: String,
    val playlistId: String,
    val title: String,
    val normalizedTitle: String,
    val year: Int? = null,
    val type: IptvVodType,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val seriesId: String? = null,
    val seriesName: String? = null,
    val streamUrl: String,
    val containerExtension: String = "mp4",
    val posterUrl: String? = null,
    val rating: Double? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val plot: String? = null,
    val durationSeconds: Long? = null,
    val headers: Map<String, String> = emptyMap()
)

data class IptvVodCategory(
    val id: String,
    val playlistId: String,
    val name: String,
    val type: IptvVodType,
    val itemCount: Int = 0
)
