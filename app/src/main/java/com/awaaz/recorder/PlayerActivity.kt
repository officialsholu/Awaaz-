package com.awaaz.recorder

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.sqrt

class PlayerActivity : AppCompatActivity() {

    private val red = 0xFFE8504A.toInt()
    private val gray = 0xFF9A9A9A.toInt()
    private val handler = Handler(Looper.getMainLooper())
    private val marks = ArrayList<Long>()
    private var levels = FloatArray(0)
    private var file: File? = null
    private var player: MediaPlayer? = null
    private var duration = 0
    private var dragging = false

    private lateinit var wave: WaveView
    private lateinit var timeBig: TextView
    private lateinit var curT: TextView
    private lateinit var totT: TextView
    private lateinit var seek: SeekBar
    private lateinit var playBtn: RingIcon
    private lateinit var flagBtn: RingIcon

    private val ticker = object : Runnable {
        override fun run() {
            try {
                val p = player
                if (p != null) {
                    val pos = p.currentPosition
                    if (!dragging) seek.progress = pos
                    timeBig.text = fmt(pos)
                    curT.text = fmt(pos)
                    wave.setTime(pos / 1000f)
                    val want = if (p.isPlaying) RingIcon.PAUSE else RingIcon.PLAY
                    if (playBtn.kind != want) {
                        playBtn.kind = want
                        playBtn.invalidate()
                    }
                }
            } catch (_: Throwable) {
            }
            handler.postDelayed(this, 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        val path = intent.getStringExtra("path")
        if (path == null) {
            finish()
            return
        }
        file = File(path)
        setContentView(buildUi())
        loadAll(File(path))
    }

    override fun onPause() {
        super.onPause()
        try { player?.pause() } catch (_: Exception) {}
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        try { player?.release() } catch (_: Exception) {}
        player = null
        super.onDestroy()
    }

    private fun loadAll(f: File) {
        Thread {
            try {
                val lv = readLevels(f)
                val mk = readMarks(f)
                runOnUiThread {
                    levels = lv
                    marks.clear()
                    marks.addAll(mk)
                    wave.marks = ArrayList(marks)
                    wave.invalidate()
                }
            } catch (_: Throwable) {
            }
        }.start()
        try {
            val p = MediaPlayer()
            p.setDataSource(f.absolutePath)
            p.prepare()
            duration = p.duration
            seek.max = duration
            totT.text = fmt(duration)
            p.setOnCompletionListener {
                playBtn.kind = RingIcon.PLAY
                playBtn.invalidate()
            }
            player = p
            handler.post(ticker)
        } catch (_: Exception) {
            Toast.makeText(this, "Cannot open this recording", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun readLevels(f: File): FloatArray {
        val raf = RandomAccessFile(f, "r")
        try {
            val samples = ((raf.length() - 44) / 2).toInt().coerceAtLeast(0)
            val seg = RecorderService.RATE / RecorderService.LEVELS_PER_SEC
            val n = samples / seg
            val out = FloatArray(n)
            raf.seek(44)
            val buf = ByteArray(seg * 2)
            for (i in 0 until n) {
                raf.readFully(buf)
                var peak = 0
                var k = 0
                while (k < buf.size) {
                    val lo = buf[k].toInt() and 0xFF
                    val hi = buf[k + 1].toInt()
                    val v = ((hi shl 8) or lo).toShort().toInt()
                    val a = if (v < 0) -v else v
                    if (a > peak) peak = a
                    k += 2
                }
                out[i] = sqrt(peak / 32768.0).toFloat()
            }
            return out
        } finally {
            raf.close()
        }
    }

    private fun readMarks(f: File): List<Long> {
        val mf = File(f.path + ".marks")
        if (!mf.exists()) return emptyList()
        return mf.readText().split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    private fun togglePlay() {
        val p = player ?: return
        try {
            if (p.isPlaying) p.pause() else p.start()
        } catch (_: Exception) {
        }
    }

    private fun skip(ms: Int) {
        val p = player ?: return
        val target = (p.currentPosition + ms).coerceIn(0, duration)
        try { p.seekTo(target) } catch (_: Exception) {}
    }

    private fun addMark() {
        val p = player ?: return
        marks.add(p.currentPosition.toLong())
        marks.sort()
        wave.marks = ArrayList(marks)
        wave.invalidate()
        val f = file ?: return
        try {
            File(f.path + ".marks").writeText(marks.joinToString(","))
        } catch (_: Exception) {
        }
        Toast.makeText(this, "Mark added", Toast.LENGTH_SHORT).show()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun medium(): Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    private fun fmt(ms: Int): String {
        val s = ms / 1000
        return "%02d:%02d".format(s / 60, s % 60)
    }

    private fun tv(s: String, size: Float, color: Int): TextView =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
        }

    private fun skipBtn(label: String, delta: Int): TextView {
        val t = tv(label, 13f, Color.WHITE)
        t.gravity = Gravity.CENTER
        t.typeface = medium()
        t.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setStroke(dp(2), 0xFF555555.toInt())
        }
        t.layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply {
            setMargins(dp(14), 0, dp(14), 0)
        }
        t.setOnClickListener { skip(delta) }
        return t
    }

    private fun buildUi(): View {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.BLACK)
        root.setPadding(dp(16), dp(28), dp(16), dp(20))

        val top = LinearLayout(this)
        top.gravity = Gravity.CENTER_VERTICAL
        val back = Icon2View(this, Icon2View.BACK)
        back.layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
        back.setOnClickListener { finish() }
        top.addView(back)
        root.addView(top)

        val tabs = LinearLayout(this)
        tabs.orientation = LinearLayout.HORIZONTAL
        tabs.gravity = Gravity.CENTER
        tabs.setPadding(0, dp(12), 0, dp(14))
        val mark = tv("Mark", 16f, Color.WHITE)
        mark.typeface = medium()
        mark.setPadding(dp(24), dp(6), dp(24), dp(6))
        val trans = tv("Transcription", 16f, gray)
        trans.setPadding(dp(24), dp(6), dp(24), dp(6))
        trans.setOnClickListener {
            Toast.makeText(this, "Needs internet", Toast.LENGTH_SHORT).show()
        }
        tabs.addView(mark)
        tabs.addView(trans)
        root.addView(tabs)

        wave = WaveView(this)
        wave.levelAt = { i -> if (i >= 0 && i < levels.size) levels[i] else 0f }
        wave.levelCount = { levels.size }
        root.addView(
            wave,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        timeBig = tv("00:00", 44f, 0xFFE6E6E6.toInt())
        timeBig.gravity = Gravity.CENTER
        timeBig.typeface = medium()
        timeBig.setPadding(0, dp(14), 0, dp(14))
        root.addView(timeBig)

        root.addView(View(this), LinearLayout.LayoutParams(0, 0, 1f))

        val seekRow = LinearLayout(this)
        seekRow.orientation = LinearLayout.HORIZONTAL
        seekRow.gravity = Gravity.CENTER_VERTICAL
        curT = tv("00:00", 13f, gray)
        totT = tv("00:00", 13f, gray)
        seek = SeekBar(this)
        seek.progressTintList = ColorStateList.valueOf(red)
        seek.progressBackgroundTintList = ColorStateList.valueOf(0xFF4A4A4A.toInt())
        seek.thumbTintList = ColorStateList.valueOf(red)
        seek.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ).apply { setMargins(dp(10), 0, dp(10), 0) }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, pr: Int, fromUser: Boolean) {
                if (fromUser) {
                    curT.text = fmt(pr)
                    wave.setTime(pr / 1000f)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {
                dragging = true
            }
            override fun onStopTrackingTouch(sb: SeekBar?) {
                dragging = false
                try { player?.seekTo(seek.progress) } catch (_: Exception) {}
            }
        })
        seekRow.addView(curT)
        seekRow.addView(seek)
        seekRow.addView(totT)
        root.addView(seekRow)

        val ctrl = LinearLayout(this)
        ctrl.orientation = LinearLayout.HORIZONTAL
        ctrl.gravity = Gravity.CENTER
        ctrl.setPadding(0, dp(18), 0, 0)
        flagBtn = RingIcon(this, RingIcon.FLAG, false)
        flagBtn.layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
        flagBtn.setOnClickListener { addMark() }
        playBtn = RingIcon(this, RingIcon.PLAY, true)
        playBtn.layoutParams = LinearLayout.LayoutParams(dp(76), dp(76))
        playBtn.setOnClickListener { togglePlay() }
        ctrl.addView(flagBtn)
        ctrl.addView(skipBtn("-10", -10000))
        ctrl.addView(playBtn)
        ctrl.addView(skipBtn("+10", 10000))
        root.addView(ctrl)

        wave.setTime(0f)
        return root
    }
}
