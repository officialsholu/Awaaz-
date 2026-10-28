package com.awaaz.recorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class Icon2View(c: Context, var kind: Int) : View(c) {
    companion object {
        const val BACK = 0
        const val PENCIL = 1
    }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val d = resources.displayMetrics.density
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.color = 0xFFE6E6E6.toInt()
        p.strokeWidth = d * 2.2f
        when (kind) {
            BACK -> {
                path.reset()
                path.moveTo(w * .62f, h * .2f)
                path.lineTo(w * .32f, h * .5f)
                path.lineTo(w * .62f, h * .8f)
                c.drawPath(path, p)
            }
            PENCIL -> {
                c.drawLine(w * .3f, h * .66f, w * .74f, h * .22f, p)
                c.drawLine(w * .22f, h * .82f, w * .5f, h * .82f, p)
                c.drawLine(w * .3f, h * .66f, w * .26f, h * .78f, p)
            }
        }
    }
}

class RingIcon(c: Context, var kind: Int, private val big: Boolean) : View(c) {
    companion object {
        const val FLAG = 0
        const val PAUSE = 1
        const val PLAY = 2
        const val CHECK = 3
    }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val red = 0xFFE8504A.toInt()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val d = resources.displayMetrics.density
        val sw = d * (if (big) 3f else 2f)
        p.style = Paint.Style.STROKE
        p.strokeWidth = sw
        p.color = 0xFF555555.toInt()
        c.drawCircle(w / 2f, h / 2f, minOf(w, h) / 2f - sw, p)
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        when (kind) {
            FLAG -> {
                p.color = 0xFFFFFFFF.toInt()
                p.strokeWidth = d * 2.4f
                c.drawLine(w * .4f, h * .28f, w * .4f, h * .74f, p)
                p.style = Paint.Style.FILL
                path.reset()
                path.moveTo(w * .42f, h * .3f)
                path.lineTo(w * .72f, h * .42f)
                path.lineTo(w * .42f, h * .54f)
                path.close()
                c.drawPath(path, p)
            }
            PAUSE -> {
                p.style = Paint.Style.FILL
                p.color = red
                c.drawRoundRect(RectF(w * .38f, h * .32f, w * .47f, h * .68f), d * 2, d * 2, p)
                c.drawRoundRect(RectF(w * .53f, h * .32f, w * .62f, h * .68f), d * 2, d * 2, p)
            }
            PLAY -> {
                p.style = Paint.Style.FILL
                p.color = red
                path.reset()
                path.moveTo(w * .42f, h * .3f)
                path.lineTo(w * .42f, h * .7f)
                path.lineTo(w * .72f, h * .5f)
                path.close()
                c.drawPath(path, p)
            }
            CHECK -> {
                p.color = 0xFFFFFFFF.toInt()
                p.strokeWidth = d * 2.6f
                path.reset()
                path.moveTo(w * .3f, h * .52f)
                path.lineTo(w * .45f, h * .67f)
                path.lineTo(w * .72f, h * .36f)
                c.drawPath(path, p)
            }
        }
    }
}

class WaveView(c: Context) : View(c) {
    var levelAt: (Int) -> Float = { 0f }
    var levelCount: () -> Int = { 0 }
    var marks: List<Long> = emptyList()
    var dimFuture = false
    var perSec = 15

    private var timeSec = 0f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val red = 0xFFE8504A.toInt()

    init {
        tp.color = 0xFF8A8A8A.toInt()
        tp.textAlign = Paint.Align.CENTER
    }

    fun setTime(t: Float) {
        timeSec = t
        invalidate()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val d = resources.displayMetrics.density
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), (d * 176).toInt())
    }

    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        val w = width.toFloat()
        val cx = w / 2f
        val pps = d * 61f
        val top = d * 44f
        val bottom = d * 170f
        val mid = (top + bottom) / 2f

        p.style = Paint.Style.FILL
        p.color = 0xFF292929.toInt()
        c.drawRect(0f, top, w, bottom, p)

        tp.textSize = d * 13f
        val firstSec = max(0, floor(timeSec - cx / pps).toInt())
        val lastSec = ceil(timeSec + (w - cx) / pps).toInt()
        for (s in firstSec..lastSec) {
            val x = cx + (s - timeSec) * pps
            c.drawText("%02d:%02d".format(s / 60, s % 60), x, d * 28f, tp)
            p.color = 0xFF555555.toInt()
            c.drawRect(x - d * 0.5f, d * 34f, x + d * 0.5f, d * 44f, p)
        }

        val dt = 1f / perSec
        val cnt = levelCount()
        val firstIdx = floor((timeSec - cx / pps) / dt).toInt() - 1
        val lastIdx = ceil((timeSec + (w - cx) / pps) / dt).toInt() + 1
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = d * 1.6f
        val maxH = (bottom - top) * 0.42f
        for (i in firstIdx..lastIdx) {
            val x = cx + (i * dt - timeSec) * pps
            if (x < 0f || x > w) continue
            val future = x > cx + 1f
            if (i < 0 || i >= cnt) {
                p.style = Paint.Style.FILL
                p.color = 0xFF5A5A5A.toInt()
                c.drawCircle(x, mid, d * 1.1f, p)
            } else {
                val lv = levelAt(i)
                if (lv < 0.06f) {
                    p.style = Paint.Style.FILL
                    p.color = 0xFF8A8A8A.toInt()
                    c.drawCircle(x, mid, d * 1.1f, p)
                } else {
                    p.style = Paint.Style.STROKE
                    p.color = if (dimFuture && future) 0xFF6A6A6A.toInt() else 0xFFCFCFCF.toInt()
                    c.drawLine(x, mid - lv * maxH, x, mid + lv * maxH, p)
                }
            }
        }

        p.style = Paint.Style.FILL
        p.color = red
        for (m in marks) {
            val x = cx + (m / 1000f - timeSec) * pps
            if (x < -d * 12f || x > w + d * 12f) continue
            c.drawRect(x - d * 0.8f, top, x + d * 0.8f, top + d * 26f, p)
            path.reset()
            path.moveTo(x, top)
            path.lineTo(x + d * 11f, top + d * 5f)
            path.lineTo(x, top + d * 10f)
            path.close()
            c.drawPath(path, p)
        }

        c.drawRect(cx - d, top - d * 4f, cx + d, bottom + d * 4f, p)
        path.reset()
        path.moveTo(cx - d * 5f, top - d * 4f)
        path.lineTo(cx + d * 5f, top - d * 4f)
        path.lineTo(cx, top + d * 3f)
        path.close()
        c.drawPath(path, p)
        path.reset()
        path.moveTo(cx - d * 5f, bottom + d * 4f)
        path.lineTo(cx + d * 5f, bottom + d * 4f)
        path.lineTo(cx, bottom - d * 3f)
        path.close()
        c.drawPath(path, p)
    }
}
