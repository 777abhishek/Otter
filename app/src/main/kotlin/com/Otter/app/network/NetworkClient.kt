package com.Otter.app.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

object NetworkClient {
    private const val BASE_URL = "https://www.googleapis.com/youtube/v3/"

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    private val okHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    private val retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
}

interface YouTubeApiService {
    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 20,
        @Query("key") apiKey: String,
    ): retrofit2.Response<SearchResponse>

    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "snippet,contentDetails,statistics",
        @Query("id") id: String,
        @Query("key") apiKey: String,
    ): retrofit2.Response<VideoDetailsResponse>
}

data class SearchResponse(
    val items: List<SearchItem>?,
)

data class SearchItem(
    val id: SearchId?,
    val snippet: Snippet?,
)

data class SearchId(
    val videoId: String?,
)

data class Snippet(
    val title: String?,
    val description: String?,
    val thumbnails: Thumbnails?,
    val channelTitle: String?,
    val channelId: String?,
    val publishedAt: String?,
)

data class Thumbnails(
    val default: Thumbnail?,
    val medium: Thumbnail?,
    val high: Thumbnail?,
)

data class Thumbnail(
    val url: String?,
    val width: Int?,
    val height: Int?,
)

data class VideoDetailsResponse(
    val items: List<VideoDetails>?,
)

data class VideoDetails(
    val id: String?,
    val snippet: Snippet?,
    val contentDetails: ContentDetails?,
    val statistics: Statistics?,
)

data class ContentDetails(
    val duration: String?,
)

data class Statistics(
    val viewCount: String?,
)
