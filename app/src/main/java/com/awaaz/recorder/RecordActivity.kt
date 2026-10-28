package com.awaaz.recorder

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RecordActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wave: WaveView
    private lateinit var timeText: TextView
    private lateinit var pauseBtn: RingIcon
    private lateinit var note: TextView
    private val born = System.currentTimeMillis()
    private var sawRecording = false
    private var lastMs = -1L
    private var lastReal = 0L
    private var shownPaused = false
    private var tabIdx = 0
    private val tabTexts = ArrayList<TextView>()
    private val tabBars = ArrayList<View>()

    private val tick = object : Runnable {
        override fun run() {
            try {
                val r = RecorderService.recording
                if (r) sawRecording = true
                val now = System.currentTimeMillis()
                val ms = RecorderService.elapsedMs
                if (ms != lastMs) {
                    lastMs = ms
                    lastReal = now
                }
                var est = ms
                if (r && !RecorderService.paused) est = ms + minOf(now - lastReal, 120L)
                val sec = est / 1000
                timeText.text = if (sec >= 3600)
                    "%d:%02d:%02d".format(sec / 3600, (sec % 3600) / 60, sec % 60)
                else
                    "%02d:%02d".format(sec / 60, sec % 60)
                wave.marks = ArrayList(RecorderService.marks)
                wave.setTime(est / 1000f)
                val p = RecorderService.paused
                if (p != shownPaused) {
                    shownPaused = p
                    pauseBtn.kind = if (p) RingIcon.PLAY else RingIcon.PAUSE
                    pauseBtn.invalidate()
                }
                if (sawRecording && !r) {
                    finish()
                    return
                }
                if (!sawRecording && now - born > 4000) {
                    finish()
                    return
                }
            } catch (_: Throwable) {
            }
            handler.postDelayed(this, 50)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun medium() = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    private fun styleTabs() {
        for (i in tabTexts.indices) {
            val on = i == tabIdx
            tabTexts[i].setTextColor(if (on) Color.WHITE else 0xFF8A8A8A.toInt())
            tabTexts[i].typeface = if (on) medium() else Typeface.DEFAULT
            tabBars[i].setBackgroundColor(if (on) 0xFFE8504A.toInt() else Color.TRANSPARENT)
        }
        note.visibility = if (tabIdx == 1) View.VISIBLE else View.GONE
    }

    private fun slot(v: View): LinearLayout {
        val box = LinearLayout(this)
        box.gravity = Gravity.CENTER
        box.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        box.addView(v)
        return box
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(0, dp(28), 0, dp(24))
        }

        val head = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(10), dp(18), dp(14))
        }
        head.addView(Icon2View(this, Icon2View.BACK).apply {
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
            setOnClickListener { finish() }
        })
        root.addView(head)

        note = TextView(this).apply {
            text = "Needs internet"
            textSize = 14f
            setTextColor(0xFF9A9A9A.toInt())
            setPadding(dp(22), dp(12), dp(22), 0)
            visibility = View.GONE
        }

        val tabs = LinearLayout(this).apply {
            setPadding(dp(16), 0, dp(16), dp(14))
        }
        listOf("Mark", "Transcription").forEachIndexed { i, label ->
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    tabIdx = i
                    styleTabs()
                }
            }
            val t = TextView(this).apply {
                text = label
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, dp(6))
            }
            val b = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(26), dp(3))
            }
            col.addView(t)
            col.addView(b)
            tabTexts.add(t)
            tabBars.add(b)
            tabs.addView(col)
        }
        root.addView(tabs)

        wave = WaveView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            levelAt = { i ->
                val a = RecorderService.levels
                if (i in a.indices) a[i] else 0f
            }
            levelCount = { RecorderService.levelCount }
        }
        root.addView(wave)

        timeText = TextView(this).apply {
            text = "00:00"
            textSize = 44f
            gravity = Gravity.CENTER
            setTextColor(0xFFE6E6E6.toInt())
            typeface = medium()
            setPadding(0, dp(14), 0, dp(14))
        }
        root.addView(timeText)

        root.addView(View(this).apply {
            setBackgroundColor(0xFF2A2A2A.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            ).apply { setMargins(dp(22), 0, dp(22), 0) }
        })

        val ideas = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(22), dp(14), dp(22), dp(6))
        }
        ideas.addView(Icon2View(this, Icon2View.PENCIL).apply {
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
        })
        ideas.addView(TextView(this).apply {
            text = "Record ideas"
            textSize = 15f
            setTextColor(0xFFCFCFCF.toInt())
            setPadding(dp(14), 0, 0, 0)
        })
        root.addView(ideas)
        root.addView(note)

        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        })

        val bottom = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
        }
        val flag = RingIcon(this, RingIcon.FLAG, false).apply {
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
            setOnClickListener {
                if (RecorderService.recording) RecorderService.marks.add(RecorderService.elapsedMs)
            }
        }
        pauseBtn = RingIcon(this, RingIcon.PAUSE, true).apply {
            layoutParams = LinearLayout.LayoutParams(dp(68), dp(68))
            setOnClickListener { RecorderService.paused = !RecorderService.paused }
        }
        val done = RingIcon(this, RingIcon.CHECK, false).apply {
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
            setOnClickListener {
                startService(
                    Intent(this@RecordActivity, RecorderService::class.java)
                        .setAction(RecorderService.ACTION_STOP)
                )
            }
        }
        bottom.addView(slot(flag))
        bottom.addView(slot(pauseBtn))
        bottom.addView(slot(done))
        root.addView(bottom)

        styleTabs()
        return root
    }
}
