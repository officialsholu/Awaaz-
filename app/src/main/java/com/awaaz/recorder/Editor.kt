package com.awaaz.recorder

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

object Editor {
    private const val SEG = RecorderService.RATE / RecorderService.LEVELS_PER_SEC

    private fun total(f: File): Long = (f.length() - 44).coerceAtLeast(0L) / 2

    private fun sampleOf(ms: Int): Long = ms.toLong() * RecorderService.RATE / 1000

    private fun header(len: Long): ByteArray {
        val b = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray())
        b.putInt((36 + len).toInt())
        b.put("WAVE".toByteArray())
        b.put("fmt ".toByteArray())
        b.putInt(16)
        b.putShort(1)
        b.putShort(1)
        b.putInt(RecorderService.RATE)
        b.putInt(RecorderService.RATE * 2)
        b.putShort(2)
        b.putShort(16)
        b.put("data".toByteArray())
        b.putInt(len.toInt())
        return b.array()
    }

    // src ke sample ranges ko dst mein copy karta hai
    private fun copyRanges(src: File, dst: File, ranges: List<Pair<Long, Long>>) {
        val dataBytes = ranges.sumOf { it.second - it.first } * 2
        BufferedOutputStream(FileOutputStream(dst)).use { out ->
            out.write(header(dataBytes))
            val raf = RandomAccessFile(src, "r")
            val buf = ByteArray(16384)
            try {
                for ((a, b) in ranges) {
                    var pos = a
                    while (pos < b) {
                        val n = minOf(8192L, b - pos).toInt()
                        raf.seek(44 + pos * 2)
                        raf.readFully(buf, 0, n * 2)
                        out.write(buf, 0, n * 2)
                        pos += n
                    }
                }
            } finally {
                raf.close()
            }
        }
    }

    // Echo settings ke saath poori recording render karta hai
    fun render(src: File, dst: File, p: Preset) {
        val n = total(src)
        val dsp = Dsp(p, RecorderService.RATE)
        BufferedOutputStream(FileOutputStream(dst)).use { out ->
            out.write(header(n * 2))
            val raf = RandomAccessFile(src, "r")
            val inBuf = ByteArray(16384)
            val outBuf = ByteArray(16384)
            try {
                var pos = 0L
                while (pos < n) {
                    val cnt = minOf(8192L, n - pos).toInt()
                    raf.seek(44 + pos * 2)
                    raf.readFully(inBuf, 0, cnt * 2)
                    for (i in 0 until cnt) {
                        val lo = inBuf[2 * i].toInt() and 0xFF
                        val hi = inBuf[2 * i + 1].toInt()
                        val x = ((hi shl 8) or lo).toShort() / 32768f
                        val v = (dsp.process(x) * 32767f).toInt().coerceIn(-32768, 32767)
                        outBuf[2 * i] = (v and 0xFF).toByte()
                        outBuf[2 * i + 1] = (v shr 8).toByte()
                    }
                    out.write(outBuf, 0, cnt * 2)
                    pos += cnt
                }
            } finally {
                raf.close()
            }
        }
    }

    // Music/Echo folder mein save karta hai (Android 10 ya naya)
    fun saveToMusic(ctx: Context, src: File): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        val tmp = File(ctx.cacheDir, "save_tmp.wav")
        render(src, tmp, Settings.preset(ctx))
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, src.nameWithoutExtension + ".wav")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Echo")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        val os = ctx.contentResolver.openOutputStream(uri) ?: return false
        os.use { o -> tmp.inputStream().use { it.copyTo(o) } }
        tmp.delete()
        return true
    }

    // Music/Echo mein saved recordings ki list
    fun savedList(ctx: Context): List<Pair<String, Uri>> {
        val out = ArrayList<Pair<String, Uri>>()
        if (Build.VERSION.SDK_INT < 29) return out
        val proj = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME)
        ctx.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            proj,
            "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("Music/Echo%"),
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                out.add(Pair(c.getString(1), uri))
            }
        }
        return out
    }

    // Automatic: lambi khamoshi (0.6 second se zyada) hata deta hai
    fun cutSilence(src: File, dst: File) {
        val total = total(src)
        val segs = (total / SEG).toInt()
        val loud = BooleanArray(segs)
        val raf = RandomAccessFile(src, "r")
        val buf = ByteArray(SEG * 2)
        try {
            for (i in 0 until segs) {
                raf.seek(44 + i.toLong() * SEG * 2)
                raf.readFully(buf)
                var peak = 0
                var k = 0
                while (k < buf.size) {
                    val v = ((buf[k + 1].toInt() shl 8) or (buf[k].toInt() and 0xFF)).toShort().toInt()
                    val a = if (v < 0) -v else v
                    if (a > peak) peak = a
                    k += 2
                }
                loud[i] = sqrt(peak / 32768.0) >= 0.15
            }
        } finally {
            raf.close()
        }
        val keep = BooleanArray(segs) { true }
        var i = 0
        while (i < segs) {
            if (loud[i]) {
                i++
                continue
            }
            var j = i
            while (j < segs && !loud[j]) j++
            if (j - i >= 9) {
                for (k in i + 3 until j - 3) keep[k] = false
            }
            i = j
        }
        val ranges = ArrayList<Pair<Long, Long>>()
        var s = 0
        while (s < segs) {
            if (!keep[s]) {
                s++
                continue
            }
            var e = s
            while (e < segs && keep[e]) e++
            ranges.add(Pair(s.toLong() * SEG, e.toLong() * SEG))
            s = e
        }
        if (segs.toLong() * SEG < total) ranges.add(Pair(segs.toLong() * SEG, total))
        if (ranges.isEmpty()) ranges.add(Pair(0L, total))
        copyRanges(src, dst, ranges)
    }

    // Manual: current position se pehle ka hissa hatata hai
    fun trimBefore(src: File, dst: File, ms: Int) {
        copyRanges(src, dst, listOf(Pair(sampleOf(ms), total(src))))
    }

    // Manual: current position ke baad ka hissa hatata hai
    fun trimAfter(src: File, dst: File, ms: Int) {
        copyRanges(src, dst, listOf(Pair(0L, sampleOf(ms))))
    }

    private fun open(a: AppCompatActivity, f: File) {
        a.startActivity(Intent(a, PlayerActivity::class.java).putExtra("path", f.absolutePath))
        a.finish()
    }

    fun menu(a: AppCompatActivity, src: File, curMs: Int) {
        val items = arrayOf(
            "Auto: silence cut karo",
            "Yahan tak pehle ka hissa hatao",
            "Yahan se baad ka hissa hatao"
        )
        AlertDialog.Builder(a)
            .setTitle("Trim")
            .setItems(items) { _, w ->
                val tag = if (w == 0) "_cut" else "_trim"
                val out = File(src.parentFile, src.nameWithoutExtension + tag + ".wav")
                Thread {
                    try {
                        when (w) {
                            0 -> cutSilence(src, out)
                            1 -> trimBefore(src, out, curMs)
                            else -> trimAfter(src, out, curMs)
                        }
                        a.runOnUiThread { open(a, out) }
                    } catch (e: Throwable) {
                        a.runOnUiThread {
                            Toast.makeText(a, "Error: " + e.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun save(a: AppCompatActivity, src: File) {
        Toast.makeText(a, "Save ho raha hai...", Toast.LENGTH_SHORT).show()
        Thread {
            val ok = try {
                saveToMusic(a, src)
            } catch (e: Throwable) {
                false
            }
            a.runOnUiThread {
                Toast.makeText(
                    a,
                    if (ok) "Music/Echo mein save ho gaya" else "Save nahi hua",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }
}
