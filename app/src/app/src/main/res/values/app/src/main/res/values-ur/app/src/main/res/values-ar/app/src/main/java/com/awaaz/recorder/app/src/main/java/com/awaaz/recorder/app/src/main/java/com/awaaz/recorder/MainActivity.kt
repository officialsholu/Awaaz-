package com.awaaz.recorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.media.MediaPlayer
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private val cats = listOf("namaz", "quran", "naat", "azan", "other")
    private val catRes = listOf(
        R.string.cat_namaz, R.string.cat_quran, R.string.cat_naat,
        R.string.cat_azan, R.string.cat_other
    )
    private val presetKeys = listOf("clean", "haram", "masjid", "naat")
    private val presetRes = listOf(
        R.string.preset_clean, R.string.preset_haram,
        R.string.preset_masjid, R.string.preset_naat
    )

    private val teal = 0xFF00B5BE.toInt()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var timer: TextView
    private lateinit var recBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var spinner: Spinner
    private lateinit var list: LinearLayout

    private var player: MediaPlayer? = null
    private var playingPath: String? = null
    private var wasRecording = false

    private val ticker = object : Runnable {
        override fun run() {
            val r = RecorderService.recording
            if (r) {
                val s = (System.currentTimeMillis() - RecorderService.startTime) / 1000
                timer.text = "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
            }
            if (wasRecording && !r) {
                timer.text = "0:00:00"
                refresh()
            }
            wasRecording = r
            recBtn.isEnabled = !r
            stopBtn.isEnabled = r
            recBtn.alpha = if (r) 0.5f else 1f
            stopBtn.alpha = if (r) 1f else 0.5f
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        val saved = getSharedPreferences("s", MODE_PRIVATE).getString("lang", null)
        if (saved != null &&
            AppCompatDelegate.getApplicationLocales().toLanguageTags() != saved
        ) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(saved))
        }
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        refresh()
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    override fun onDestroy() {
        stopPlayer()
        super.onDestroy()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun circle(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun round(color: Int) = GradientDrawable().apply {
        cornerRadius = dp(14).toFloat()
        setColor(color)
    }

    private fun bigButton(text: String, size: Int): Button = Button(this).apply {
        this.text = text
        textSize = 18f
        isAllCaps = false
        setTextColor(0xFF000000.toInt())
        background = circle(teal)
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size)).apply {
            setMargins(dp(10), dp(10), dp(10), dp(10))
        }
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1B1D21.toInt())
            setPadding(dp(16), dp(24), dp(16), dp(16))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        top.addView(Button(this).apply {
            text = getString(R.string.language)
            isAllCaps = false
            setOnClickListener { chooseLanguage() }
        })
        root.addView(top)

        spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            catRes.map { getString(it) }
        )
        root.addView(spinner)

        timer = TextView(this).apply {
            text = "0:00:00"
            textSize = 40f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, dp(12), 0, dp(4))
        }
        root.addView(timer)

        val btns = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        recBtn = bigButton(getString(R.string.record), 150)
        stopBtn = bigButton(getString(R.string.stop), 110)
        recBtn.setOnClickListener { startRecording() }
        stopBtn.setOnClickListener {
            startService(
                Intent(this, RecorderService::class.java).setAction(RecorderService.ACTION_STOP)
            )
        }
        btns.addView(recBtn)
        btns.addView(stopBtn)
        root.addView(btns)

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this)
        scroll.addView(list)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )
        return root
    }

    private fun chooseLanguage() {
        val names = arrayOf("English", "اردو", "العربية")
        val tags = arrayOf("en", "ur", "ar")
        AlertDialog.Builder(this)
            .setTitle(R.string.language)
            .setItems(names) { _, which ->
                getSharedPreferences("s", MODE_PRIVATE).edit()
                    .putString("lang", tags[which]).apply()
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(tags[which])
                )
            }
            .show()
    }

    private fun startRecording() {
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
        val i = Intent(this, RecorderService::class.java)
            .putExtra("cat", cats[spinner.selectedItemPosition])
        ContextCompat.startForegroundService(this, i)
    }

    private fun title(f: File): String {
        val parts = f.nameWithoutExtension.split("_")
        val idx = cats.indexOf(parts[0])
        val label = if (idx >= 0) getString(catRes[idx]) else parts[0]
        val d = parts.getOrNull(1)
        val t = parts.getOrNull(2)
        var when_ = ""
        if (d != null && t != null && d.length == 8 && t.length == 6) {
            when_ = "${d.substring(6, 8)}-${d.substring(4, 6)}-${d.substring(0, 4)} " +
                "${t.substring(0, 2)}:${t.substring(2, 4)}"
        }
        val extra = if (parts.size > 3) " (" + parts.drop(3).joinToString(" ") + ")" else ""
        return "$label  $when_$extra"
    }

    private fun refresh() {
        list.removeAllViews()
        val files = RecorderService.dir(this).listFiles()
            ?.filter { it.extension == "wav" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
        if (files.isEmpty()) {
            list.addView(TextView(this).apply {
                text = getString(R.string.no_recordings)
                setTextColor(0xFF9AA0A6.toInt())
                setPadding(dp(8), dp(16), dp(8), dp(8))
            })
            return
        }
        for (f in files) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = round(0xFF2A2D33.toInt())
                setPadding(dp(12), dp(10), dp(12), dp(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, dp(6), 0, dp(6)) }
            }
            row.addView(TextView(this).apply {
                text = title(f)
                textSize = 16f
                setTextColor(0xFFFFFFFF.toInt())
            })
            val acts = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            acts.addView(smallBtn(getString(R.string.play)) { play(f) })
            acts.addView(smallBtn(getString(R.string.enhance)) { enhance(f) })
            acts.addView(smallBtn(getString(R.string.delete)) { confirmDelete(f) })
            row.addView(acts)
            list.addView(row)
        }
    }

    private fun smallBtn(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 13f
        setOnClickListener { onClick() }
    }

    private fun stopPlayer() {
        try { player?.release() } catch (_: Exception) {}
        player = null
        playingPath = null
    }

    private fun play(f: File) {
        val same = playingPath == f.absolutePath
        stopPlayer()
        if (same) return
        try {
            player = MediaPlayer().apply {
                setDataSource(f.absolutePath)
                prepare()
                start()
                setOnCompletionListener { stopPlayer() }
            }
            playingPath = f.absolutePath
        } catch (_: Exception) {
            stopPlayer()
        }
    }

    private fun enhance(f: File) {
        AlertDialog.Builder(this)
            .setTitle(R.string.enhance)
            .setItems(presetRes.map { getString(it) }.toTypedArray()) { _, which ->
                Toast.makeText(this, R.string.processing, Toast.LENGTH_SHORT).show()
                Thread {
                    try {
                        val out = File(
                            f.parentFile,
                            f.nameWithoutExtension + "_" + presetKeys[which] + ".wav"
                        )
                        Effects.process(f, out, Effects.byName(presetKeys[which]))
                        runOnUiThread {
                            Toast.makeText(this, R.string.done, Toast.LENGTH_SHORT).show()
                            refresh()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
            }
            .show()
    }

    private fun confirmDelete(f: File) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(title(f))
            .setPositiveButton(R.string.delete) { _, _ ->
                if (playingPath == f.absolutePath) stopPlayer()
                f.delete()
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
