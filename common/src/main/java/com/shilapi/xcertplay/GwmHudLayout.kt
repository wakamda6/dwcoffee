package com.shilapi.xcertplay

import android.content.Context
import android.graphics.Color

/** Editable HUD element positions are percentages of the secondary display. */
internal object GwmHudLayout {
    enum class Part(val label: String, val x: Int, val y: Int, val width: Int, val size: Int) {
        ICON("转向图标", 3, 8, 14, 65),
        ROAD("下一道路", 19, 7, 37, 36),
        TURN("转向距离", 19, 25, 36, 50),
        ROUTE("路线概要", 19, 47, 38, 25),
        LANES("车道指引", 4, 71, 53, 32),
        CALL("通话", 57, 69, 39, 29),
        MEDIA("媒体标题", 60, 8, 36, 34),
        ARTIST("媒体艺人", 60, 29, 36, 27),
        PROGRESS("播放进度", 60, 48, 36, 23),
    }

    data class Style(val x: Int, val y: Int, val width: Int, val size: Int,
                     val color: Int, val visible: Boolean)

    private const val PREFS = "gwm_hud_layout"
    val palette = intArrayOf(Color.WHITE, Color.RED, Color.YELLOW, Color.GREEN,
        0xffff9800.toInt(), Color.CYAN, Color.BLUE)

    fun load(context: Context, part: Part): Style {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = part.name.lowercase()
        return Style(p.getInt("${key}_x", part.x), p.getInt("${key}_y", part.y),
            p.getInt("${key}_width", part.width), p.getInt("${key}_size", part.size),
            p.getInt("${key}_color", Color.WHITE), p.getBoolean("${key}_visible", true))
    }

    fun save(context: Context, part: Part, style: Style) {
        val key = part.name.lowercase()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("${key}_x", style.x.coerceIn(0, 95))
            .putInt("${key}_y", style.y.coerceIn(0, 95))
            .putInt("${key}_width", style.width.coerceIn(5, 100))
            .putInt("${key}_size", style.size.coerceIn(12, 100))
            .putInt("${key}_color", style.color)
            .putBoolean("${key}_visible", style.visible).apply()
        changed?.invoke()
    }

    fun animateMedia(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("animate_media", false)

    fun setAnimateMedia(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("animate_media", enabled).apply()
        changed?.invoke()
    }

    @Volatile var changed: (() -> Unit)? = null
}
