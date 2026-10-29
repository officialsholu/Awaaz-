package com.awaaz.recorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
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

    private var player: LivePlayer? = null
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
        try { player?.pause() } catch (_: Exception) {}
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
                if (k == IconView.MORE) setOnClickListener {
                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                }
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
        selected.clear()
        if (path != null) selected.add(path)
        applyMode()
        refresh()
    }

    private fun exitSel() {
        popup?.dismiss()
        selMode = false
        selected.clear()
        applyMode()
        refresh()
    }

    private fun updateSelHeader() {
        selCount.text = "${selected.size} selected"
        selAll.state = when {
            selected.isEmpty() -> 0
            shown.isNotEmpty() && selected.size >= shown.size -> 1
            else -> 2
        }
    }

    private fun toggleAll() {
        if (shown.isNotEmpty() && selected.size >= shown.size) {
            selected.clear()
        } else {
            selected.clear()
            shown.forEach { selected.add(it.absolutePath) }
        }
        for ((k, v) in checks) v.state = if (selected.contains(k)) 1 else 0
        updateSelHeader()
    }

    private fun toggle(f: File) {
        val key = f.absolutePath
        if (!selected.add(key)) selected.remove(key)
        checks[key]?.state = if (selected.contains(key)) 1 else 0
        updateSelHeader()
    }

    private fun selectedFiles(): List<File> = shown.filter { selected.contains(it.absolutePath) }

    private fun doDelete() {
        val files = selectedFiles()
        if (files.isEmpty()) {
            toast("Select a recording")
            return
        }
        deleteFiles(files)
    }

    private fun deleteFiles(files: List<File>) {
        if (files.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage("Delete ${files.size} recording(s)?")
            .setPositiveButton(R.string.delete) { _, _ ->
                for (f in files) {
                    if (playingPath == f.absolutePath) stopPlayer()
                    if (expanded == f.absolutePath) expanded = null
                    f.delete()
                }
                exitSel()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun doShare() {
        val files = selectedFiles()
        if (files.isEmpty()) {
            toast("Select a recording")
            return
        }
        try {
            val uris = ArrayList<Uri>()
            for (f in files) {
                uris.add(FileProvider.getUriForFile(this, "$packageName.fileprovider", f))
            }
            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM, uris
                )
            }
            intent.type = "audio/*"
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: Throwable) {
            toast("Error: " + e.message)
        }
    }

    private fun doRename() {
        val files = selectedFiles()
        if (files.size != 1) {
            toast("Select one recording")
            return
        }
        renameFile(files[0])
    }

    private fun renameFile(f: File) {
        val et = EditText(this)
        et.setText(title(f))
        et.setSelection(et.text.length)
        et.setSingleLine(true)
        val box = FrameLayout(this)
        box.setPadding(dp(20), dp(8), dp(20), 0)
        box.addView(et)
        AlertDialog.Builder(this)
            .setTitle("Rename")
            .setView(box)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val clean = et.text.toString().trim()
                    .replace(Regex("[\\\\/:*?\"<>|]"), "")
                if (clean.isEmpty()) return@setPositiveButton
                val dest = File(f.parentFile, "rec_$clean.wav")
                if (dest.exists() && dest.absolutePath != f.absolutePath) {
                    toast("Name already exists")
                } else {
                    if (playingPath == f.absolutePath) stopPlayer()
                    if (expanded == f.absolutePath) expanded = null
                    f.renameTo(dest)
                    exitSel()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showMore(anchor: View) {
        popup?.dismiss()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(0xFF262626.toInt())
            }
            setPadding(0, dp(4), 0, dp(4))
        }
        fun item(label: String, action: () -> Unit) {
            box.addView(TextView(this).apply {
                text = label
                textSize = 15f
                setTextColor(Color.WHITE)
                setPadding(dp(20), dp(14), dp(20), dp(14))
                setOnClickListener {
                    popup?.dismiss()
                    action()
                }
            })
        }
        item("Trim") { toast("Trim: coming soon") }
        item("Set as ringtone") { toast("Set as ringtone: coming soon") }
        item("File info") { showInfo() }
        item("Move to Private space") { toast("Private space: coming soon") }
        val w = PopupWindow(box, dp(230), LinearLayout.LayoutParams.WRAP_CONTENT, true)
        w.elevation = dp(8).toFloat()
        w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup = w
        w.showAtLocation(window.decorView, Gravity.BOTTOM or Gravity.END, dp(12), dp(88))
    }

    private fun showInfo() {
        val files = selectedFiles()
        if (files.size != 1) {
            toast("Select one recording")
            return
        }
        val f = files[0]
        val sec = ((f.length() - 44).coerceAtLeast(0) / (RecorderService.RATE * 2L)).toInt()
        val mb = f.length() / 1048576.0
        val date = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US).format(Date(f.lastModified()))
        val msg = "Name: " + title(f) +
            "\nDuration: %02d:%02d".format(sec / 60, sec % 60) +
            "\nSize: %.2f MB".format(mb) +
            "\nDate: " + date +
            "\nPath: " + f.absolutePath
        AlertDialog.Builder(this)
            .setTitle("File info")
            .setMessage(msg)
            .setPositiveButton(android.R.string.ok, null)
            .show()
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
            expanded = null
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

    private fun fmt(ms: Int): String {
        val sec = ms / 1000
        return "%02d:%02d".format(sec / 60, sec % 60)
    }

    private fun isPlayingNow(): Boolean =
        try { player?.isPlaying == true } catch (_: Exception) { false }

    private fun openPlayer(path: String) {
        stopPlayer()
        startActivity(Intent(this, PlayerActivity::class.java).putExtra("path", path))
    }

    private fun refresh() {
        list.removeAllViews()
        checks.clear()
        seek = null
        curT = null
        totT = null
        val files = if (tab == 2) emptyList() else
            RecorderService.dir(this).listFiles()
                ?.filter { it.extension == "wav" }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
        shown = files
        selected.retainAll(files.map { it.absolutePath }.toSet())
        if (selMode) updateSelHeader()
        if (files.isEmpty()) {
            list.addView(TextView(this).apply {
                text = getString(R.string.no_recordings)
                setTextColor(gray)
                setPadding(dp(8), dp(16), dp(8), dp(8))
            })
            return
        }
        for (f in files) {
            val path = f.absolutePath
            val isOpen = !selMode && expanded == path

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(cardBg)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, dp(8)) }
            }

            val head = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                if (selMode) setPadding(dp(16), dp(12), dp(16), dp(12))
                else setPadding(dp(18), dp(12), dp(8), dp(12))
            }
            if (selMode) {
                val cv = CheckView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(26), dp(26)).apply {
                        marginEnd = dp(16)
                    }
                    state = if (selected.contains(path)) 1 else 0
                }
                checks[path] = cv
                head.addView(cv)
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
            head.addView(texts)
            if (!selMode) {
                val nowPlaying = playingPath == path && isPlayingNow()
                head.addView(IconView(this, if (nowPlaying) IconView.PAUSE else IconView.PLAY).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                    setOnClickListener { play(f) }
                })
                head.setOnClickListener { openPlayer(path) }
            } else {
                head.setOnClickListener { toggle(f) }
            }
            head.setOnLongClickListener {
                if (!selMode) enterSel(path)
                true
            }
            card.addView(head)
            if (isOpen) card.addView(buildPanel(f))
            list.addView(card)
        }
    }

    private fun buildPanel(f: File): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), 0, dp(18), dp(12))
        }
        val p = player
        val sameFile = p != null && playingPath == f.absolutePath
        val total = ((f.length() - 44).coerceAtLeast(0) * 1000L / (RecorderService.RATE * 2L)).toInt()
        val cur = if (sameFile && p != null) {
            try { p.currentPosition } catch (_: Exception) { 0 }
        } else 0

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        curT = TextView(this).apply {
            text = fmt(cur)
            textSize = 13f
            setTextColor(gray)
        }
        totT = TextView(this).apply {
            text = fmt(total)
            textSize = 13f
            setTextColor(gray)
        }
        val s = SeekBar(this).apply {
            max = total.coerceAtLeast(1)
            progress = cur.coerceAtMost(max)
            progressTintList = ColorStateList.valueOf(red)
            progressBackgroundTintList = ColorStateList.valueOf(0xFF4A4A4A.toInt())
            thumbTintList = ColorStateList.valueOf(red)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { setMargins(dp(10), 0, dp(10), 0) }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, pr: Int, fromUser: Boolean) {
                    if (fromUser) {
                        try { player?.seekTo(pr) } catch (_: Exception) {}
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        seek = s
        bar.addView(curT)
        bar.addView(s)
        bar.addView(totT)
        panel.addView(bar)

        val acts = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        acts.addView(barItem(IconView.TRASH, "Delete") { deleteFiles(listOf(f)) })
        acts.addView(barItem(IconView.EDIT, "Rename") { renameFile(f) })
        acts.addView(barItem(IconView.SLIDERS, "Echo") {
            Settings.showEchoMenu(this) { refresh() }
        })
        acts.addView(barItem(IconView.SPEAKER, "Speaker") { toggleSpeaker() })
        panel.addView(acts)
        return panel
    }

    private fun toggleSpeaker() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.isSpeakerphoneOn = !am.isSpeakerphoneOn
        toast(if (am.isSpeakerphoneOn) "Speaker on" else "Speaker off")
    }

    private fun updatePlayback() {
        val s = seek ?: return
        val ct = curT ?: return
        val tt = totT ?: return
        val p = player ?: return
        if (playingPath == null || playingPath != expanded) return
        try {
            val d = p.duration.coerceAtLeast(1)
            if (s.max != d) s.max = d
            val pos = p.currentPosition
            s.progress = pos
            ct.text = fmt(pos)
            tt.text = fmt(d)
        } catch (_: Exception) {
        }
    }

    private fun stopPlayer() {
        try { player?.release() } catch (_: Exception) {}
        player = null
        playingPath = null
    }

    private fun play(f: File) {
        val p = player
        if (p != null && playingPath == f.absolutePath) {
            if (p.isPlaying) p.pause() else p.start()
            refresh()
            return
        }
        stopPlayer()
        try {
            val lp = LivePlayer(this, f)
            lp.onDone = {
                runOnUiThread {
                    stopPlayer()
                    expanded = null
                    refresh()
                }
            }
            lp.start()
            player = lp
            playingPath = f.absolutePath
            expanded = f.absolutePath
        } catch (_: Exception) {
            stopPlayer()
            expanded = null
            toast("Cannot play this recording")
        }
        refresh()
    }
}
