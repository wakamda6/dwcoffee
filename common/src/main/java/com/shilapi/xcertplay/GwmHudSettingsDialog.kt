package com.shilapi.xcertplay

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView

/** Center-screen editor for the nine independent HUD layout groups found through REA. */
internal object GwmHudSettingsDialog {
    fun show(context: Context) {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
        }
        val picker = Spinner(context)
        val parts = GwmHudLayout.Part.entries
        picker.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item,
            parts.map { it.label })
        root.addView(picker)
        val preview = Preview(context)
        root.addView(preview, LinearLayout.LayoutParams(-1,
            (context.resources.displayMetrics.density * 210).toInt()))
        val controls = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        root.addView(controls)
        val animation = CheckBox(context).apply {
            text = "媒体文字切换动画"
            isChecked = GwmHudLayout.animateMedia(context)
            setOnCheckedChangeListener { _, checked -> GwmHudLayout.setAnimateMedia(context, checked) }
        }
        root.addView(animation)

        fun render(part: GwmHudLayout.Part) {
            preview.selected = part
            controls.removeAllViews()
            var style = GwmHudLayout.load(context, part)
            val visible = CheckBox(context).apply {
                text = "显示"
                isChecked = style.visible
                setOnCheckedChangeListener { _, value ->
                    style = style.copy(visible = value); GwmHudLayout.save(context, part, style)
                }
            }
            controls.addView(visible)
            fun slider(label: String, value: Int, max: Int, update: (Int) -> Unit) {
                val heading = TextView(context).apply { text = "$label: $value" }
                controls.addView(heading)
                controls.addView(SeekBar(context).apply {
                    this.max = max
                    progress = value
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(bar: SeekBar, amount: Int, fromUser: Boolean) {
                            if (fromUser) { heading.text = "$label: $amount"; update(amount) }
                        }
                        override fun onStartTrackingTouch(bar: SeekBar) = Unit
                        override fun onStopTrackingTouch(bar: SeekBar) = Unit
                    })
                })
            }
            slider("横向位置 %", style.x, 95) { style = style.copy(x = it); GwmHudLayout.save(context, part, style) }
            slider("纵向位置 %", style.y, 95) { style = style.copy(y = it); GwmHudLayout.save(context, part, style) }
            slider("宽度 %", style.width, 100) { style = style.copy(width = it); GwmHudLayout.save(context, part, style) }
            slider("字体/图标尺寸", style.size, 100) { style = style.copy(size = it); GwmHudLayout.save(context, part, style) }
            controls.addView(TextView(context).apply { text = "颜色" })
            val colors = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
            GwmHudLayout.palette.forEach { color ->
                colors.addView(TextView(context).apply {
                    text = "●"
                    textSize = 30f
                    setTextColor(color)
                    setShadowLayer(1f, 0f, 0f, Color.DKGRAY)
                    setPadding(5, 3, 5, 3)
                    setOnClickListener {
                        style = style.copy(color = color)
                        GwmHudLayout.save(context, part, style)
                        preview.invalidate()
                    }
                })
            }
            controls.addView(colors)
        }
        picker.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                render(parts[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        render(parts.first())
        AlertDialog.Builder(context).setTitle("拿铁 HUD 设置")
            .setView(ScrollView(context).apply { addView(root) })
            .setPositiveButton("完成", null).show()
    }

    private class Preview(context: Context) : View(context) {
        var selected = GwmHudLayout.Part.ICON
            set(value) { field = value; invalidate() }
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)
            canvas.save()
            val scale = minOf(width / 800f, height / 480f)
            val left = (width - 800f * scale) / 2f
            canvas.translate(left, 0f)
            canvas.scale(scale, scale)
            GwmHudLayout.load(context, selected).let { style ->
                paint.color = Color.DKGRAY
                canvas.drawRect(style.x * 8f, style.y * 4.8f,
                    (style.x + style.width) * 8f, style.y * 4.8f + 55f, paint)
            }
            GwmHudLayout.Part.entries.forEach { part ->
                val style = GwmHudLayout.load(context, part)
                val x = style.x * 8f
                val y = style.y * 4.8f
                paint.color = if (style.visible) style.color else Color.GRAY
                paint.textSize = 25f
                canvas.drawText(part.label, x + 4f, y + 32f, paint)
            }
            canvas.restore()
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked != MotionEvent.ACTION_DOWN &&
                event.actionMasked != MotionEvent.ACTION_MOVE) return true
            val current = GwmHudLayout.load(context, selected)
            val scale = minOf(width / 800f, height / 480f)
            val left = (width - 800f * scale) / 2f
            GwmHudLayout.save(context, selected, current.copy(
                x = ((event.x - left) * 100 / (800f * scale)).toInt(),
                y = (event.y * 100 / (480f * scale)).toInt()))
            invalidate()
            return true
        }
    }
}
