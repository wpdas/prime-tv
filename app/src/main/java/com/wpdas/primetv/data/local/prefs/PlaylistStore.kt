package com.wpdas.primetv.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wpdas.primetv.domain.model.PlaylistSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Guarda a lista configurada no DataStore, cifrada pelo [SecureStore]. */
@Singleton
class PlaylistStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStore: SecureStore,
    private val json: Json,
) {
    val source: Flow<PlaylistSource?> = context.appDataStore.data
        .map { prefs ->
            prefs[KEY_PLAYLIST]
                ?.let { secureStore.decrypt(it) }
                ?.let { runCatching { json.decodeFromString(PlaylistSource.serializer(), it) }.getOrNull() }
        }
        .distinctUntilChanged()

    suspend fun save(source: PlaylistSource) {
        val cipher = secureStore.encrypt(json.encodeToString(PlaylistSource.serializer(), source))
        context.appDataStore.edit { it[KEY_PLAYLIST] = cipher }
    }

    suspend fun clear() {
        context.appDataStore.edit { it.remove(KEY_PLAYLIST) }
    }

    private companion object {
        val KEY_PLAYLIST = stringPreferencesKey("playlist_source")
    }
}
