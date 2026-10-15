package com.shilapi.xcertplay

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.Spinner
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GwmHudSettingsDialogTest {
    @Test fun editorShowsAllNineGroupsAtCompactAndFullSizes() {
        for ((widthDp, fontScale) in listOf(600 to 1.3f, 1280 to 1.5f)) {
            RuntimeEnvironment.setQualifiers("zh-rCN-w${widthDp}dp-h960dp-mdpi")
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
            assertEquals(9, spinner.adapter.count)
            assertTrue(decor.width > 0 && decor.height > 0)
            descendants(decor).forEach { it.scrollIndicators = 0 }
            val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(bitmap))
            val directory = File("build/gwm-hud-screenshots").apply { mkdirs() }
            File(directory, "hud-editor-${widthDp}dp-font-$fontScale.png").outputStream().use {
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
}
