package com.shilapi.xcertplay

import android.app.Presentation
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.SystemClock
import android.view.Display
import android.view.View
import android.view.WindowManager
import com.shilapi.xcertplay.hud.CarPlayCallCard
import com.shilapi.xcertplay.hud.CarPlayHudFeed
import com.shilapi.xcertplay.hud.ClusterTurnGuidance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Latte HUD uses a native Presentation and CarPlay control data, independent of BYD HUD services. */
internal class GwmLatteHudPresentation(context: Context, display: Display) : Presentation(context, display) {
    private var guidance: ClusterTurnGuidance? = null
    private var feed = CarPlayHudFeed.State()
    private var canvasView: HudView? = null
    private var mediaChangedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.apply {
            setFormat(PixelFormat.TRANSLUCENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            setDimAmount(0f)
        }
        val view = HudView(context)
        view.setBackgroundColor(Color.TRANSPARENT)
        canvasView = view
        setContentView(view)
    }

    fun setGuidance(next: ClusterTurnGuidance?) { guidance = next; canvasView?.invalidate() }
    fun setFeed(next: CarPlayHudFeed.State) {
        if (feed.media?.title != next.media?.title) mediaChangedAt = SystemClock.uptimeMillis()
        feed = next
        canvasView?.invalidate()
    }
    fun refreshLayout() { canvasView?.invalidate() }

    private inner class HudView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val clock = SimpleDateFormat("HH:mm", Locale.getDefault())

        override fun onDraw(canvas: Canvas) {
            val turn = guidance
            val media = feed.media
            val call = feed.call
            if (turn != null) {
                line(canvas, GwmHudLayout.Part.ICON, arrow(turn.icon))
                line(canvas, GwmHudLayout.Part.ROAD, turn.road)
                line(canvas, GwmHudLayout.Part.TURN, distance(turn.distanceMeters.toLong()))
                val route = listOfNotNull(
                    turn.remainingMeters?.let(::distance),
                    turn.arrivalEpochSeconds?.let { clock.format(Date(it * 1000)) },
                ).joinToString("   ")
                line(canvas, GwmHudLayout.Part.ROUTE, route)
            }
            if (feed.lanes.isNotEmpty()) {
                line(canvas, GwmHudLayout.Part.LANES, feed.lanes.joinToString("   ") { arrow(it) })
            }
            if (call != null) {
                val state = when (call.phase) {
                    CarPlayCallCard.Phase.RINGING -> "来电"
                    CarPlayCallCard.Phase.DIALING -> "拨号"
                    CarPlayCallCard.Phase.ACTIVE -> "通话中"
                }
                line(canvas, GwmHudLayout.Part.CALL, "$state  ${call.name}")
            }
            if (media != null) {
                val age = SystemClock.uptimeMillis() - mediaChangedAt
                val alpha = if (GwmHudLayout.animateMedia(context) && age in 0..400)
                    (age * 255 / 400).toInt() else 255
                line(canvas, GwmHudLayout.Part.MEDIA, media.title, alpha)
                line(canvas, GwmHudLayout.Part.ARTIST, media.artist, alpha)
                progress(canvas, media)
                if (alpha < 255) postInvalidateDelayed(16)
            }
        }

        private fun line(canvas: Canvas, part: GwmHudLayout.Part, value: String, alpha: Int = 255) {
            if (value.isBlank()) return
            val style = GwmHudLayout.load(context, part)
            if (!style.visible) return
            paint.color = style.color
            paint.alpha = alpha
            paint.textSize = style.size * height / 480f
            paint.style = Paint.Style.FILL
            val x = width * style.x / 100f
            val y = height * style.y / 100f + paint.textSize
            val maxWidth = width * style.width / 100f
            var shown = value
            while (shown.length > 1 && paint.measureText(shown) > maxWidth) shown = shown.dropLast(1)
            if (shown != value) shown = shown.dropLast(1) + "…"
            canvas.drawText(shown, x, y, paint)
        }

        private fun distance(meters: Long): String =
            if (meters >= 1000) String.format(Locale.getDefault(), "%.1f km", meters / 1000.0)
            else "$meters m"

        private fun progress(canvas: Canvas, media: CarPlayHudFeed.Media) {
            val duration = media.durationMillis?.takeIf { it > 0 } ?: return
            val elapsed = media.elapsedMillis?.coerceIn(0, duration) ?: return
            val style = GwmHudLayout.load(context, GwmHudLayout.Part.PROGRESS)
            if (!style.visible) return
            val x = width * style.x / 100f
            val y = height * style.y / 100f
            val barWidth = width * style.width / 100f
            paint.color = style.color
            paint.alpha = 255
            paint.textSize = style.size * height / 480f
            canvas.drawText("${time(elapsed)} / ${time(duration)}", x, y + paint.textSize, paint)
            val barY = y + paint.textSize + 9f
            paint.alpha = 80
            canvas.drawRect(x, barY, x + barWidth, barY + 7f, paint)
            paint.alpha = 255
            canvas.drawRect(x, barY, x + barWidth * elapsed / duration, barY + 7f, paint)
        }

        private fun time(millis: Long): String = "%d:%02d".format(Locale.getDefault(),
            millis / 60_000, millis / 1000 % 60)

        private fun arrow(code: Int): String = when (code) {
            1, 2, 3, 4 -> "↑"
            5, 6, 7, 8 -> "↗"
            9, 10, 11 -> "→"
            12, 13, 14 -> "↘"
            15, 16 -> "↓"
            17, 18, 19 -> "↙"
            20, 21, 22 -> "←"
            23, 24, 25 -> "↖"
            else -> "◆"
        }
    }
}
