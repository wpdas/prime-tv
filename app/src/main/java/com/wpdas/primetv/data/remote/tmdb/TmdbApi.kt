package com.wpdas.primetv.data.remote.tmdb

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** API v3 do TMDB. A chave entra por interceptor; o idioma padrão é pt-BR. */
interface TmdbApi {
    @GET("trending/all/week")
    suspend fun trending(@Query("language") language: String = LANG): TmdbPage

    @GET("movie/popular")
    suspend fun popularMovies(@Query("language") language: String = LANG, @Query("region") region: String = REGION): TmdbPage

    @GET("movie/now_playing")
    suspend fun nowPlaying(@Query("language") language: String = LANG, @Query("region") region: String = REGION): TmdbPage

    @GET("tv/popular")
    suspend fun popularTv(@Query("language") language: String = LANG): TmdbPage

    /** Populares por gênero/país (abas Kids e Anime). Gêneros: "16" animação, "10762" kids, "10751" família; "|" = ou. */
    @GET("discover/tv")
    suspend fun discoverTv(
        @Query("with_genres") genres: String,
        @Query("with_origin_country") country: String? = null,
        @Query("sort_by") sort: String = "popularity.desc",
        @Query("vote_count.gte") minVotes: Int = 50,
        @Query("language") language: String = LANG,
    ): TmdbPage

    @GET("discover/movie")
    suspend fun discoverMovie(
        @Query("with_genres") genres: String,
        @Query("with_origin_country") country: String? = null,
        @Query("sort_by") sort: String = "popularity.desc",
        @Query("vote_count.gte") minVotes: Int = 50,
        @Query("language") language: String = LANG,
    ): TmdbPage

    @GET("search/movie")
    suspend fun searchMovie(@Query("query") query: String, @Query("year") year: Int? = null, @Query("language") language: String = LANG): TmdbPage

    @GET("search/tv")
    suspend fun searchTv(@Query("query") query: String, @Query("first_air_date_year") year: Int? = null, @Query("language") language: String = LANG): TmdbPage

    @GET("movie/{id}")
    suspend fun movie(@Path("id") id: Int, @Query("append_to_response") append: String = "videos", @Query("language") language: String = LANG): TmdbDetails

    @GET("tv/{id}")
    suspend fun tv(@Path("id") id: Int, @Query("append_to_response") append: String = "videos", @Query("language") language: String = LANG): TmdbDetails

    @GET("genre/movie/list")
    suspend fun movieGenres(@Query("language") language: String = LANG): TmdbGenreList

    @GET("genre/tv/list")
    suspend fun tvGenres(@Query("language") language: String = LANG): TmdbGenreList

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val LANG = "pt-BR"
        const val REGION = "BR"
        const val IMAGE_BASE = "https://image.tmdb.org/t/p/"
        fun backdrop(path: String?) = path?.let { "${IMAGE_BASE}w1280$it" }
        fun poster(path: String?) = path?.let { "${IMAGE_BASE}w342$it" }
    }
}
