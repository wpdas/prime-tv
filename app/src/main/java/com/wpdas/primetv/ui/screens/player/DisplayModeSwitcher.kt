package com.wpdas.primetv.ui.screens.player

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.Display
import com.wpdas.primetv.BuildConfig
import com.wpdas.primetv.core.util.DisplayMode
import com.wpdas.primetv.core.util.DisplayModePicker

/**
 * Pede ao sistema o modo de saída (HDMI) com a resolução do vídeo, para a TV fazer o upscaling no
 * lugar da box. Usa `preferredDisplayModeId` da janela: sem root, e a box só troca se expuser o
 * modo. [video] null volta ao modo padrão do aparelho.
 */
object DisplayModeSwitcher {
    private const val TAG = "DisplayMode"

    fun apply(activity: Activity, video: Pair<Int, Int>?) {
        val display = currentDisplay(activity) ?: return
        val current = display.mode.toModel()
        val modes = display.supportedModes.map { it.toModel() }
        val target = video?.let { (w, h) -> DisplayModePicker.pick(modes, current, w, h) }
        val id = target?.id ?: 0
        val attrs = activity.window.attributes
        if (attrs.preferredDisplayModeId == id) return
        attrs.preferredDisplayModeId = id
        activity.window.attributes = attrs
        if (BuildConfig.DEBUG) Log.d(TAG, "modo de saída: ${target?.let { "${it.width}x${it.height}@${it.refreshRate}" } ?: "padrão"} (vídeo ${video?.first}x${video?.second}, ${modes.size} modos)")
    }

    @Suppress("DEPRECATION")
    private fun currentDisplay(activity: Activity): Display? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.display else activity.windowManager.defaultDisplay
    }.getOrNull()

    private fun Display.Mode.toModel() = DisplayMode(modeId, physicalWidth, physicalHeight, refreshRate)
}
