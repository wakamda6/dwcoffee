package com.shilapi.xcertplay

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.content.DialogInterface
import android.view.View
import android.os.Looper
import android.widget.Spinner
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GwmHudSettingsDialogTest {
    @Test fun cancellingKeepsActiveProfileAndSavingSwitchesIt() {
        RuntimeEnvironment.setQualifiers("zh-rCN-w600dp-h960dp-mdpi")
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.setTheme(android.R.style.Theme_Material_NoActionBar)
        GwmHudSettingsDialog.show(activity)
        var dialog = ShadowAlertDialog.getLatestAlertDialog()
        layout(dialog.window!!.decorView)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val firstPicker = descendants(dialog.window!!.decorView).filterIsInstance<Spinner>().single()
        firstPicker.setSelection(2)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        firstPicker.onItemSelectedListener!!.onItemSelected(firstPicker, null, 2, 2)
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        assertEquals(1, GwmHudLayout.activeProfile(activity))

        GwmHudSettingsDialog.show(activity)
        dialog = ShadowAlertDialog.getLatestAlertDialog()
        layout(dialog.window!!.decorView)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val secondPicker = descendants(dialog.window!!.decorView).filterIsInstance<Spinner>().single()
        secondPicker.setSelection(2)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        secondPicker.onItemSelectedListener!!.onItemSelected(secondPicker, null, 2, 2)
        assertEquals(2, secondPicker.selectedItemPosition)
        assertTrue(dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick())
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(3, GwmHudLayout.activeProfile(activity))
        activity.finish()
    }

    @Test fun editorShowsThreeProfilesAtCompactAndFullSizes() {
        for ((locale, widthDp, fontScale) in listOf(
            Triple("zh-rCN", 320, 1.3f),
            Triple("zh-rCN", 1280, 1.5f),
            Triple("ar", 320, 1.3f),
        )) {
            RuntimeEnvironment.setQualifiers("${locale}-w${widthDp}dp-h960dp-mdpi")
            val app = RuntimeEnvironment.getApplication()
            app.resources.updateConfiguration(Configuration(app.resources.configuration).apply {
                this.fontScale = fontScale
            }, app.resources.displayMetrics)
            val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
            activity.setTheme(android.R.style.Theme_Material_NoActionBar)
            GwmHudSettingsDialog.show(activity)
            val dialog = ShadowAlertDialog.getLatestAlertDialog()
            val decor = dialog.window!!.decorView
            val width = (widthDp * app.resources.displayMetrics.density).toInt()
            val height = (960 * app.resources.displayMetrics.density).toInt()
            decor.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            decor.layout(0, 0, decor.measuredWidth, decor.measuredHeight)
            val spinner = descendants(decor).filterIsInstance<Spinner>().single()
            val preview = descendants(decor).first { it.javaClass.simpleName == "Preview" }
            assertEquals(3, spinner.adapter.count)
            assertTrue(decor.width > 0 && decor.height > 0)
            assertTrue(preview.width > 0)
            assertEquals(480f / 800f, preview.height.toFloat() / preview.width, .01f)
            assertTrue(preview.width <= (800 * app.resources.displayMetrics.density).toInt())
            descendants(decor).forEach { it.scrollIndicators = 0 }
            val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(bitmap))
            val directory = File("build/gwm-hud-screenshots").apply { mkdirs() }
            File(directory, "hud-editor-$locale-${widthDp}dp-font-$fontScale.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
            dialog.dismiss()
            activity.finish()
        }
    }

    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is android.view.ViewGroup) {
            for (index in 0 until view.childCount) yieldAll(descendants(view.getChildAt(index)))
        }
    }

    private fun layout(decor: View) {
        decor.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(960, View.MeasureSpec.EXACTLY))
        decor.layout(0, 0, 600, 960)
    }
}
