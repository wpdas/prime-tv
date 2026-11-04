package com.wpdas.primetv.data.link

import com.wpdas.primetv.BuildConfig
import com.wpdas.primetv.domain.link.LinkReceiver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LinkModule {

    /** Respostas minúsculas: prazos curtos para a tela avisar logo quando a TV está sem internet. */
    @Provides @Singleton
    fun provideLinkApi(http: OkHttpClient, json: Json): LinkApi = Retrofit.Builder()
        .baseUrl(LinkSites.origin(BuildConfig.LINK_SITE_URL))
        .client(http.newBuilder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(LinkApi::class.java)

    /**
     * O site real é o padrão. O fake só entra em build debug com `FAKE_LINK_RECEIVER=true` em
     * `local.properties` (ou no ambiente): aí "Receber link" entrega `IPTV_TEST_URL` depois de
     * alguns segundos, sem rede e sem gastar sessão no servidor.
     */
    @Provides @Singleton
    fun provideLinkReceiver(http: HttpLinkReceiver, fake: FakeLinkReceiver): LinkReceiver =
        if (BuildConfig.DEBUG && BuildConfig.FAKE_LINK_RECEIVER) fake else http
}
