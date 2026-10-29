package com.awaaz.recorder

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var sw: Switch
    private lateinit var volV: TextView
    private lateinit var delV: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rowView(title: String): Pair<LinearLayout, TextView> {
        val v = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFF9A9A9A.toInt())
        }
        val t = TextView(this).apply {
            text = title
            textSize = 17f
            setTextColor(Color.WHITE)
        }
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(18), 0, dp(18))
            addView(t, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(v)
        }
        return Pair(r, v)
    }

    private fun refreshVals() {
        sw.isChecked = Settings.echoOn(this)
        volV.text = Settings.echoVolPct(this).toString() + "%"
        delV.text = "%.1f seconds".format(Settings.echoDelayMs(this) / 1000.0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(dp(18), dp(28), dp(18), dp(24))
        }

        val head = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(20))
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

        sw = Switch(this).apply {
            text = "Echo"
            textSize = 17f
            setTextColor(Color.WHITE)
            setOnCheckedChangeListener { _, on ->
                Settings.prefs(this@SettingsActivity).edit().putBoolean("echoOn", on).apply()
            }
        }
        root.addView(sw)

        val volR = rowView("Echo volume")
        volV = volR.second
        volR.first.setOnClickListener { Settings.pickVolume(this) { refreshVals() } }
        root.addView(volR.first)

        val delR = rowView("Echo delay")
        delV = delR.second
        delR.first.setOnClickListener { Settings.pickDelay(this) { refreshVals() } }
        root.addView(delR.first)

        val note = TextView(this).apply {
            text = "Ye setting turant lagti hai, chal rahe audio mein bhi."
            textSize = 13f
            setTextColor(0xFF9A9A9A.toInt())
            setPadding(0, dp(24), 0, 0)
        }
        root.addView(note)

        setContentView(ScrollView(this).apply { addView(root) })
        refreshVals()
    }

    override fun onResume() {
        super.onResume()
        if (::sw.isInitialized) refreshVals()
    }
}
