package com.wpdas.primetv.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Onde o usuário estava na tela Ao vivo (categoria e canal), para reabrir no mesmo lugar. */
@Singleton
class LiveStateStore @Inject constructor(@ApplicationContext private val context: Context) {
    data class LivePosition(val railId: String, val channelId: String?)

    suspend fun read(): LivePosition? {
        val prefs = runCatching { context.appDataStore.data.first() }.getOrNull() ?: return null
        val rail = prefs[KEY_RAIL] ?: return null
        return LivePosition(rail, prefs[KEY_CHANNEL])
    }

    suspend fun save(railId: String, channelId: String?) {
        context.appDataStore.edit {
            it[KEY_RAIL] = railId
            if (channelId != null) it[KEY_CHANNEL] = channelId else it.remove(KEY_CHANNEL)
        }
    }

    private companion object {
        val KEY_RAIL = stringPreferencesKey("live_last_rail")
        val KEY_CHANNEL = stringPreferencesKey("live_last_channel")
    }
}
