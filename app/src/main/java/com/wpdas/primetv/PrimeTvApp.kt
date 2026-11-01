package com.wpdas.primetv

import android.app.Application
import com.wpdas.primetv.core.i18n.I18n
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PrimeTvApp : Application() {

    @Inject lateinit var i18n: I18n

    override fun onCreate() {
        super.onCreate()
        // Arquivo pequeno; carregar antes da primeira tela evita mostrar chaves cruas.
        i18n.loadBlocking()
    }
}
