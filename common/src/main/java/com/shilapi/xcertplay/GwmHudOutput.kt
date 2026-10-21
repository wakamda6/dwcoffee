package com.shilapi.xcertplay

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.shilapi.xcertplay.hud.CarPlayHudFeed

/** Process-owned transparent HUD window for an active CarPlay session with content. */
internal object GwmHudOutput {
    private const val TAG = "GwmHudOutput"
    private val main = Handler(Looper.getMainLooper())
    private var app: Context? = null
    private var presentation: GwmLatteHudPresentation? = null
    private var feed = CarPlayHudFeed.State()

    private val feedListener: (CarPlayHudFeed.State) -> Unit = { next ->
        onMain {
            feed = next
            refreshOnMain()
        }
    }
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = refreshOnMain()
        override fun onDisplayRemoved(displayId: Int) = refreshOnMain()
        override fun onDisplayChanged(displayId: Int) = refreshOnMain()
    }

    fun start(context: Context) = onMain {
        if (app == null) {
            app = context.applicationContext
            app?.getSystemService(DisplayManager::class.java)
                ?.registerDisplayListener(displayListener, main)
            GwmHudLayout.changed = { onMain { presentation?.refreshLayout() } }
            CarPlayHudFeed.setListener(feedListener)
        }
        refreshOnMain()
    }

    fun refresh(context: Context) = start(context)

    private fun refreshOnMain() {
        val context = app ?: return
        val hasContent = feed.guidance != null || feed.media != null ||
            feed.call != null || feed.lanes.isNotEmpty()
        val display = if (CarPlayBackgroundSession.active && hasContent)
            GwmLatteDisplays.hud(context) else null
        if (display == null) {
            presentation?.dismiss()
            presentation = null
            return
        }
        val current = presentation
        if (current != null && current.display.displayId == display.displayId && current.isShowing) {
            current.setGuidance(feed.guidance)
            current.setFeed(feed)
            return
        }
        current?.dismiss()
        val next = GwmLatteHudPresentation(context, display)
        next.setGuidance(feed.guidance)
        next.setFeed(feed)
        next.setOnDismissListener { if (presentation === next) presentation = null }
        try {
            next.show()
            presentation = next
            Log.i(TAG, "HUD shown display=${display.displayId} " +
                "navigation=${feed.guidance != null} media=${feed.media != null} " +
                "lanes=${feed.lanes.isNotEmpty()} call=${feed.call != null}")
        } catch (error: RuntimeException) {
            Log.e(TAG, "HUD presentation failed display=${display.displayId}", error)
        }
    }

    private inline fun onMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post { block() }
    }
}
