package com.wpdas.primetv.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Único DataStore de preferências do app (lista cifrada e configurações). */
internal val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "primetv_prefs")
