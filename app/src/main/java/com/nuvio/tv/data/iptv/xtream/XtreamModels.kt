package com.nuvio.tv.data.iptv.xtream

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class XtreamAuthResponse(
    @SerializedName("user_info") @Json(name = "user_info") val userInfo: XtreamUserInfo? = null,
    @SerializedName("server_info") @Json(name = "server_info") val serverInfo: XtreamServerInfo? = null
)

@JsonClass(generateAdapter = true)
data class XtreamUserInfo(
    @SerializedName("username") @Json(name = "username") val username: String? = null,
    @SerializedName("password") @Json(name = "password") val password: String? = null,
    @SerializedName("status") @Json(name = "status") val status: String? = null,
    @SerializedName("exp_date") @Json(name = "exp_date") val expDate: String? = null,
    @SerializedName("is_trial") @Json(name = "is_trial") val isTrial: String? = null,
    @SerializedName("active_cons") @Json(name = "active_cons") val activeCons: String? = null,
    @SerializedName("max_connections") @Json(name = "max_connections") val maxConnections: String? = null,
    @SerializedName("auth") @Json(name = "auth") val auth: Int? = null,
    @SerializedName("allowed_output_formats") @Json(name = "allowed_output_formats") val allowedOutputFormats: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class XtreamServerInfo(
    @SerializedName("url") @Json(name = "url") val url: String? = null,
    @SerializedName("port") @Json(name = "port") val port: String? = null,
    @SerializedName("https_port") @Json(name = "https_port") val httpsPort: String? = null,
    @SerializedName("server_protocol") @Json(name = "server_protocol") val serverProtocol: String? = null,
    @SerializedName("timezone") @Json(name = "timezone") val timezone: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamCategory(
    @SerializedName("category_id") @Json(name = "category_id") val categoryId: String,
    @SerializedName("category_name") @Json(name = "category_name") val categoryName: String,
    @SerializedName("parent_id") @Json(name = "parent_id") val parentId: Int? = null
)

@JsonClass(generateAdapter = true)
data class XtreamStreamItem(
    @SerializedName("num") @Json(name = "num") val num: Any? = null,
    @SerializedName("name") @Json(name = "name") val name: String,
    @SerializedName("stream_type") @Json(name = "stream_type") val streamType: String? = null,
    @SerializedName("stream_id") @Json(name = "stream_id") val streamId: Int,
    @SerializedName("stream_icon") @Json(name = "stream_icon") val streamIcon: String? = null,
    @SerializedName("epg_channel_id") @Json(name = "epg_channel_id") val epgChannelId: String? = null,
    @SerializedName("added") @Json(name = "added") val added: String? = null,
    @SerializedName("category_id") @Json(name = "category_id") val categoryId: String? = null,
    @SerializedName("custom_sid") @Json(name = "custom_sid") val customSid: String? = null,
    @SerializedName("direct_source") @Json(name = "direct_source") val directSource: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamVodItem(
    @SerializedName("num") @Json(name = "num") val num: Any? = null,
    @SerializedName("name") @Json(name = "name") val name: String,
    @SerializedName("stream_type") @Json(name = "stream_type") val streamType: String? = null,
    @SerializedName("stream_id") @Json(name = "stream_id") val streamId: Int,
    @SerializedName("stream_icon") @Json(name = "stream_icon") val streamIcon: String? = null,
    @SerializedName("rating") @Json(name = "rating") val rating: Any? = null,
    @SerializedName("rating_5based") @Json(name = "rating_5based") val rating5Based: Any? = null,
    @SerializedName("added") @Json(name = "added") val added: String? = null,
    @SerializedName("category_id") @Json(name = "category_id") val categoryId: String? = null,
    @SerializedName("container_extension") @Json(name = "container_extension") val containerExtension: String? = null,
    @SerializedName("custom_sid") @Json(name = "custom_sid") val customSid: String? = null,
    @SerializedName("direct_source") @Json(name = "direct_source") val directSource: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamSeriesItem(
    @SerializedName("num") @Json(name = "num") val num: Any? = null,
    @SerializedName("name") @Json(name = "name") val name: String,
    @SerializedName("series_id") @Json(name = "series_id") val seriesId: Int,
    @SerializedName("cover") @Json(name = "cover") val cover: String? = null,
    @SerializedName("plot") @Json(name = "plot") val plot: String? = null,
    @SerializedName("cast") @Json(name = "cast") val cast: String? = null,
    @SerializedName("director") @Json(name = "director") val director: String? = null,
    @SerializedName("genre") @Json(name = "genre") val genre: String? = null,
    @SerializedName("releaseDate") @Json(name = "releaseDate") val releaseDate: String? = null,
    @SerializedName("rating") @Json(name = "rating") val rating: Any? = null,
    @SerializedName("rating_5based") @Json(name = "rating_5based") val rating5Based: Any? = null,
    @SerializedName("episode_run_time") @Json(name = "episode_run_time") val episodeRunTime: Any? = null,
    @SerializedName("category_id") @Json(name = "category_id") val categoryId: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamEpisodeItem(
    @SerializedName("id") @Json(name = "id") val id: String,
    @SerializedName("episode_num") @Json(name = "episode_num") val episodeNum: Any? = null,
    @SerializedName("title") @Json(name = "title") val title: String? = null,
    @SerializedName("container_extension") @Json(name = "container_extension") val containerExtension: String? = null,
    @SerializedName("season") @Json(name = "season") val season: Any? = null,
    @SerializedName("rating") @Json(name = "rating") val rating: Any? = null
)
