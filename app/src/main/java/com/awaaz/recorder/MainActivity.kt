package com.awaaz.recorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IconView(c: Context, var kind: Int) : View(c) {
    companion object {
        const val SELECT = 0
        const val SEARCH = 1
        const val MORE = 2
        const val PLAY = 3
        const val PAUSE = 4
        const val TRASH = 5
        const val SHARE = 6
        const val EDIT = 7
        const val SPARK = 8
        const val CLOSE = 9
        const val SLIDERS = 10
        const val SPEAKER = 11
    }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val light = 0xFFE6E6E6.toInt()
    private val red = 0xFFE8504A.toInt()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val d = resources.displayMetrics.density
        p.strokeWidth = d * 2f
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        when (kind) {
            SELECT -> {
                p.style = Paint.Style.STROKE
                p.color = light
                c.drawRoundRect(RectF(w * .16f, h * .16f, w * .84f, h * .84f), d * 4, d * 4, p)
                path.reset()
                path.moveTo(w * .31f, h * .52f)
                path.lineTo(w * .45f, h * .66f)
                path.lineTo(w * .70f, h * .36f)
                c.drawPath(path, p)
            }
            SEARCH -> {
                p.style = Paint.Style.STROKE
                p.color = light
                c.drawCircle(w * .45f, h * .45f, w * .26f, p)
                c.drawLine(w * .64f, h * .64f, w * .84f, h * .84f, p)
            }
            MORE -> {
                p.style = Paint.Style.FILL
                p.color = light
                c.drawCircle(w * .5f, h * .22f, d * 2.3f, p)
                c.drawCircle(w * .5f, h * .5f, d * 2.3f, p)
                c.drawCircle(w * .5f, h * .78f, d * 2.3f, p)
            }
            PLAY -> {
                p.style = Paint.Style.FILL
                p.color = red
                path.reset()
                path.moveTo(w * .36f, h * .26f)
                path.lineTo(w * .36f, h * .74f)
                path.lineTo(w * .76f, h * .5f)
                path.close()
                c.drawPath(path, p)
            }
            PAUSE -> {
                p.style = Paint.Style.FILL
                p.color = red
                c.drawRoundRect(RectF(w * .32f, h * .26f, w * .46f, h * .74f), d * 2, d * 2, p)
                c.drawRoundRect(RectF(w * .54f, h * .26f, w * .68f, h * .74f), d * 2, d * 2, p)
            }
            TRASH -> {
                p.style = Paint.Style.STROKE
                p.color = light
                c.drawLine(w * .2f, h * .28f, w * .8f, h * .28f, p)
                c.drawRoundRect(RectF(w * .4f, h * .16f, w * .6f, h * .28f), d * 2, d * 2, p)
                path.reset()
                path.moveTo(w * .27f, h * .28f)
                path.lineTo(w * .32f, h * .82f)
                path.lineTo(w * .68f, h * .82f)
                path.lineTo(w * .73f, h * .28f)
                c.drawPath(path, p)
                c.drawLine(w * .42f, h * .4f, w * .42f, h * .7f, p)
                c.drawLine(w * .58f, h * .4f, w * .58f, h * .7f, p)
            }
            SHARE -> {
                p.style = Paint.Style.STROKE
                p.color = light
                path.reset()
                path.moveTo(w * .56f, h * .22f)
                path.lineTo(w * .24f, h * .22f)
                path.lineTo(w * .24f, h * .78f)
                path.lineTo(w * .78f, h * .78f)
                path.lineTo(w * .78f, h * .5f)
                c.drawPath(path, p)
                c.drawLine(w * .46f, h * .54f, w * .8f, h * .2f, p)
                path.reset()
                path.moveTo(w * .62f, h * .2f)
                path.lineTo(w * .8f, h * .2f)
                path.lineTo(w * .8f, h * .38f)
                c.drawPath(path, p)
            }
            EDIT -> {
                p.style = Paint.Style.STROKE
                p.color = light
                path.reset()
                path.moveTo(w * .56f, h * .22f)
                path.lineTo(w * .24f, h * .22f)
                path.lineTo(w * .24f, h * .78f)
                path.lineTo(w * .78f, h * .78f)
                path.lineTo(w * .78f, h * .5f)
                c.drawPath(path, p)
                p.strokeWidth = d * 3f
                c.drawLine(w * .42f, h * .58f, w * .78f, h * .22f, p)
            }
            SPARK -> {
                p.style = Paint.Style.STROKE
                p.color = light
                path.reset()
                path.moveTo(w * .5f, h * .15f)
                path.quadTo(w * .5f, h * .5f, w * .85f, h * .5f)
                path.quadTo(w * .5f, h * .5f, w * .5f, h * .85f)
                path.quadTo(w * .5f, h * .5f, w * .15f, h * .5f)
                path.quadTo(w * .5f, h * .5f, w * .5f, h * .15f)
                c.drawPath(path, p)
            }
            CLOSE -> {
                p.style = Paint.Style.STROKE
                p.color = light
                c.drawLine(w * .25f, h * .25f, w * .75f, h * .75f, p)
                c.drawLine(w * .75f, h * .25f, w * .25f, h * .75f, p)
            }
            SLIDERS -> {
                p.style = Paint.Style.STROKE
                p.color = light
                c.drawLine(w * .15f, h * .3f, w * .85f, h * .3f, p)
                c.drawLine(w * .15f, h * .5f, w * .85f, h * .5f, p)
                c.drawLine(w * .15f, h * .7f, w * .85f, h * .7f, p)
                c.drawCircle(w * .35f, h * .3f, d * 3.5f, p)
                c.drawCircle(w * .65f, h * .5f, d * 3.5f, p)
                c.drawCircle(w * .4f, h * .7f, d * 3.5f, p)
            }
            SPEAKER -> {
                p.style = Paint.Style.STROKE
                p.color = light
                path.reset()
                path.moveTo(w * .18f, h * .38f)
                path.lineTo(w * .34f, h * .38f)
                path.lineTo(w * .54f, h * .22f)
                path.lineTo(w * .54f, h * .78f)
                path.lineTo(w * .34f, h * .62f)
                path.lineTo(w * .18f, h * .62f)
                path.close()
                c.drawPath(path, p)
                c.drawArc(RectF(w * .5f, h * .32f, w * .78f, h * .68f), -50f, 100f, false, p)
                c.drawArc(RectF(w * .58f, h * .2f, w * .92f, h * .8f), -50f, 100f, false, p)
            }
        }
    }
}

class CheckView(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    var state: Int = 0
        set(v) {
            field = v
            invalidate()
        }

    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        val r = RectF(d * 2, d * 2, w - d * 2, h - d * 2)
        if (state == 0) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = d * 2f
            p.color = 0xFF777777.toInt()
            c.drawRoundRect(r, d * 4, d * 4, p)
        } else {
            p.style = Paint.Style.FILL
            p.color = 0xFFE8504A.toInt()
            c.drawRoundRect(r, d * 4, d * 4, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = d * 2f
            p.strokeCap = Paint.Cap.ROUND
            p.strokeJoin = Paint.Join.ROUND
            p.color = Color.WHITE
            if (state == 1) {
                path.reset()
                path.moveTo(w * .28f, h * .52f)
                path.lineTo(w * .43f, h * .67f)
                path.lineTo(w * .72f, h * .34f)
                c.drawPath(path, p)
            } else {
                c.drawLine(w * .3f, h * .5f, w * .7f, h * .5f, p)
            }
        }
    }
}

class RecView(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    var recording: Boolean = false
        set(v) {
            if (field != v) {
                field = v
                invalidate()
            }
        }

    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f - d * 3f
        p.style = Paint.Style.STROKE
        p.strokeWidth = d * 3f
        p.color = 0xFF555555.toInt()
        c.drawCircle(cx, cy, r, p)
        p.style = Paint.Style.FILL
        p.color = 0xFFE8504A.toInt()
        if (recording) {
            val s = r * 0.42f
            c.drawRoundRect(RectF(cx - s, cy - s, cx + s, cy + s), d * 5, d * 5, p)
        } else {
            c.drawCircle(cx, cy, r * 0.52f, p)
        }
    }
}

class MainActivity : AppCompatActivity() {

    private val red = 0xFFE8504A.toInt()
    private val cardBg = 0xFF202020.toInt()
    private val gray = 0xFF9A9A9A.toInt()
    private val tabGray = 0xFF8A8A8A.toInt()
    private val light = 0xFFE6E6E6.toInt()
    private val handler = Handler(Looper.getMainLooper())

    private var ready = false
    private var tab = 0
    private lateinit var list: LinearLayout
    private lateinit var rec: RecView
    private lateinit var normalTop: LinearLayout
    private lateinit var selTop: LinearLayout
    private lateinit var selCount: TextView
    private lateinit var selAll: CheckView
    private lateinit var recBottom: LinearLayout
    private lateinit var barBottom: LinearLayout
    private val tabLabels = listOf("All", "Recorder", "Phone")
    private val tabTexts = ArrayList<TextView>()
    private val tabBars = ArrayList<View>()

    private var selMode = false
    private val selected = LinkedHashSet<String>()
    private val checks = HashMap<String, CheckView>()
    private var shown: List<File> = emptyList()
    private var popup: PopupWindow? = null

    private var player: MediaPlayer? = null
    private var playingPath: String? = null
    private var expanded: String? = null
    private var seek: SeekBar? = null
    private var curT: TextView? = null
    private var totT: TextView? = null
    private var wasRecording = false

    private val ticker = object : Runnable {
        override fun run() {
            try {
                val r = RecorderService.recording
                if (wasRecording && !r) refresh()
                wasRecording = r
                rec.recording = r
                updatePlayback()
            } catch (_: Throwable) {
            }
            handler.postDelayed(this, 250)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (selMode) {
                    exitSel()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        try {
            setContentView(buildUi())
            ready = true
        } catch (e: Throwable) {
            showError(e)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!ready) return
        try {
            refresh()
            handler.removeCallbacks(ticker)
            handler.post(ticker)
        } catch (e: Throwable) {
            showError(e)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    override fun onDestroy() {
        popup?.dismiss()
        stopPlayer()
        super.onDestroy()
    }

    private fun showError(e: Throwable) {
        val tv = TextView(this).apply {
            text = "Error:\n" + android.util.Log.getStackTraceString(e)
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(dp(16), dp(48), dp(16), dp(16))
        }
        val sv = ScrollView(this)
        sv.setBackgroundColor(Color.BLACK)
        sv.addView(tv)
        setContentView(sv)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun medium() = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(dp(16), dp(28), dp(16), dp(10))
        }

        normalTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(8), dp(4), dp(16))
        }
        normalTop.addView(TextView(this).apply {
            text = "Recorder"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = medium()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        for (k in listOf(IconView.SELECT, IconView.SEARCH, IconView.MORE)) {
            normalTop.addView(IconView(this, k).apply {
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                    marginStart = dp(14)
                }
                if (k == IconView.SELECT) setOnClickListener { enterSel(null) }
            })
        }
        root.addView(normalTop)

        selTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(8), dp(4), dp(16))
            visibility = View.GONE
        }
        selAll = CheckView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
            setOnClickListener { toggleAll() }
        }
        selCount = TextView(this).apply {
            text = "0 selected"
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = medium()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        selTop.addView(selAll)
        selTop.addView(selCount)
        selTop.addView(IconView(this, IconView.CLOSE).apply {
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
            setOnClickListener { exitSel() }
        })
        root.addView(selTop)

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(14))
        }
        tabLabels.forEachIndexed { i, label ->
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    tab = i
                    styleTabs()
                    refresh()
                }
            }
            val t = TextView(this).apply {
                text = label
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, dp(6))
            }
            val b = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(3))
            }
            col.addView(t)
            col.addView(b)
            tabTexts.add(t)
            tabBars.add(b)
            tabs.addView(col)
        }
        root.addView(tabs)
        styleTabs()

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this)
        scroll.addView(list)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        recBottom = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(6))
        }
        rec = RecView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(76), dp(76))
            setOnClickListener { toggleRecording() }
        }
        recBottom.addView(rec)
        root.addView(recBottom)

        barBottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        barBottom.addView(View(this).apply {
            setBackgroundColor(0xFF2A2A2A.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            )
        })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(barItem(IconView.TRASH, "Delete") { doDelete() })
        row.addView(barItem(IconView.SHARE, "Share") { doShare() })
        row.addView(barItem(IconView.EDIT, "Rename") { doRename() })
        row.addView(barItem(IconView.SPARK, "Smart naming") {
            toast("Smart naming needs internet")
        })
        row.addView(barItem(IconView.MORE, "More") { v -> showMore(v) })
        barBottom.addView(row)
        root.addView(barBottom)
        return root
    }

    private fun barItem(kind: Int, label: String, onClick: (View) -> Unit): LinearLayout {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(0, dp(10), 0, dp(8))
        }
        col.setOnClickListener { onClick(col) }
        col.addView(IconView(this, kind).apply {
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
        })
        col.addView(TextView(this).apply {
            text = label
            textSize = 11.5f
            gravity = Gravity.CENTER
            maxLines = 1
            setTextColor(light)
            setPadding(0, dp(4), 0, 0)
        })
        return col
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun styleTabs() {
        for (i in tabTexts.indices) {
            val on = i == tab
            tabTexts[i].setTextColor(if (on) Color.WHITE else tabGray)
            tabTexts[i].typeface = if (on) medium() else Typeface.DEFAULT
            tabBars[i].setBackgroundColor(if (on) red else Color.TRANSPARENT)
        }
    }

    private fun applyMode() {
        normalTop.visibility = if (selMode) View.GONE else View.VISIBLE
        selTop.visibility = if (selMode) View.VISIBLE else View.GONE
        recBottom.visibility = if (selMode) View.GONE else View.VISIBLE
        barBottom.visibility = if (selMode) View.VISIBLE else View.GONE
    }

    private fun enterSel(path: String?) {
        stopPlayer()
        expanded = null
        selMode = true
      
