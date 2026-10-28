package com.awaaz.recorder

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.io.File
import java.io.RandomAccessFile

class LivePlayer(private val ctx: Context, private val file: File) {
    private val rate = RecorderService.RATE
    private val totalSamples: Long = (file.length() - 44).coerceAtLeast(0L) / 2
    private val dsp = Dsp(Settings.preset(ctx), rate)

    @Volatile private var playing = false
    @Volatile private var stopFlag = false
    @Volatile private var posSamples = 0L
    @Volatile private var seekReq = -1L
    private var thread: Thread? = null

    var onDone: (() -> Unit)? = null

    val duration: Int get() = (totalSamples * 1000 / rate).toInt()
    val currentPosition: Int get() = (posSamples * 1000 / rate).toInt()
    val isPlaying: Boolean get() = playing

    fun start() {
        if (posSamples >= totalSamples) seekReq = 0L
        playing = true
        if (thread == null) {
            thread = Thread { loop() }.also { it.start() }
        }
    }

    fun pause() {
        playing = false
    }

    fun seekTo(ms: Int) {
        seekReq = (ms.toLong() * rate / 1000).coerceIn(0L, totalSamples)
    }

    fun release() {
        stopFlag = true
        playing = false
        thread?.join(800)
        thread = null
    }

    private fun loop() {
        val raf = RandomAccessFile(file, "r")
        val minBuf = AudioTrack.getMinBufferSize(
            rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        val inBuf = ByteArray(2048)
        val outBuf = ByteArray(2048)
        var pos = posSamples
        var trackOn = false
        var lastPrefs = 0L
        try {
            while (!stopFlag) {
                val req = seekReq
                if (req >= 0) {
                    seekReq = -1L
                    pos = req
                    posSamples = pos
                }
                if (!playing) {
                    if (trackOn) {
                        track.pause()
                        track.flush()
                        trackOn = false
                    }
                    Thread.sleep(30)
                    continue
                }
                if (!trackOn) {
                    track.play()
                    trackOn = true
                }
                if (pos >= totalSamples) {
                    playing = false
                    onDone?.invoke()
                    continue
                }
                val now = System.currentTimeMillis()
                if (now - lastPrefs > 100) {
                    lastPrefs = now
                    dsp.setPreset(Settings.preset(ctx))
                }
                val want = minOf(1024L, totalSamples - pos).toInt()
                raf.seek(44L + pos * 2)
                raf.readFully(inBuf, 0, want * 2)
                for (i in 0 until want) {
                    val lo = inBuf[2 * i].toInt() and 0xFF
                    val hi = inBuf[2 * i + 1].toInt()
                    val x = ((hi shl 8) or lo).toShort() / 32768f
                    val y = dsp.process(x)
                    val v = (y * 32767f).toInt().coerceIn(-32768, 32767)
                    outBuf[2 * i] = (v and 0xFF).toByte()
                    outBuf[2 * i + 1] = (v shr 8).toByte()
                }
                track.write(outBuf, 0, want * 2)
                pos += want
                posSamples = pos
            }
        } catch (_: Throwable) {
        } finally {
            try { track.stop() } catch (_: Exception) {}
            track.release()
            raf.close()
        }
    }
}
