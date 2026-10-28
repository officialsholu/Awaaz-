package com.awaaz.recorder

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.tanh

class Dsp(private val p: Preset, rate: Int) {
    private val dt = 1f / rate
    private val twoPi = 2f * PI.toFloat()

    private fun alpha(fc: Float): Float {
        val rc = 1f / (twoPi * fc)
        return dt / (rc + dt)
    }

    private val hpRc = 1f / (twoPi * 90f)
    private val hpA = hpRc / (hpRc + dt)
    private val a250 = alpha(250f)
    private val a3k = alpha(3000f)
    private val aDamp = alpha(3500f)

    private var hpY = 0f
    private var hpX = 0f
    private var l250 = 0f
    private var l3k = 0f
    private var env = 0f
    private var gate = 1f

    private val echoLen = max(1, p.echoDelayMs * rate / 1000)
    private val echoBuf = FloatArray(echoLen)
    private var echoIdx = 0
    private var echoLp = 0f

    private val combBase = intArrayOf(1557, 1617, 1491, 1422)
    private val scale = 0.5f + p.room * 0.9f
    private val combBufs = Array(4) { FloatArray(max(32, (combBase[it] * scale).toInt())) }
    private val combIdx = IntArray(4)
    private val combLp = FloatArray(4)
    private val combFb = 0.6f + 0.25f * p.room

    private val apBufs = arrayOf(FloatArray(225), FloatArray(556))
    private val apIdx = IntArray(2)

    private fun limit(v: Float): Float {
        val a = abs(v)
        return if (a <= 0.8f) v else sign(v) * (0.8f + 0.2f * tanh((a - 0.8f) / 0.2f))
    }

    // ek sample (-1 se 1 ke beech) ko process karke wapas deta hai
    fun process(x: Float): Float {
        hpY = hpA * (hpY + x - hpX)
        hpX = x
        var s = hpY

        l250 += a250 * (s - l250)
        l3k += a3k * (s - l3k)
        s = s + p.warmth * l250 + p.clarity * (s - l3k)

        env = max(abs(s), env * 0.9995f)
        val target = if (env < 0.004f) 0.35f else 1f
        gate += 0.001f * (target - gate)
        s *= gate * 1.3f

        val d = echoBuf[echoIdx]
        echoLp += aDamp * (d - echoLp)
        echoBuf[echoIdx] = s + echoLp * p.echoRepeat
        echoIdx++
        if (echoIdx >= echoLen) echoIdx = 0
        val echoOut = if (p.echoOn) d * p.echoVolume else 0f

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

        return limit(s + echoOut + p.reverbMix * rev)
    }
}
