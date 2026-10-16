package com.shilapi.xcertplay

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import com.shilapi.xcertplay.host.R

/** The editor canvas is a sample layout, not the physical HUD's covered area. */
internal object GwmHudSettingsDialog {
    fun show(context: Context) {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density + .5f).toInt()
        val drafts = (1..GwmHudLayout.PROFILE_COUNT).associateWith { profile ->
            GwmHudLayout.Part.entries.associateWith { GwmHudLayout.load(context, it, profile) }
                .toMutableMap()
        }
        val animations = (1..GwmHudLayout.PROFILE_COUNT).associateWith {
            GwmHudLayout.animateMedia(context, it)
        }.toMutableMap()
        var profile = GwmHudLayout.activeProfile(context)
        var selected = GwmHudLayout.Part.ICON
        fun style() = requireNotNull(drafts.getValue(profile)[selected])

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val profilePicker = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item,
                (1..GwmHudLayout.PROFILE_COUNT).map { context.getString(R.string.hud_editor_profile, it) })
        }
        root.addView(profilePicker)
        root.addView(TextView(context).apply {
            text = context.getString(R.string.hud_editor_hint)
            textSize = 13f
            setPadding(0, dp(8), 0, dp(8))
        })
        val selectedLabel = TextView(context).apply {
            textSize = 17f
            setPadding(0, dp(8), 0, dp(4))
        }
        val visible = CheckBox(context).apply { text = context.getString(R.string.hud_editor_visible) }
        val widthLabel = TextView(context)
        val widthBar = SeekBar(context).apply { max = 95 }
        val animation = CheckBox(context).apply { text = context.getString(R.string.hud_editor_animation) }
        var syncing = false
        lateinit var syncControls: () -> Unit
        val preview = Preview(context, { drafts.getValue(profile) }) { part ->
            selected = part
            syncControls()
        }
        preview.selected = selected
        root.addView(preview, LinearLayout.LayoutParams(-1, dp(220)))
        root.addView(selectedLabel)
        val sizeRow = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        fun sizeButton(label: String, factor: Float) = Button(context).apply {
            text = label
            setOnClickListener {
                val current = style()
                drafts.getValue(profile)[selected] = current.copy(
                    size = (current.size * factor).toInt().coerceIn(12, 100),
                    width = (current.width * factor).toInt().coerceIn(5, 100))
                syncControls()
            }
        }
        sizeRow.addView(sizeButton(context.getString(R.string.hud_editor_shrink), .9f),
            LinearLayout.LayoutParams(0, dp(48), 1f))
        sizeRow.addView(sizeButton(context.getString(R.string.hud_editor_enlarge), 1.1f),
            LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(sizeRow)
        root.addView(visible)
        root.addView(widthLabel)
        root.addView(widthBar)
        root.addView(TextView(context).apply { text = context.getString(R.string.hud_editor_color) })
        val colors = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        GwmHudLayout.palette.forEach { color ->
            colors.addView(TextView(context).apply {
                text = "●"
                textSize = 31f
                gravity = Gravity.CENTER
                setTextColor(color)
                setShadowLayer(1f, 0f, 0f, Color.DKGRAY)
                contentDescription = context.getString(R.string.hud_editor_select_color)
                setOnClickListener {
                    drafts.getValue(profile)[selected] = style().copy(color = color)
                    syncControls()
                }
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        root.addView(colors)
        root.addView(animation)

        syncControls = {
            syncing = true
            val current = style()
            selectedLabel.text = context.getString(R.string.hud_editor_selected,
                selected.label, current.size)
            visible.isChecked = current.visible
            widthBar.progress = current.width - 5
            widthLabel.text = context.getString(R.string.hud_editor_width, current.width)
            animation.isChecked = animations[profile] ?: false
            preview.selected = selected
            preview.invalidate()
            syncing = false
        }
        visible.setOnCheckedChangeListener { _, checked ->
            if (!syncing) {
                drafts.getValue(profile)[selected] = style().copy(visible = checked)
                preview.invalidate()
            }
        }
        widthBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && !syncing) {
                    val width = (progress + 5).coerceIn(5, 100)
                    drafts.getValue(profile)[selected] = style().copy(width = width)
                    widthLabel.text = context.getString(R.string.hud_editor_width, width)
                    preview.invalidate()
                }
            }
            override fun onStartTrackingTouch(bar: SeekBar) = Unit
            override fun onStopTrackingTouch(bar: SeekBar) = Unit
        })
        animation.setOnCheckedChangeListener { _, checked ->
            if (!syncing) animations[profile] = checked
        }
        profilePicker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                profile = position + 1
                syncControls()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        profilePicker.setSelection(profile - 1)
        syncControls()
        AlertDialog.Builder(context).setTitle(R.string.settings_gwm_hud_layout_title)
            .setView(ScrollView(context).apply { addView(root) })
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.hud_editor_save) { _, _ ->
                GwmHudLayout.saveProfiles(context, drafts, animations, profile)
            }.show()
    }

    private class Preview(
        context: Context,
        private val styles: () -> MutableMap<GwmHudLayout.Part, GwmHudLayout.Style>,
        private val onSelect: (GwmHudLayout.Part) -> Unit,
    ) : View(context) {
        var selected = GwmHudLayout.Part.ICON
            set(value) { field = value; invalidate() }
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val samples = mapOf(
            GwmHudLayout.Part.ICON to "↰",
            GwmHudLayout.Part.ROAD to context.getString(R.string.hud_editor_current_road),
            GwmHudLayout.Part.TURN to context.getString(R.string.hud_editor_turn_sample),
            GwmHudLayout.Part.ROUTE to context.getString(R.string.hud_editor_route_sample),
            GwmHudLayout.Part.LANES to "↑   ↰   ↑",
            GwmHudLayout.Part.CALL to context.getString(R.string.hud_editor_call_sample),
            GwmHudLayout.Part.MEDIA to context.getString(R.string.hud_editor_media_sample),
            GwmHudLayout.Part.ARTIST to context.getString(R.string.hud_editor_artist_sample),
            GwmHudLayout.Part.PROGRESS to "1:23 / 3:45  ▰▰▱▱",
        )
        private val bounds = mutableMapOf<GwmHudLayout.Part, RectF>()
        private var dragOffsetX = 0f
        private var dragOffsetY = 0f
        private var dragging = false

        override fun onDraw(canvas: Canvas) {
            canvas.drawColor(Color.rgb(18, 23, 29))
            val sx = width / 800f
            val sy = height / 480f
            bounds.clear()
            GwmHudLayout.Part.entries.forEach { part ->
                val style = requireNotNull(styles()[part])
                val x = width * style.x / 100f
                val y = height * style.y / 100f
                val textSize = style.size * sy
                val rect = RectF(x, y, (x + width * style.width / 100f).coerceAtMost(width.toFloat()),
                    (y + maxOf(textSize * 1.3f, 36f * sy)).coerceAtMost(height.toFloat()))
                bounds[part] = rect
                paint.color = if (style.visible) style.color else Color.GRAY
                paint.alpha = if (style.visible) 255 else 120
                paint.style = Paint.Style.FILL
                paint.textSize = textSize.coerceAtLeast(12f * sy)
                var shown = requireNotNull(samples[part])
                while (shown.length > 1 && paint.measureText(shown) > rect.width()) {
                    shown = shown.dropLast(1)
                }
                canvas.drawText(shown, x, y + paint.textSize, paint)
                if (part == selected) {
                    paint.color = Color.CYAN
                    paint.alpha = 255
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f * sx
                    canvas.drawRect(rect, paint)
                    paint.style = Paint.Style.FILL
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val hit = GwmHudLayout.Part.entries.reversed().firstOrNull {
                        bounds[it]?.contains(event.x, event.y) == true
                    } ?: return true
                    selected = hit
                    val rect = requireNotNull(bounds[hit])
                    dragOffsetX = event.x - rect.left
                    dragOffsetY = event.y - rect.top
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    onSelect(hit)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!dragging || event.pointerCount != 1 || width == 0 || height == 0) return true
                    val current = requireNotNull(styles()[selected])
                    val x = ((event.x - dragOffsetX) * 100 / width).toInt().coerceIn(0, 95)
                    val y = ((event.y - dragOffsetY) * 100 / height).toInt().coerceIn(0, 95)
                    styles()[selected] = current.copy(x = x, y = y)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    dragging = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    performClick()
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
            return true
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }
    }
}
