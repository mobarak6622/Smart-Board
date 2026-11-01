package com.example.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.example.model.CompassState
import com.example.model.ProtractorState
import com.example.model.RulerState
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object GeometryInstrumentOverlay {

    fun drawInstruments(
        canvas: Canvas,
        ruler: RulerState,
        protractor: ProtractorState,
        compass: CompassState
    ) {
        if (ruler.enabled) {
            drawRuler(canvas, ruler)
        }
        if (protractor.enabled) {
            drawProtractor(canvas, protractor)
        }
        if (compass.enabled) {
            drawCompass(canvas, compass)
        }
    }

    private fun drawRuler(canvas: Canvas, ruler: RulerState) {
        canvas.save()
        canvas.translate(ruler.centerX, ruler.centerY)
        canvas.rotate(ruler.angleDegrees)

        val halfLen = ruler.lengthPx / 2f
        val halfH = 46f
        val rect = RectF(-halfLen, -halfH, halfLen, halfH)

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(215, 254, 243, 199) // Warm translucent amber-ivory studio ruler
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#D97706")
            strokeWidth = 3f
        }
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#78350F")
            strokeWidth = 2f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#78350F")
            textSize = 18f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }

        canvas.drawRoundRect(rect, 12f, 12f, bodyPaint)
        canvas.drawRoundRect(rect, 12f, 12f, borderPaint)

        // Draw cm and mm ticks along top edge
        val stepPx = 10f
        var x = -halfLen + 20f
        var tickIndex = 0
        while (x <= halfLen - 20f) {
            val tickHeight = when {
                tickIndex % 10 == 0 -> 22f
                tickIndex % 5 == 0 -> 14f
                else -> 8f
            }
            canvas.drawLine(x, -halfH, x, -halfH + tickHeight, tickPaint)
            if (tickIndex % 10 == 0) {
                val cm = tickIndex / 10
                canvas.drawText(cm.toString(), x, -halfH + 36f, textPaint)
            }
            x += stepPx
            tickIndex++
        }

        // Angle readout pill in center
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#0F172A")
        }
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val badgeRect = RectF(-68f, -4f, 68f, 34f)
        canvas.drawRoundRect(badgeRect, 18f, 18f, badgePaint)
        canvas.drawText("${ruler.angleDegrees.roundToInt()}°", 0f, 22f, badgeTextPaint)

        // Rotate knob on right end
        val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#2563EB")
        }
        canvas.drawCircle(halfLen - 28f, 12f, 18f, knobPaint)
        canvas.drawText("↻", halfLen - 28f, 19f, badgeTextPaint)

        canvas.restore()
    }

    private fun drawProtractor(canvas: Canvas, protractor: ProtractorState) {
        canvas.save()
        canvas.translate(protractor.centerX, protractor.centerY)
        canvas.rotate(protractor.rotationDegrees)

        val r = protractor.radiusPx
        val arcRect = RectF(-r, -r, r, r)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(195, 224, 242, 254) // Translucent sky blue
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#0284C7")
            strokeWidth = 3f
        }
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#0C4A6E")
            strokeWidth = 2f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0C4A6E")
            textSize = 16f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }

        // Draw upper semicircle (180 degrees to 360 degrees in Android canvas coordinates)
        val path = Path().apply {
            arcTo(arcRect, 180f, 180f)
            close()
        }
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, borderPaint)

        // Inner cutout arc
        val innerR = r * 0.45f
        val innerRect = RectF(-innerR, -innerR, innerR, innerR)
        val innerPath = Path().apply {
            arcTo(innerRect, 180f, 180f)
            close()
        }
        canvas.drawPath(innerPath, borderPaint)

        // Degree ticks from 0 to 180
        for (deg in 0..180 step 5) {
            val rad = Math.toRadians(deg.toDouble())
            val cosD = cos(rad).toFloat()
            val sinD = sin(rad).toFloat()
            val tickLen = when {
                deg % 30 == 0 -> 22f
                deg % 10 == 0 -> 14f
                else -> 7f
            }
            val xOuter = r * cosD
            val yOuter = -r * sinD
            val xInner = (r - tickLen) * cosD
            val yInner = -(r - tickLen) * sinD
            canvas.drawLine(xOuter, yOuter, xInner, yInner, tickPaint)

            if (deg % 30 == 0) {
                val lx = (r - 36f) * cosD
                val ly = -(r - 36f) * sinD + 5f
                canvas.drawText("$deg°", lx, ly, labelPaint)
            }
        }

        // Origin crosshairs
        canvas.drawCircle(0f, 0f, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0284C7")
            style = Paint.Style.FILL
        })

        // Guide Ray Line & Handle
        val guideRad = Math.toRadians(protractor.guideAngleDegrees.toDouble())
        val rayX = (r + 36f) * cos(guideRad).toFloat()
        val rayY = -(r + 36f) * sin(guideRad).toFloat()

        val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#DC2626")
            strokeWidth = 3.5f
            pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        }
        canvas.drawLine(0f, 0f, rayX, rayY, rayPaint)

        val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#DC2626")
        }
        canvas.drawCircle(rayX, rayY, 18f, handlePaint)

        // Readout badge
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#0F172A")
        }
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawRoundRect(RectF(-56f, -54f, 56f, -18f), 16f, 16f, badgePaint)
        canvas.drawText("${protractor.guideAngleDegrees.roundToInt()}°", 0f, -30f, badgeTextPaint)

        canvas.restore()
    }

    private fun drawCompass(canvas: Canvas, compass: CompassState) {
        val px = compass.pivotX
        val py = compass.pivotY
        val r = compass.radiusPx

        // Dashed preview circle
        val previewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#7C3AED")
            strokeWidth = 3f
            pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
        }
        canvas.drawCircle(px, py, r, previewPaint)

        // Radius arm line from pivot to pencil tip
        val tipX = px + r
        val tipY = py
        val armPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#334155")
            strokeWidth = 5f
        }
        canvas.drawLine(px, py - 70f, px, py, armPaint)
        canvas.drawLine(px, py - 70f, tipX, tipY, armPaint)
        canvas.drawLine(px, py, tipX, tipY, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#94A3B8")
            strokeWidth = 2.5f
        })

        // Pivot Needle Point (Left/Center)
        val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#0F172A")
        }
        canvas.drawCircle(px, py, 16f, pivotPaint)

        // Pencil Tip Handle (Right)
        val pencilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#7C3AED")
        }
        canvas.drawCircle(tipX, tipY, 20f, pencilPaint)

        // Radius label in cm
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#0F172A")
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val midX = px + r / 2f
        val cmVal = (r / 45f * 10f).roundToInt() / 10f
        canvas.drawRoundRect(RectF(midX - 52f, py - 42f, midX + 52f, py - 10f), 14f, 14f, badgePaint)
        canvas.drawText("r = ${cmVal}cm", midX, py - 20f, textPaint)
    }
}
