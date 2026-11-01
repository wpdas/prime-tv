package com.wpdas.primetv.data.remote.tmdb

import com.wpdas.primetv.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class TmdbApiKey

@Module
@InstallIn(SingletonComponent::class)
object TmdbModule {

    @Provides @TmdbApiKey
    fun provideApiKey(): String = BuildConfig.TMDB_API_KEY

    @Provides @Singleton
    fun provideTmdbApi(http: OkHttpClient, json: Json, @TmdbApiKey apiKey: String): TmdbApi {
        val client = http.newBuilder()
            .addInterceptor { chain ->
                val url = chain.request().url.newBuilder().addQueryParameter("api_key", apiKey).build()
                chain.proceed(chain.request().newBuilder().url(url).build())
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(TmdbApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TmdbApi::class.java)
    }
}
