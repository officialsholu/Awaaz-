package com.awaaz.recorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

class IconView(c: Context, var kind: Int) : View(c) {
    companion object {
        const val SELECT = 0
        const val SEARCH = 1
        const val MORE = 2
        const val PLAY = 3
        const val PAUSE = 4
        const val TRASH = 5
        const val SHARE = 6
        const val EDIT = 7
        const val SPARK = 8
        const val CLOSE = 9
        const val SLIDERS = 10
        const val SPEAKER = 11
    }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val light = 0xFFE6E6E6.toInt()
    private val red = 0xFFE8504A.toInt()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val d = resources.displayMetrics.density
        p.strokeWidth = d * 2f
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        when (kind) {
            SELECT -> {
                p.style = Paint.Style.STROKE
                p.color = light
                c.drawRoundRect(RectF(w * .16f, h * .16f, w * .84f, h * .84f), d * 4, d * 4, p)
                path.reset()
                path.moveTo(w * .31f, h * .52f)
                path.lineTo(w * .45f, h * .66f)
                path.lineTo(w * .70f, h * .36f)
                c.drawPath(path, p)
            }
            SEARCH -> {
                p.style = Paint.Style.STROKE
                p.color = light
                c.drawCircle(w * .45f, h * .45f, w * .26f, p)
                c.drawLine(w * .64f, h * .64f, w * .84f, h * .84f, p)
            }
            MORE -> {
                p.style = Paint.Style.FILL
                p.color = light
                c.drawCircle(w * .5f, h * .22f, d * 2.3f, p)
                c.drawCircle(w * .5f, h * .5f, d * 2.3f, p)
                c.drawCircle(w * .5f, h * .78f, d * 2.3f, p)
            }
            PLAY -> {
                p.style = Paint.Style.FILL
                p.color = red
                path.reset()
                path.moveTo(w * .36f, h * .26f)
                path.lineTo(w * .36f, h * .74f)
                path.lineTo(w * .76f, h * .5f)
                path.close()
                c.drawPath(path, p)
            }
            PAUSE -> {
                p.style = Paint.Style.FILL
                p.color = red
                c.drawRoundRect(RectF(w * .32f, h * .26f, w * .46f, h * .74f), d * 2, d * 2, p)
                c.drawRoundRect(RectF(w * .54f, h * .26f, w * .68f, h * .74f), d * 2, d * 2, p)
            }
            TRASH -> {
                p.style = Paint.Style.STROKE
                p.color = light
                c.drawLine(w * .2f, h * .28f, w * .8f, h * .28f, p)
                c.drawRoundRect(RectF(w * .4f, h * .16f, w * .6f, h * .28f), d * 2, d * 2, p)
                path.reset()
                path.moveTo(w * .27f, h * .28f)
                path.lineTo(w * .32f, h * .82f)
                path.lineTo(w * .68f, h * .82f)
                path.lineTo(w * .73f, h * .28f)
                c.drawPath(path, p)
                c.drawLine(w * .42f, h * .4f, w * .42f, h * .7f, p)
                c.drawLine(w * .58f, h * .4f, w * .58f, h * .7f, p)
            }
            SHARE -> {
                p.style = Paint.Style.STROKE
                p.color = light
                path.reset()
                path.moveTo(w * .56f, h * .22f)
                path.lineTo(w * .24f, h * .22f)
                path.lineTo(w * .24f, h * .78f)
                path.lineTo(w * .78f, h * .78f)
                path.lineTo(w * .78f, h * .5f)
                c.drawPath(path, p)
                c.drawLine(w * .46f, h * .54f, w * .8f, h * .2f, p)
                path.reset()
                path.moveTo(w * .62f, h * .2f)
                path.lineTo(w * .8f, h * .2f)
                path.lineTo(w * .8f, h * .38f)
                c.drawPath(path, p)
            }
            EDIT -> {
                p.style = Paint.Style.STROKE
                p.color = light
                path.reset()
                path.moveTo(w * .56f, h * .22f)
                path.lineTo(w * .24f, h * .22f)
                path.lineTo(w * .24f, h * .78f)
                path.lineTo(w * .78f, h * .78f)
                path.lineTo(w * .78f, h * .5f)
                c.drawPath(path, p)
                p.strokeWidth = d * 3f
                c.drawLine(w * .42f, h * .58f, w * .78f, h * .22f, p)
            }
            SPARK -> {
                p.style = Paint.Style.STROKE
                p.color = light
                path.reset()
                path.moveTo(w * .5f, h * .15f)
                path.quadTo(w * .5f, h * .5f, w * .85f, h * .5f)
                path.quadTo(w * .5f, h * .5f, w * .5f, h * .85f)
                path.quadTo(w * .5f, h * .5f, w * .15f, h * .5f)
                path.quadTo(w * .5f, h * .5f, w * .5f, h * .15f)
                c.drawPath(path, p)
            }
            CLOSE -> {
                p.style = Paint.Style.STROKE
                p.color = light
                c.drawLine(w * .25f, h * .25f, w * .75f, h * .75f, p)
                c.drawLine(w * .75f, h * .25f, w * .25f, h * .75f, p)
            }
            SLIDERS -> {
                p.style = Paint.Style.STROKE
                p.color = light
                c.drawLine(w * .15f, h * .3f, w * .85f, h * .3f, p)
                c.drawLine(w * .15f, h * .5f, w * .85f, h * .5f, p)
                c.drawLine(w * .15f, h * .7f, w * .85f, h * .7f, p)
                c.drawCircle(w * .35f, h * .3f, d * 3.5f, p)
                c.drawCircle(w * .65f, h * .5f, d * 3.5f, p)
                c.drawCircle(w * .4f, h * .7f, d * 3.5f, p)
            }
            SPEAKER -> {
                p.style = Paint.Style.STROKE
                p.color = light
                path.reset()
                path.moveTo(w * .18f, h * .38f)
                path.lineTo(w * .34f, h * .38f)
                path.lineTo(w * .54f, h * .22f)
                path.lineTo(w * .54f, h * .78f)
                path.lineTo(w * .34f, h * .62f)
                path.lineTo(w * .18f, h * .62f)
                path.close()
                c.drawPath(path, p)
                c.drawArc(RectF(w * .5f, h * .32f, w * .78f, h * .68f), -50f, 100f, false, p)
                c.drawArc(RectF(w * .58f, h * .2f, w * .92f, h * .8f), -50f, 100f, false, p)
            }
        }
    }
}

class CheckView(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    var state: Int = 0
        set(v) {
            field = v
            invalidate()
        }

    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        val r = RectF(d * 2, d * 2, w - d * 2, h - d * 2)
        if (state == 0) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = d * 2f
            p.color = 0xFF777777.toInt()
            c.drawRoundRect(r, d * 4, d * 4, p)
        } else {
            p.style = Paint.Style.FILL
            p.color = 0xFFE8504A.toInt()
            c.drawRoundRect(r, d * 4, d * 4, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = d * 2f
            p.strokeCap = Paint.Cap.ROUND
            p.strokeJoin = Paint.Join.ROUND
            p.color = Color.WHITE
            if (state == 1) {
                path.reset()
                path.moveTo(w * .28f, h * .52f)
                path.lineTo(w * .43f, h * .67f)
                path.lineTo(w * .72f, h * .34f)
                c.drawPath(path, p)
            } else {
                c.drawLine(w * .3f, h * .5f, w * .7f, h * .5f, p)
            }
        }
    }
}

class RecView(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    var recording: Boolean = false
        set(v) {
            if (field != v) {
                field = v
                invalidate()
            }
        }

    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f - d * 3f
        p.style = Paint.Style.STROKE
        p.strokeWidth = d * 3f
        p.color = 0xFF555555.toInt()
        c.drawCircle(cx, cy, r, p)
        p.style = Paint.Style.FILL
        p.color = 0xFFE8504A.toInt()
        if (recording) {
            val s = r * 0.42f
            c.drawRoundRect(RectF(cx - s, cy - s, cx + s, cy + s), d * 5, d * 5, p)
        } else {
            c.drawCircle(cx, cy, r * 0.52f, p)
        }
    }
}
