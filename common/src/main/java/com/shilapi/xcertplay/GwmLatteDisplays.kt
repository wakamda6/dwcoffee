package com.shilapi.xcertplay

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

/** Display topology measured on the 2022 GWM Latte DHT head unit. */
internal object GwmLatteDisplays {
    private const val CLUSTER_ID = 3
    private const val HUD_ID = 4

    fun cluster(context: Context): Display? = target(context, CLUSTER_ID, 1920, 384)

    fun hud(context: Context): Display? = target(context, HUD_ID, 800, 480)

    private fun target(context: Context, id: Int, width: Int, height: Int): Display? {
        val manager = context.getSystemService(DisplayManager::class.java) ?: return null
        val available = manager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        return available.firstOrNull { it.displayId == id && matches(it, width, height) }
    }

    private fun matches(display: Display, width: Int, height: Int): Boolean {
        if (!display.isValid) return false
        val size = ClusterMapPresentation.sizeOf(display)
        return size.x == width && size.y == height
    }
}
