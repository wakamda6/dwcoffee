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
    const val PROFILE_COUNT = 3
    val palette = intArrayOf(Color.WHITE, Color.RED, Color.YELLOW, Color.GREEN,
        0xffff9800.toInt(), Color.CYAN, Color.BLUE)

    fun activeProfile(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt("active_profile", 1).coerceIn(1, PROFILE_COUNT)

    private fun key(profile: Int, part: Part): String {
        require(profile in 1..PROFILE_COUNT)
        // Profile 1 keeps existing users' saved layout without a migration step.
        return if (profile == 1) part.name.lowercase() else "profile_${profile}_${part.name.lowercase()}"
    }

    fun load(context: Context, part: Part, profile: Int = activeProfile(context)): Style {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = key(profile, part)
        return Style(p.getInt("${key}_x", part.x), p.getInt("${key}_y", part.y),
            p.getInt("${key}_width", part.width), p.getInt("${key}_size", part.size),
            p.getInt("${key}_color", Color.WHITE), p.getBoolean("${key}_visible", true))
    }

    fun saveProfiles(context: Context, profiles: Map<Int, Map<Part, Style>>,
                     animations: Map<Int, Boolean>, active: Int) {
        require(active in 1..PROFILE_COUNT)
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        profiles.forEach { (profile, styles) ->
            require(profile in 1..PROFILE_COUNT && styles.keys.containsAll(Part.entries))
            Part.entries.forEach { part ->
                val style = requireNotNull(styles[part])
                val key = key(profile, part)
                editor.putInt("${key}_x", style.x.coerceIn(0, 95))
                    .putInt("${key}_y", style.y.coerceIn(0, 95))
                    .putInt("${key}_width", style.width.coerceIn(5, 100))
                    .putInt("${key}_size", style.size.coerceIn(12, 100))
                    .putInt("${key}_color", style.color)
                    .putBoolean("${key}_visible", style.visible)
            }
            editor.putBoolean(animationKey(profile), animations[profile] ?: false)
        }
        editor.putInt("active_profile", active).apply()
        changed?.invoke()
    }

    private fun animationKey(profile: Int) =
        if (profile == 1) "animate_media" else "profile_${profile}_animate_media"

    fun animateMedia(context: Context, profile: Int = activeProfile(context)): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(animationKey(profile), false)

    @Volatile var changed: (() -> Unit)? = null
}
