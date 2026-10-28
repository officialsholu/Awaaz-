package com.awaaz.recorder

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

data class SetRow(val key: String, val label: String, val min: Int, val max: Int, val def: Int, val unit: String)

class SettingsActivity : AppCompatActivity() {

    private val rows = listOf(
        SetRow("echoVol", "Echo volume", 0, 100, 35, "%"),
        SetRow("echoDelay", "Echo delay", 50, 600, 250, " ms"),
        SetRow("echoRepeat", "Echo repeat", 0, 85, 40, "%"),
        SetRow("reverb", "Reverb", 0, 100, 30, "%"),
        SetRow("room", "Room size", 0, 100, 60, "%"),
        SetRow("warmth", "Warmth", 0, 100, 30, "%"),
        SetRow("clarity", "Clarity", 0, 100, 20, "%")
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        val prefs = Settings.prefs(this)
        val red = 0xFFE8504A.toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(dp(18), dp(28), dp(18), dp(24))
        }

        val head = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(16))
        }
        head.addView(Icon2View(this, Icon2View.BACK).apply {
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
            setOnClickListener { finish() }
        })
        head.addView(TextView(this).apply {
            text = "Settings"
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(dp(12), 0, 0, 0)
        })
        root.addView(head)

        val sw = Switch(this).apply {
            text = "Echo"
            textSize = 16f
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("echoOn", true)
            setOnCheckedChangeListener { _, on ->
                prefs.edit().putBoolean("echoOn", on).apply()
            }
        }
        root.addView(sw)

        for (r in rows) {
            val cur = prefs.getInt(r.key, r.def)
            val valT = TextView(this).apply {
                text = "$cur${r.unit}"
                textSize = 14f
                setTextColor(0xFF9A9A9A.toInt())
            }
            val title = TextView(this).apply {
                text = r.label
                textSize = 16f
                setTextColor(Color.WHITE)
            }
            val top = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(14), 0, 0)
                addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(valT)
            }
            val sb = SeekBar(this).apply {
                max = r.max - r.min
                progress = cur - r.min
                progressTintList = ColorStateList.valueOf(red)
                progressBackgroundTintList = ColorStateList.valueOf(0xFF4A4A4A.toInt())
                thumbTintList = ColorStateList.valueOf(red)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        val v = p + r.min
                        valT.text = "$v${r.unit}"
                        if (fromUser) prefs.edit().putInt(r.key, v).apply()
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            }
            root.addView(top)
            root.addView(sb)
        }

        val note = TextView(this).apply {
            text = "Ye settings nayi recordings aur Enhance (Meri settings) par lagengi."
            textSize = 13f
            setTextColor(0xFF9A9A9A.toInt())
            setPadding(0, dp(24), 0, 0)
        }
        root.addView(note)

        setContentView(ScrollView(this).apply { addView(root) })
    }
}
