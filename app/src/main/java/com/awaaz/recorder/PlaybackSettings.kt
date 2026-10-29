package com.awaaz.recorder

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

object Playback {
    private val steps = floatArrayOf(0.5f, 1f, 1.5f, 2f)

    fun speedIndex(c: Context): Int = Settings.prefs(c).getInt("pbSpeed", 1).coerceIn(0, 3)
    fun speed(c: Context): Float = steps[speedIndex(c)]
    fun skipSilence(c: Context): Boolean = Settings.prefs(c).getBoolean("pbSkip", false)
    fun loop(c: Context): Boolean = Settings.prefs(c).getBoolean("pbLoop", false)
    fun vocal(c: Context): Boolean = Settings.prefs(c).getBoolean("pbVocal", false)

    fun effectivePreset(c: Context): Preset {
        val p = Settings.preset(c)
        return if (vocal(c)) p.copy(warmth = 0.6f, clarity = 0.6f) else p
    }
}

object PlaybackSettings {
    private val labels = listOf("0.5x", "Normal", "1.5x", "2.0x")
    private val gray = 0xFF9A9A9A.toInt()
    private val red = 0xFFE8504A.toInt()

    fun show(ctx: Context) {
        val d = Dialog(ctx)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dp = { v: Int -> (v * ctx.resources.displayMetrics.density).toInt() }
        val prefs = Settings.prefs(ctx)

        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.VERTICAL
        root.background = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            setColor(0xFF1E1E1E.toInt())
        }
        root.setPadding(dp(22), dp(18), dp(22), dp(24))

        val head = LinearLayout(ctx)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val titles = LinearLayout(ctx)
        titles.orientation = LinearLayout.VERTICAL
        titles.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        titles.addView(TextView(ctx).apply {
            text = "Playback settings"
            textSize = 20f
            setTextColor(Color.WHITE)
        })
        titles.addView(TextView(ctx).apply {
            text = "Works for a single recording only"
            textSize = 13f
            setTextColor(gray)
        })
        head.addView(titles)
        head.addView(IconView(ctx, IconView.CLOSE).apply {
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
            setOnClickListener { d.dismiss() }
        })
        root.addView(head)

        root.addView(TextView(ctx).apply {
            text = "Playback speed"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, dp(22), 0, dp(6))
        })

        val bar = SeekBar(ctx)
        bar.max = 3
        bar.progress = Playback.speedIndex(ctx)
        bar.progressTintList = ColorStateList.valueOf(red)
        bar.progressBackgroundTintList = ColorStateList.valueOf(0xFF4A4A4A.toInt())
        bar.thumbTintList = ColorStateList.valueOf(red)
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) prefs.edit().putInt("pbSpeed", p).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        root.addView(bar)

        val lab = LinearLayout(ctx)
        lab.orientation = LinearLayout.HORIZONTAL
        labels.forEachIndexed { i, s ->
            lab.addView(
                TextView(ctx).apply {
                    text = s
                    textSize = 14f
                    setTextColor(if (i == 1) Color.WHITE else gray)
                    gravity = when (i) {
                        0 -> Gravity.START
                        3 -> Gravity.END
                        else -> Gravity.CENTER
                    }
                },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
        }
        root.addView(lab)

        val card = LinearLayout(ctx)
        card.orientation = LinearLayout.VERTICAL
        card.background = GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(0xFF262626.toInt())
        }
        card.setPadding(dp(16), dp(4), dp(16), dp(4))
        card.addView(switchRow(ctx, "Skip silent segments", null, "pbSkip", prefs, dp))
        card.addView(divider(ctx, dp))
        card.addView(switchRow(ctx, "Loop", null, "pbLoop", prefs, dp))
        card.addView(divider(ctx, dp))
        card.addView(
            switchRow(
                ctx,
                "Vocal enhancement",
                "Awaaz saaf aur saamne aati hai, shor kam hota hai.",
                "pbVocal",
                prefs,
                dp
            )
        )
        root.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(18) }
        )

        d.setContentView(root)
        d.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.BOTTOM)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        d.show()
    }

    private fun switchRow(
        ctx: Context,
        label: String,
        sub: String?,
        key: String,
        prefs: SharedPreferences,
        dp: (Int) -> Int
    ): View {
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(14), 0, dp(14))

        val texts = LinearLayout(ctx)
        texts.orientation = LinearLayout.VERTICAL
        texts.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        texts.addView(TextView(ctx).apply {
            text = label
            textSize = 17f
            setTextColor(Color.WHITE)
        })
        if (sub != null) {
            texts.addView(TextView(ctx).apply {
                text = sub
                textSize = 13f
                setTextColor(gray)
            })
        }
        row.addView(texts)

        row.addView(Switch(ctx).apply {
            isChecked = prefs.getBoolean(key, false)
            setOnCheckedChangeListener { _, on ->
                prefs.edit().putBoolean(key, on).apply()
            }
        })
        return row
    }

    private fun divider(ctx: Context, dp: (Int) -> Int): View =
        View(ctx).apply {
            setBackgroundColor(0xFF333333.toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        }
}
