package com.awaaz.recorder

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.tanh

data class Preset(
    val echoOn: Boolean,
    val echoVolume: Float,
    val echoDelayMs: Int,
    val echoRepeat: Float,
    val reverbMix: Float,
    val room: Float,
    val warmth: Float,
    val clarity: Float
)

object Effects {

    val CLEAN = Preset(false, 0f, 0, 0f, 0f, 0f, 0.10f, 0.20f)
    val HARAM = Preset(true, 0.22f, 190, 0.35f, 0.45f, 0.95f, 0.30f, 0.25f)
    val MASJID = Preset(true, 0.18f, 130, 0.30f, 0.35f, 0.70f, 0.25f, 0.20f)
    val NAAT = Preset(true, 0.30f, 260, 0.42f, 0.30f, 0.60f, 0.35f, 0.30f)

    fun byName(name: String): Preset = when (name) {
        "haram" -> HARAM
        "masjid" -> MASJID
        "naat" -> NAAT
        else -> CLEAN
    }

    // Settings screen se aane wali values ke liye
    fun custom(echoOn: Boolean, echoVolume: Float, echoDelayMs: Int, reverbMix: Float): Preset =
        Preset(echoOn, echoVolume, echoDelayMs, 0.35f, reverbMix, 0.7f, 0.25f, 0.2f)

    private fun limit(v: Float): Float {
        val a = abs(v)
        return if (a <= 0.8f) v else sign(v) * (0.8f + 0.2f * tanh((a - 0.8f) / 0.2f))
    }

    private fun header(len: Long, rate: Int): ByteArray {
        val b = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray())
        b.putInt((36 + len).toInt())
        b.put("WAVE".toByteArray())
        b.put("fmt ".toByteArray())
        b.putInt(16)
        b.putShort(1)
        b.putShort(1)
        b.putInt(rate)
        b.putInt(rate * 2)
        b.putShort(2)
        b.putShort(16)
        b.put("data".toByteArray())
        b.putInt(len.toInt())
        return b.array()
    }

    fun process(input: File, output: File, p: Preset) {
        val rate = RecorderService.RATE
        val dataLen = max(0L, input.length() - 44)
        val dt = 1f / rate
        val twoPi = 2f * PI.toFloat()

        fun alpha(fc: Float): Float {
            val rc = 1f / (twoPi * fc)
            return dt / (rc + dt)
        }

        val hpRc = 1f / (twoPi * 90f)
        val hpA = hpRc / (hpRc + dt)
        val a250 = alpha(250f)
        val a3k = alpha(3000f)
        val aDamp = alpha(3500f)

        var hpY = 0f
        var hpX = 0f
        var l250 = 0f
        var l3k = 0f
        var env = 0f
        var gate = 1f

        val echoLen = max(1, p.echoDelayMs * rate / 1000)
        val echoBuf = FloatArray(echoLen)
        var echoIdx = 0
        var echoLp = 0f

        val combBase = intArrayOf(1557, 1617, 1491, 1422)
        val scale = 0.5f + p.room * 0.9f
        val combBufs = Array(4) { FloatArray(max(32, (combBase[it] * scale).toInt())) }
        val combIdx = IntArray(4)
        val combLp = FloatArray(4)
        val combFb = 0.6f + 0.25f * p.room

        val apBufs = arrayOf(FloatArray(225), FloatArray(556))
        val apIdx = IntArray(2)

        FileInputStream(input).use { ins ->
            ins.channel.position(44)
            BufferedOutputStream(FileOutputStream(output)).use { out ->
                out.write(header(dataLen, rate))
                val inBuf = ByteArray(16384)
                val outBuf = ByteArray(16384)
                while (true) {
                    val n = ins.read(inBuf)
                    if (n <= 0) break
                    val frames = n / 2
                    for (i in 0 until frames) {
                        val lo = inBuf[2 * i].toInt() and 0xFF
                        val hi = inBuf[2 * i + 1].toInt()
                        val x = ((hi shl 8) or lo).toShort() / 32768f

                        // hum, hawa aur rumble kam karna
                        hpY = hpA * (hpY + x - hpX)
                        hpX = x
                        var s = hpY

                        // awaaz garam aur saaf karna
                        l250 += a250 * (s - l250)
                        l3k += a3k * (s - l3k)
                        s = s + p.warmth * l250 + p.clarity * (s - l3k)

                        // halka noise gate
                        env = max(abs(s), env * 0.9995f)
                        val target = if (env < 0.004f) 0.35f else 1f
                        gate += 0.001f * (target - gate)
                        s *= gate * 1.3f

                        // echo: feedback mein dheemi awaaz, isliye metallic nahi lagta
                        val d = echoBuf[echoIdx]
                        echoLp += aDamp * (d - echoLp)
                        echoBuf[echoIdx] = s + echoLp * p.echoRepeat
                        echoIdx++
                        if (echoIdx >= echoLen) echoIdx = 0
                        val echoOut = if (p.echoOn) d * p.echoVolume else 0f

                        // reverb
                        var rev = 0f
                        for (c in 0 until 4) {
                            val buf = combBufs[c]
                            val y = buf[combIdx[c]]
                            combLp[c] += aDamp * (y - combLp[c])
                            buf[combIdx[c]] = s + combLp[c] * combFb
                            combIdx[c]++
                            if (combIdx[c] >= buf.size) combIdx[c] = 0
                            rev += y
                        }
                        rev *= 0.25f
                        for (a in 0 until 2) {
                            val buf = apBufs[a]
                            val b = buf[apIdx[a]]
                            val y = -rev + b
                            buf[apIdx[a]] = rev + b * 0.5f
                            apIdx[a]++
                            if (apIdx[a] >= buf.size) apIdx[a] = 0
                            rev = y
                        }

                        val mixed = limit(s + echoOut + p.reverbMix * rev)
                        val v = (mixed * 32767f).toInt().coerceIn(-32768, 32767)
                        outBuf[2 * i] = (v and 0xFF).toByte()
                        outBuf[2 * i + 1] = (v shr 8).toByte()
                    }
                    out.write(outBuf, 0, frames * 2)
                }
            }
        }
    }
}
