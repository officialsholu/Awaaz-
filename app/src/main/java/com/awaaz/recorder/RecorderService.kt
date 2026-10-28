package com.awaaz.recorder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class RecorderService : Service() {

    companion object {
        const val ACTION_STOP = "stop"
        const val RATE = 44100
        const val LEVELS_PER_SEC = 15
        @Volatile var recording = false
        @Volatile var paused = false
        @Volatile var startTime = 0L
        @Volatile var elapsedMs = 0L
        @Volatile var levels = FloatArray(4096)
        @Volatile var levelCount = 0
        val marks: MutableList<Long> = Collections.synchronizedList(ArrayList<Long>())

        fun dir(ctx: Context): File =
            File(ctx.getExternalFilesDir(null), "Recordings").apply { mkdirs() }

        fun addLevel(v: Float) {
            var arr = levels
            val c = levelCount
            if (c >= arr.size) {
                arr = arr.copyOf(arr.size * 2)
                levels = arr
            }
            arr[c] = v
            levelCount = c + 1
        }
    }

    private var worker: Thread? = null
    private var wake: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRec()
            return START_NOT_STICKY
        }
        if (!recording) startRec(intent?.getStringExtra("cat") ?: "other")
        return START_NOT_STICKY
    }

    private fun startRec(cat: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("rec", "Recording", NotificationManager.IMPORTANCE_LOW)
        )
        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, RecorderService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val openPi = PendingIntent.getActivity(
            this, 2, Intent(this, RecordActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, "rec")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.recording))
            .setContentIntent(openPi)
            .setOngoing(true)
            .addAction(0, getString(R.string.stop), stopPi)
            .build()
        ServiceCompat.startForeground(
            this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir(this), "${cat}_$stamp.wav")
        levels = FloatArray(4096)
        levelCount = 0
        elapsedMs = 0L
        paused = false
        marks.clear()
        recording = true
        startTime = System.currentTimeMillis()
        wake = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "awaaz:rec")
            .apply { acquire(6 * 60 * 60 * 1000L) }
        worker = Thread { record(file) }.also { it.start() }
        try {
            startActivity(
                Intent(this, RecordActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }

    @Suppress("MissingPermission")
    private fun record(file: File) {
        val min = AudioRecord.getMinBufferSize(
            RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val ar = AudioRecord(
            MediaRecorder.AudioSource.MIC, RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, maxOf(min, RATE)
        )
        if (ar.state != AudioRecord.STATE_INITIALIZED) {
            recording = false
            stopSelf()
            return
        }
        var ns: NoiseSuppressor? = null
        var agc: AutomaticGainControl? = null
        try {
            if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(ar.audioSessionId)
                ns?.enabled = true
            }
        } catch (_: Exception) {}
        try {
            if (AutomaticGainControl.isAvailable()) {
                agc = AutomaticGainControl.create(ar.audioSessionId)
                agc?.enabled = true
            }
        } catch (_: Exception) {}

        val dsp = Dsp(Settings.preset(this), RATE)
        val raf = RandomAccessFile(file, "rw")
        raf.write(header(0))
        val buf = ShortArray(4096)
        val bytes = ByteArray(8192)
        var total = 0L
        var lastFix = System.currentTimeMillis()
        val seg = RATE / LEVELS_PER_SEC
        var segPeak = 0
        var segN = 0
        ar.startRecording()
        while (recording) {
            val n = ar.read(buf, 0, buf.size)
            if (n < 0) break
            if (n == 0) continue
            if (paused) continue
            for (i in 0 until n) {
                val raw = buf[i].toInt()
                val a = if (raw < 0) -raw else raw
                if (a > segPeak) segPeak = a
                segN++
                if (segN >= seg) {
                    addLevel(Math.sqrt(segPeak / 32768.0).toFloat())
                    segPeak = 0
                    segN = 0
                }
                val y = dsp.process(raw / 32768f)
                val v = (y * 32767f).toInt().coerceIn(-32768, 32767)
                bytes[2 * i] = (v and 0xFF).toByte()
                bytes[2 * i + 1] = (v shr 8).toByte()
            }
            raf.write(bytes, 0, n * 2)
            total += n * 2
            elapsedMs = total * 1000L / (RATE * 2L)
            if (System.currentTimeMillis() - lastFix > 3000) {
                fixHeader(raf, total)
                lastFix = System.currentTimeMillis()
            }
        }
        try { ar.stop() } catch (_: Exception) {}
        ar.release()
        ns?.release()
        agc?.release()
        fixHeader(raf, total)
        raf.close()
        if (marks.isNotEmpty()) {
            try {
                val text = synchronized(marks) { marks.joinToString(",") }
                File(file.path + ".marks").writeText(text)
            } catch (_: Exception) {}
        }
    }

    private fun fixHeader(raf: RandomAccessFile, len: Long) {
        raf.seek(0)
        raf.write(header(len))
        raf.seek(44 + len)
    }

    private fun header(len: Long): ByteArray {
        val b = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray())
        b.putInt((36 + len).toInt())
        b.put("WAVE".toByteArray())
        b.put("fmt ".toByteArray())
        b.putInt(16)
        b.putShort(1)
        b.putShort(1)
        b.putInt(RATE)
        b.putInt(RATE * 2)
        b.putShort(2)
        b.putShort(16)
        b.put("data".toByteArray())
        b.putInt(len.toInt())
        return b.array()
    }

    private fun stopRec() {
        recording = false
        paused = false
        worker?.join(2500)
        wake?.let { if (it.isHeld) it.release() }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (recording) {
            recording = false
            worker?.join(2500)
        }
        wake?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}
