package com.wpdas.primetv.core.di

import android.content.Context
import com.wpdas.primetv.core.i18n.AssetsI18nSource
import com.wpdas.primetv.core.i18n.I18n
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides @Singleton
    fun provideI18n(@ApplicationContext context: Context, json: Json): I18n =
        I18n(AssetsI18nSource(context), json)

    @Provides @IoDispatcher fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    @Provides @DefaultDispatcher fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    @Provides @MainDispatcher fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
