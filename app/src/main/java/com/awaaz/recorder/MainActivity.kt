package com.awaaz.recorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    private val handler = Handler(Looper.getMainLooper())

    private var ready = false
    private var tab = 0
    private lateinit var list: LinearLayout
    private lateinit var rec: RecView
    private val tabLabels = listOf("All", "Recorder", "Phone")
    private val tabTexts = ArrayList<TextView>()
    private val tabBars = ArrayList<View>()

    private var player: MediaPlayer? = null
    private var playingPath: String? = null
    private var wasRecording = false

    private val ticker = object : Runnable {
        override fun run() {
            try {
                val r = RecorderService.recording
                if (wasRecording && !r) refresh()
                wasRecording = r
                rec.recording = r
            } catch (_: Throwable) {
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
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

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(8), dp(4), dp(16))
        }
        top.addView(TextView(this).apply {
            text = "Recorder"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = medium()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        for (k in listOf(IconView.SELECT, IconView.SEARCH, IconView.MORE)) {
            top.addView(IconView(this, k).apply {
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                    marginStart = dp(14)
                }
            })
        }
        root.addView(top)

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

        val bottom = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(6))
        }
        rec = RecView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(76), dp(76))
            setOnClickListener { toggleRecording() }
        }
        bottom.addView(rec)
        root.addView(bottom)
        return root
    }

    private fun styleTabs() {
        for (i in tabTexts.indices) {
            val on = i == tab
            tabTexts[i].setTextColor(if (on) Color.WHITE else tabGray)
            tabTexts[i].typeface = if (on) medium() else Typeface.DEFAULT
            tabBars[i].setBackgroundColor(if (on) red else Color.TRANSPARENT)
        }
    }

    private fun toggleRecording() {
        if (RecorderService.recording) {
            startService(
                Intent(this, RecorderService::class.java).setAction(RecorderService.ACTION_STOP)
            )
            return
        }
        val need = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) need.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = need.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 7)
        } else {
            launchService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 7) {
            val mic = ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (mic) launchService()
            else Toast.makeText(this, R.string.permission_needed, Toast.LENGTH_LONG).show()
        }
    }

    private fun launchService() {
        try {
            stopPlayer()
            val i = Intent(this, RecorderService::class.java).putExtra("cat", "rec")
            ContextCompat.startForegroundService(this, i)
        } catch (e: Throwable) {
            Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun title(f: File): String {
        val n = f.nameWithoutExtension
        return n.substringAfter("_", n)
    }

    private fun sub(f: File): String {
        val sec = ((f.length() - 44).coerceAtLeast(0) / (RecorderService.RATE * 2L)).toInt()
        val dur = "%02d:%02d".format(sec / 60, sec % 60)
        val date = SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(f.lastModified()))
        return "$dur $date"
    }

    private fun refresh() {
        list.removeAllViews()
        val files = if (tab == 2) emptyList() else
            RecorderService.dir(this).listFiles()
                ?.filter { it.extension == "wav" }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
        if (files.isEmpty()) {
            list.addView(TextView(this).apply {
                text = getString(R.string.no_recordings)
                setTextColor(gray)
                setPadding(dp(8), dp(16), dp(8), dp(8))
            })
            return
        }
        for (f in files) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(cardBg)
                }
                setPadding(dp(18), dp(12), dp(8), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, dp(8)) }
            }
            val texts = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            texts.addView(TextView(this).apply {
                text = title(f)
                textSize = 17f
                maxLines = 2
                setTextColor(Color.WHITE)
            })
            texts.addView(TextView(this).apply {
                text = sub(f)
                textSize = 13f
                setTextColor(gray)
            })
            val isPlaying = playingPath == f.absolutePath
            val icon = IconView(this, if (isPlaying) IconView.PAUSE else IconView.PLAY).apply {
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                setOnClickListener { play(f) }
            }
            row.addView(texts)
            row.addView(icon)
            list.addView(row)
        }
    }

    private fun stopPlayer() {
        try { player?.release() } catch (_: Exception) {}
        player = null
        playingPath = null
    }

    private fun play(f: File) {
        val same = playingPath == f.absolutePath
        stopPlayer()
        if (!same) {
            try {
                player = MediaPlayer().apply {
                    setDataSource(f.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        stopPlayer()
                        refresh()
                    }
                }
                playingPath = f.absolutePath
            } catch (_: Exception) {
                stopPlayer()
            }
        }
        refresh()
    }
}
