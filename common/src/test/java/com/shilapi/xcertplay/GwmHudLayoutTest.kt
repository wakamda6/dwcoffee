package com.shilapi.xcertplay

import android.content.Context
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class GwmHudLayoutTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test fun existingLayoutBecomesProfileOne() {
        context.getSharedPreferences("gwm_hud_layout", Context.MODE_PRIVATE).edit()
            .putInt("icon_x", 42).putInt("icon_color", Color.RED).apply()
        assertEquals(1, GwmHudLayout.activeProfile(context))
        assertEquals(42, GwmHudLayout.load(context, GwmHudLayout.Part.ICON).x)
        assertEquals(Color.RED, GwmHudLayout.load(context, GwmHudLayout.Part.ICON).color)
    }

    @Test fun profilesKeepIndependentPositionsVisibilityAndAnimation() {
        val parts = GwmHudLayout.Part.entries
        val first = parts.associateWith { GwmHudLayout.load(context, it, 1) }
        val second = parts.associateWith { GwmHudLayout.load(context, it, 2) }
            .toMutableMap()
        second[GwmHudLayout.Part.ICON] = second.getValue(GwmHudLayout.Part.ICON)
            .copy(x = 70, visible = false, color = Color.GREEN)
        GwmHudLayout.saveProfiles(context, mapOf(1 to first, 2 to second),
            mapOf(1 to false, 2 to true), 2)
        assertEquals(2, GwmHudLayout.activeProfile(context))
        assertEquals(70, GwmHudLayout.load(context, GwmHudLayout.Part.ICON).x)
        assertEquals(false, GwmHudLayout.load(context, GwmHudLayout.Part.ICON).visible)
        assertEquals(Color.GREEN, GwmHudLayout.load(context, GwmHudLayout.Part.ICON).color)
        assertEquals(true, GwmHudLayout.animateMedia(context))
        assertEquals(first.getValue(GwmHudLayout.Part.ICON),
            GwmHudLayout.load(context, GwmHudLayout.Part.ICON, 1))
    }
}
