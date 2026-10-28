
package com.awaaz.recorder

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

object EchoSettingsSheet {

    private const val RED = 0xFFE8504A.toInt()
    private const val BG = 0xFF202020.toInt()
    private const val WHITE = 0xFFE6E6E6.toInt()
    private const val GREY = 0xFF999999.toInt()

    fun show(
        context: Context,
        initial: Preset = Effects.MASJID,
        onPreview: (Preset) -> Unit = {},
        onApply: (Preset) -> Unit = {}
    ) {
        val d = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(22))
            background = GradientDrawable().apply {
                setColor(BG)
                cornerRadius = dp(26).toFloat()
            }
        }

        fun text(
            value: String,
            size: Float,
            color: Int = WHITE,
            bold: Boolean = false
        ) = TextView(context).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }

        val heading = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        heading.addView(
            text("Echo settings", 22f, WHITE, true),
            LinearLayout.LayoutParams(0, dp(42), 1f)
        )

        val close = text("✕", 24f).apply {
            gravity = Gravity.CENTER
            setOnClickListener { dialog.dismiss() }
        }
        heading.addView(close, LinearLayout.LayoutParams(dp(40), dp(42)))
        root.addView(heading)

        root.addView(
            text("Adjust the sound of your recording", 13f, GREY)
        )

        val enabled = Switch(context).apply {
            text = "Enable effects"
            textSize = 15f
            setTextColor(WHITE)
            buttonTintList = null
            isChecked = initial.echoMix > 0f || initial.reverbMix > 0f
        }

        val switchRow = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(8))
            addView(
                enabled,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(48)
                )
            )
        }
        root.addView(switchRow)

        fun addSlider(
            title: String,
            min: Int,
            max: Int,
            start: Int,
            suffix: String
        ): SeekBar {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(7), 0, dp(5))
            }

            val label = text("", 14f)
            val seek = SeekBar(context).apply {
                this.max = max - min
                progress = (start - min).coerceIn(0, max - min)
                progressTintList =
                    android.content.res.ColorStateList.valueOf(RED)
                thumbTintList =
                    android.content.res.ColorStateList.valueOf(RED)
            }

            fun updateLabel(value: Int) {
                label.text = "$title     $value$suffix"
            }

            updateLabel(start.coerceIn(min, max))

            seek.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        bar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        updateLabel(progress + min)
                    }

                    override fun onStartTrackingTouch(bar: SeekBar?) {}
                    override fun onStopTrackingTouch(bar: SeekBar?) {}
                }
            )

            row.addView(label)
            row.addView(seek)
            root.addView(row)
            return seek
        }

        val echo = addSlider(
            "Echo amount", 0, 100,
            (initial.echoMix * 100).toInt(), "%"
        )

        val delay = addSlider(
            "Delay", 50, 500,
            initial.echoMs.coerceIn(50, 500), " ms"
        )

        val feedback = addSlider(
            "Feedback", 0, 90,
            (initial.feedback * 100).toInt(), "%"
        )

        val reverb = addSlider(
            "Reverb", 0, 100,
            (initial.reverbMix * 100).toInt(), "%"
        )

        fun currentPreset(): Preset {
            val active = enabled.isChecked

            return initial.copy(
                echoMix = if (active) echo.progress / 100f else 0f,
                echoMs = delay.progress + 50,
                feedback = feedback.progress / 100f,
                reverbMix =
                    if (active) reverb.progress / 100f else 0f
            )
        }

        val buttons = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        }

        val preview = Button(context).apply {
            text = "Preview"
            isAllCaps = false
            setTextColor(WHITE)
            setBackgroundColor(0xFF393939.toInt())
            setOnClickListener { onPreview(currentPreset()) }
        }

        val apply = Button(context).apply {
            text = "Apply"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(RED)
            setOnClickListener {
                onApply(currentPreset())
                dialog.dismiss()
            }
        }

        buttons.addView(
            preview,
            LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                marginEnd = dp(8)
            }
        )
        buttons.addView(
            apply,
            LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                marginStart = dp(8)
            }
        )

        root.addView(buttons)

        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.BOTTOM)
            addFlags(Window.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply {
                dimAmount = 0.65f
            }
        }

        dialog.show()

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.BOTTOM)
    }
}
