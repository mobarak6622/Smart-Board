package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import com.example.model.BackgroundType
import com.example.model.BoardPage
import com.example.model.CanvasObject
import com.example.model.CanvasObjectType
import com.example.model.CanvasStroke
import com.example.model.CompassState
import com.example.model.DrawingToolType
import com.example.model.MathFormulaType
import com.example.model.PointFData
import com.example.model.ProtractorState
import com.example.model.RulerState
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

object CanvasRenderEngine {

    private val bitmapCache = mutableMapOf<String, Bitmap>()

    fun renderPageToAndroidCanvas(
        canvas: Canvas,
        width: Float,
        height: Float,
        page: BoardPage,
        activeStroke: CanvasStroke? = null,
        selectedObjectId: String? = null,
        renderUnbakedMathNodes: Boolean = true
    ) {
        // 1. Background Layer
        drawBackground(canvas, width, height, page.backgroundType)

        // 2. Offscreen Ink Layer so Pixel Eraser (PorterDuff.Mode.CLEAR) only erases strokes, never background
        val layerCount = canvas.saveLayer(0f, 0f, width, height, null)
        for (stroke in page.strokes) {
            drawStroke(canvas, stroke)
        }
        if (activeStroke != null) {
            drawStroke(canvas, activeStroke)
        }
        canvas.restoreToCount(layerCount)

        // 3. Objects Layer (Text, Structured Math Cards, Images)
        for (obj in page.objects) {
            if (obj.type == CanvasObjectType.MATH_FORMULA && !obj.isBakedToCanvas && !renderUnbakedMathNodes) {
                continue
            }
            drawCanvasObject(canvas, obj, isSelected = obj.id == selectedObjectId)
        }
    }

    fun drawBackground(
        canvas: Canvas,
        width: Float,
        height: Float,
        backgroundType: BackgroundType
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        when (backgroundType) {
            BackgroundType.PLAIN_WHITE -> Unit

            BackgroundType.SQUARE_GRID -> {
                val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = Color.parseColor("#E2E8F0")
                    strokeWidth = 1.5f
                }
                val majorGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = Color.parseColor("#CBD5E1")
                    strokeWidth = 2.2f
                }
                val step = 48f
                var x = step
                var col = 1
                while (x < width) {
                    canvas.drawLine(x, 0f, x, height, if (col % 5 == 0) majorGridPaint else gridPaint)
                    x += step
                    col++
                }
                var y = step
                var row = 1
                while (y < height) {
                    canvas.drawLine(0f, y, width, y, if (row % 5 == 0) majorGridPaint else gridPaint)
                    y += step
                    row++
                }
            }

            BackgroundType.DOT_GRID -> {
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.parseColor("#94A3B8")
                }
                val step = 48f
                var x = step
                while (x < width) {
                    var y = step
                    while (y < height) {
                        canvas.drawCircle(x, y, 2.4f, dotPaint)
                        y += step
                    }
                    x += step
                }
            }

            BackgroundType.RULED_LINES -> {
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = Color.parseColor("#CBD5E1")
                    strokeWidth = 1.8f
                }
                val marginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = Color.parseColor("#FCA5A5")
                    strokeWidth = 2.5f
                }
                val step = 64f
                var y = 120f
                while (y < height) {
                    canvas.drawLine(0f, y, width, y, linePaint)
                    y += step
                }
                canvas.drawLine(96f, 0f, 96f, height, marginPaint)
            }

            BackgroundType.COORDINATE_GRAPH -> {
                // Minor grid
                val minorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = Color.parseColor("#F1F5F9")
                    strokeWidth = 1.4f
                }
                val majorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = Color.parseColor("#E2E8F0")
                    strokeWidth = 2f
                }
                val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = Color.parseColor("#334155")
                    strokeWidth = 3.5f
                }
                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#475569")
                    textSize = 22f
                    typeface = Typeface.MONOSPACE
                }

                val cx = width / 2f
                val cy = height / 2f
                val unit = 50f

                // Draw vertical grid lines from center
                var offset = unit
                var stepCount = 1
                while (cx + offset < width || cx - offset > 0) {
                    val p = if (stepCount % 2 == 0) majorPaint else minorPaint
                    if (cx + offset < width) canvas.drawLine(cx + offset, 0f, cx + offset, height, p)
                    if (cx - offset > 0) canvas.drawLine(cx - offset, 0f, cx - offset, height, p)
                    offset += unit
                    stepCount++
                }

                offset = unit
                stepCount = 1
                while (cy + offset < height || cy - offset > 0) {
                    val p = if (stepCount % 2 == 0) majorPaint else minorPaint
                    if (cy + offset < height) canvas.drawLine(0f, cy + offset, width, cy + offset, p)
                    if (cy - offset > 0) canvas.drawLine(0f, cy - offset, width, cy - offset, p)
                    offset += unit
                    stepCount++
                }

                // X and Y Axes
                canvas.drawLine(0f, cy, width, cy, axisPaint)
                canvas.drawLine(cx, 0f, cx, height, axisPaint)

                // Ticks & Numbers
                var idx = 1
                var tickOffset = unit * 2
                while (cx + tickOffset < width - 20f || cx - tickOffset > 20f) {
                    val labelVal = idx * 2
                    if (cx + tickOffset < width - 24f) {
                        canvas.drawLine(cx + tickOffset, cy - 8f, cx + tickOffset, cy + 8f, axisPaint)
                        canvas.drawText(labelVal.toString(), cx + tickOffset - 8f, cy + 28f, labelPaint)
                    }
                    if (cx - tickOffset > 24f) {
                        canvas.drawLine(cx - tickOffset, cy - 8f, cx - tickOffset, cy + 8f, axisPaint)
                        canvas.drawText("-$labelVal", cx - tickOffset - 16f, cy + 28f, labelPaint)
                    }
                    tickOffset += unit * 2
                    idx++
                }

                idx = 1
                tickOffset = unit * 2
                while (cy + tickOffset < height - 20f || cy - tickOffset > 20f) {
                    val labelVal = idx * 2
                    if (cy - tickOffset > 24f) {
                        canvas.drawLine(cx - 8f, cy - tickOffset, cx + 8f, cy - tickOffset, axisPaint)
                        canvas.drawText(labelVal.toString(), cx + 12f, cy - tickOffset + 8f, labelPaint)
                    }
                    if (cy + tickOffset < height - 24f) {
                        canvas.drawLine(cx - 8f, cy + tickOffset, cx + 8f, cy + tickOffset, axisPaint)
                        canvas.drawText("-$labelVal", cx + 12f, cy + tickOffset + 8f, labelPaint)
                    }
                    tickOffset += unit * 2
                    idx++
                }
                canvas.drawText("O(0,0)", cx + 10f, cy + 26f, labelPaint)
                canvas.drawText("X", width - 32f, cy - 14f, labelPaint)
                canvas.drawText("Y", cx + 14f, 36f, labelPaint)
            }
        }
    }

    fun drawStroke(canvas: Canvas, stroke: CanvasStroke) {
        val pts = stroke.points
        if (pts.isEmpty()) return

        val baseColor = stroke.colorArgb.toInt()
        val effectiveAlpha = (stroke.alpha.coerceIn(0.05f, 1f) * 255).toInt()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke.strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = baseColor
            alpha = effectiveAlpha
        }

        when (stroke.toolType) {
            DrawingToolType.PEN -> {
                drawSmoothFreehandPath(canvas, pts, paint)
            }

            DrawingToolType.PENCIL -> {
                paint.alpha = (effectiveAlpha * 0.78f).toInt().coerceIn(20, 255)
                paint.strokeWidth = (stroke.strokeWidth * 0.85f).coerceAtLeast(1.5f)
                drawSmoothFreehandPath(canvas, pts, paint)
            }

            DrawingToolType.BRUSH -> {
                paint.strokeWidth = stroke.strokeWidth * 1.45f
                paint.alpha = (effectiveAlpha * 0.88f).toInt()
                drawSmoothFreehandPath(canvas, pts, paint)
            }

            DrawingToolType.HIGHLIGHTER -> {
                paint.strokeCap = Paint.Cap.SQUARE
                paint.strokeJoin = Paint.Join.BEVEL
                paint.strokeWidth = stroke.strokeWidth * 2.2f
                paint.alpha = (effectiveAlpha * 0.36f).toInt().coerceIn(30, 120)
                drawSmoothFreehandPath(canvas, pts, paint)
            }

            DrawingToolType.MARKER -> {
                paint.strokeCap = Paint.Cap.SQUARE
                paint.strokeJoin = Paint.Join.ROUND
                paint.strokeWidth = stroke.strokeWidth * 1.35f
                drawSmoothFreehandPath(canvas, pts, paint)
            }

            DrawingToolType.NEON_PEN -> {
                val glowPaint = Paint(paint).apply {
                    strokeWidth = stroke.strokeWidth * 2.4f
                    alpha = 150
                    maskFilter = BlurMaskFilter(stroke.strokeWidth * 1.3f + 6f, BlurMaskFilter.Blur.NORMAL)
                }
                val corePaint = Paint(paint).apply {
                    color = Color.WHITE
                    strokeWidth = (stroke.strokeWidth * 0.42f).coerceAtLeast(2f)
                    alpha = 245
                }
                drawSmoothFreehandPath(canvas, pts, glowPaint)
                drawSmoothFreehandPath(canvas, pts, paint)
                drawSmoothFreehandPath(canvas, pts, corePaint)
            }

            DrawingToolType.PIXEL_ERASER -> {
                val eraserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = stroke.strokeWidth * 1.6f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
                drawSmoothFreehandPath(canvas, pts, eraserPaint)
            }

            DrawingToolType.LINE -> {
                val start = pts.first()
                val end = pts.last()
                canvas.drawLine(start.x, start.y, end.x, end.y, paint)
            }

            DrawingToolType.ARROW -> {
                val start = pts.first()
                val end = pts.last()
                canvas.drawLine(start.x, start.y, end.x, end.y, paint)
                val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
                val headLen = (stroke.strokeWidth * 3.5f).coerceAtLeast(24f)
                val wingAngle = Math.toRadians(28.0)
                val x1 = end.x - headLen * cos(angle - wingAngle).toFloat()
                val y1 = end.y - headLen * sin(angle - wingAngle).toFloat()
                val x2 = end.x - headLen * cos(angle + wingAngle).toFloat()
                val y2 = end.y - headLen * sin(angle + wingAngle).toFloat()
                val arrowPath = Path().apply {
                    moveTo(x1, y1)
                    lineTo(end.x, end.y)
                    lineTo(x2, y2)
                }
                canvas.drawPath(arrowPath, paint)
            }

            DrawingToolType.CIRCLE -> {
                val start = pts.first()
                val end = pts.last()
                val rect = RectF(
                    minOf(start.x, end.x),
                    minOf(start.y, end.y),
                    maxOf(start.x, end.x),
                    maxOf(start.y, end.y)
                )
                canvas.drawOval(rect, paint)
            }

            DrawingToolType.RECTANGLE -> {
                val start = pts.first()
                val end = pts.last()
                val rect = RectF(
                    minOf(start.x, end.x),
                    minOf(start.y, end.y),
                    maxOf(start.x, end.x),
                    maxOf(start.y, end.y)
                )
                canvas.drawRect(rect, paint)
            }

            DrawingToolType.TRIANGLE -> {
                val start = pts.first()
                val end = pts.last()
                val topX = (start.x + end.x) / 2f
                val topY = minOf(start.y, end.y)
                val bottomY = maxOf(start.y, end.y)
                val leftX = minOf(start.x, end.x)
                val rightX = maxOf(start.x, end.x)
                val path = Path().apply {
                    moveTo(topX, topY)
                    lineTo(rightX, bottomY)
                    lineTo(leftX, bottomY)
                    close()
                }
                canvas.drawPath(path, paint)
            }

            DrawingToolType.POLYGON -> {
                val start = pts.first()
                val end = pts.last()
                val cx = (start.x + end.x) / 2f
                val cy = (start.y + end.y) / 2f
                val radius = min(
                    kotlin.math.abs(end.x - start.x),
                    kotlin.math.abs(end.y - start.y)
                ) / 2f
                val sides = stroke.polygonSides.coerceIn(5, 8)
                if (radius > 2f) {
                    val path = Path()
                    for (i in 0 until sides) {
                        val theta = Math.toRadians((i * 360.0 / sides) - 90.0)
                        val px = cx + radius * cos(theta).toFloat()
                        val py = cy + radius * sin(theta).toFloat()
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    canvas.drawPath(path, paint)
                }
            }

            DrawingToolType.STROKE_ERASER,
            DrawingToolType.SELECT_MOVE,
            DrawingToolType.HAND_PAN -> Unit
        }
    }

    private fun drawSmoothFreehandPath(canvas: Canvas, pts: List<PointFData>, paint: Paint) {
        if (pts.size == 1) {
            val pt = pts.first()
            val dotPaint = Paint(paint).apply { style = Paint.Style.FILL }
            canvas.drawCircle(pt.x, pt.y, paint.strokeWidth / 2f, dotPaint)
            return
        }
        val path = Path()
        path.moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) {
            val prev = pts[i - 1]
            val curr = pts[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            path.quadTo(prev.x, prev.y, midX, midY)
        }
        val last = pts.last()
        path.lineTo(last.x, last.y)
        canvas.drawPath(path, paint)
    }

    fun drawCanvasObject(canvas: Canvas, obj: CanvasObject, isSelected: Boolean) {
        canvas.save()
        canvas.translate(obj.x, obj.y)
        canvas.rotate(obj.rotation)
        canvas.scale(obj.scale, obj.scale)

        val bounds = RectF(-140f, -60f, 140f, 60f)

        when (obj.type) {
            CanvasObjectType.TEXT -> {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = obj.colorArgb.toInt()
                    textSize = 42f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val lines = obj.text.ifBlank { "Text" }.split("\n")
                var maxW = 80f
                val lineHeight = 52f
                val totalH = lines.size * lineHeight
                var startY = -totalH / 2f + 36f
                for (line in lines) {
                    val w = textPaint.measureText(line)
                    if (w > maxW) maxW = w
                    canvas.drawText(line, -w / 2f, startY, textPaint)
                    startY += lineHeight
                }
                bounds.set(-maxW / 2f - 20f, -totalH / 2f - 16f, maxW / 2f + 20f, totalH / 2f + 16f)
            }

            CanvasObjectType.MATH_FORMULA -> {
                drawMathObject(canvas, obj, bounds)
            }

            CanvasObjectType.IMAGE -> {
                val bmp = loadCachedBitmap(obj.imagePath)
                if (bmp != null) {
                    val targetW = 320f
                    val ratio = bmp.height.toFloat() / bmp.width.coerceAtLeast(1).toFloat()
                    val targetH = targetW * ratio
                    val dest = RectF(-targetW / 2f, -targetH / 2f, targetW / 2f, targetH / 2f)
                    canvas.drawBitmap(bmp, null, dest, Paint(Paint.FILTER_BITMAP_FLAG))
                    bounds.set(dest.left - 12f, dest.top - 12f, dest.right + 12f, dest.bottom + 12f)
                }
            }
        }

        if (isSelected) {
            val selectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.parseColor("#2563EB")
                strokeWidth = 3f
                pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
            }
            val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.parseColor("#2563EB")
            }
            canvas.drawRoundRect(bounds, 12f, 12f, selectPaint)
            canvas.drawCircle(bounds.left, bounds.top, 8f, handlePaint)
            canvas.drawCircle(bounds.right, bounds.top, 8f, handlePaint)
            canvas.drawCircle(bounds.left, bounds.bottom, 8f, handlePaint)
            canvas.drawCircle(bounds.right, bounds.bottom, 8f, handlePaint)
        }

        canvas.restore()
    }

    private fun drawMathObject(canvas: Canvas, obj: CanvasObject, outBounds: RectF) {
        val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = obj.colorArgb.toInt()
            textSize = 40f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        val subPaint = Paint(mainPaint).apply {
            textSize = 26f
        }
        val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = obj.colorArgb.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(235, 248, 250, 252)
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 2f
        }

        when (obj.mathType) {
            MathFormulaType.FRACTION -> {
                val num = obj.primaryArg.ifBlank { "a" }
                val den = obj.secondaryArg.ifBlank { "b" }
                val w = maxOf(mainPaint.measureText(num), mainPaint.measureText(den), 56f) + 28f
                outBounds.set(-w / 2f - 18f, -66f, w / 2f + 18f, 66f)
                canvas.drawRoundRect(outBounds, 14f, 14f, cardBgPaint)
                canvas.drawRoundRect(outBounds, 14f, 14f, cardBorderPaint)

                canvas.drawText(num, -mainPaint.measureText(num) / 2f, -16f, mainPaint)
                canvas.drawLine(-w / 2f, 0f, w / 2f, 0f, symbolPaint)
                canvas.drawText(den, -mainPaint.measureText(den) / 2f, 42f, mainPaint)
            }

            MathFormulaType.SQUARE_ROOT, MathFormulaType.CUBE_ROOT -> {
                val rad = obj.primaryArg.ifBlank { "x" }
                val textW = mainPaint.measureText(rad)
                val totalW = textW + 52f
                outBounds.set(-totalW / 2f - 18f, -48f, totalW / 2f + 18f, 44f)
                canvas.drawRoundRect(outBounds, 14f, 14f, cardBgPaint)
                canvas.drawRoundRect(outBounds, 14f, 14f, cardBorderPaint)

                val startX = -totalW / 2f
                if (obj.mathType == MathFormulaType.CUBE_ROOT) {
                    canvas.drawText("3", startX, -14f, subPaint)
                }
                val rootPath = Path().apply {
                    moveTo(startX + 6f, 4f)
                    lineTo(startX + 16f, 20f)
                    lineTo(startX + 30f, -30f)
                    lineTo(startX + totalW, -30f)
                }
                canvas.drawPath(rootPath, symbolPaint)
                canvas.drawText(rad, startX + 36f, 16f, mainPaint)
            }

            MathFormulaType.POWER, MathFormulaType.SUBSCRIPT -> {
                val base = obj.primaryArg.ifBlank { "x" }
                val script = obj.secondaryArg.ifBlank { "2" }
                val baseW = mainPaint.measureText(base)
                val scriptW = subPaint.measureText(script)
                val totalW = baseW + scriptW + 12f
                outBounds.set(-totalW / 2f - 20f, -48f, totalW / 2f + 20f, 44f)
                canvas.drawRoundRect(outBounds, 14f, 14f, cardBgPaint)
                canvas.drawRoundRect(outBounds, 14f, 14f, cardBorderPaint)

                val startX = -totalW / 2f
                canvas.drawText(base, startX, 14f, mainPaint)
                val scriptY = if (obj.mathType == MathFormulaType.POWER) -10f else 28f
                canvas.drawText(script, startX + baseW + 4f, scriptY, subPaint)
            }

            MathFormulaType.INTEGRAL, MathFormulaType.SUMMATION -> {
                val bigSymbol = if (obj.mathType == MathFormulaType.INTEGRAL) "∫" else "∑"
                val bigPaint = Paint(mainPaint).apply {
                    textSize = 60f
                    typeface = Typeface.SERIF
                }
                val expr = obj.primaryArg.ifBlank { "f(x) dx" }
                val lower = obj.secondaryArg.ifBlank { "a" }
                val upper = obj.tertiaryArg.ifBlank { "b" }
                val exprW = mainPaint.measureText(expr)
                val totalW = 64f + exprW
                outBounds.set(-totalW / 2f - 20f, -64f, totalW / 2f + 20f, 64f)
                canvas.drawRoundRect(outBounds, 14f, 14f, cardBgPaint)
                canvas.drawRoundRect(outBounds, 14f, 14f, cardBorderPaint)

                val startX = -totalW / 2f
                canvas.drawText(upper, startX + 12f, -34f, subPaint)
                canvas.drawText(bigSymbol, startX + 4f, 18f, bigPaint)
                canvas.drawText(lower, startX + 10f, 48f, subPaint)
                canvas.drawText(expr, startX + 54f, 12f, mainPaint)
            }

            MathFormulaType.EQUATION -> {
                val expr = obj.primaryArg.ifBlank { "x = y" }
                val w = mainPaint.measureText(expr)
                outBounds.set(-w / 2f - 22f, -38f, w / 2f + 22f, 38f)
                canvas.drawRoundRect(outBounds, 14f, 14f, cardBgPaint)
                canvas.drawRoundRect(outBounds, 14f, 14f, cardBorderPaint)
                canvas.drawText(expr, -w / 2f, 14f, mainPaint)
            }
        }
    }

    private fun loadCachedBitmap(path: String): Bitmap? {
        if (path.isBlank()) return null
        bitmapCache[path]?.let { return it }
        val file = File(path)
        if (!file.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath)?.also {
                bitmapCache[path] = it
            }
        }.getOrNull()
    }

    /**
     * Projects raw touch coordinates onto active geometry instruments (Ruler edge or Protractor ray)
     * when the pointer is within snapping distance.
     */
    fun snapPointToInstruments(
        rawX: Float,
        rawY: Float,
        ruler: RulerState,
        protractor: ProtractorState
    ): PointFData {
        // 1. Check Ruler Top & Bottom Edges
        if (ruler.enabled) {
            val rad = Math.toRadians(ruler.angleDegrees.toDouble())
            val cosA = cos(rad).toFloat()
            val sinA = sin(rad).toFloat()
            val dx = rawX - ruler.centerX
            val dy = rawY - ruler.centerY
            // Local coordinates relative to ruler center
            val localX = dx * cosA + dy * sinA
            val localY = -dx * sinA + dy * cosA
            val halfLen = ruler.lengthPx / 2f
            val halfHeight = 46f
            val snapThreshold = 42f

            if (localX in (-halfLen - 20f)..(halfLen + 20f)) {
                if (kotlin.math.abs(localY - (-halfHeight)) <= snapThreshold) {
                    val snappedLocalY = -halfHeight - 3f
                    return PointFData(
                        x = ruler.centerX + localX * cosA - snappedLocalY * sinA,
                        y = ruler.centerY + localX * sinA + snappedLocalY * cosA
                    )
                } else if (kotlin.math.abs(localY - halfHeight) <= snapThreshold) {
                    val snappedLocalY = halfHeight + 3f
                    return PointFData(
                        x = ruler.centerX + localX * cosA - snappedLocalY * sinA,
                        y = ruler.centerY + localX * sinA + snappedLocalY * cosA
                    )
                }
            }
        }

        // 2. Check Protractor Guide Ray
        if (protractor.enabled) {
            val totalAngleRad = Math.toRadians((protractor.rotationDegrees - protractor.guideAngleDegrees).toDouble())
            val dirX = cos(totalAngleRad).toFloat()
            val dirY = sin(totalAngleRad).toFloat()
            val vx = rawX - protractor.centerX
            val vy = rawY - protractor.centerY
            val proj = vx * dirX + vy * dirY
            if (proj in 0f..(protractor.radiusPx + 90f)) {
                val perpDist = kotlin.math.abs(vx * (-dirY) + vy * dirX)
                if (perpDist <= 40f) {
                    return PointFData(
                        x = protractor.centerX + proj * dirX,
                        y = protractor.centerY + proj * dirY
                    )
                }
            }
        }

        return PointFData(rawX, rawY)
    }

    /**
     * Generates a circular arc stroke for the virtual Compass instrument.
     */
    fun generateCompassArcStroke(
        compass: CompassState,
        colorArgb: Long,
        strokeWidth: Float
    ): CanvasStroke {
        val pts = mutableListOf<PointFData>()
        val steps = 72
        val start = compass.startAngleDegrees
        val sweep = compass.sweepAngleDegrees
        for (i in 0..steps) {
            val deg = start + (sweep * i / steps.toFloat())
            val rad = Math.toRadians(deg.toDouble())
            pts.add(
                PointFData(
                    x = compass.pivotX + compass.radiusPx * cos(rad).toFloat(),
                    y = compass.pivotY + compass.radiusPx * sin(rad).toFloat()
                )
            )
        }
        return CanvasStroke(
            toolType = DrawingToolType.PEN,
            colorArgb = colorArgb,
            strokeWidth = strokeWidth,
            alpha = 1f,
            points = pts
        )
    }

    /**
     * Finds if a touch point hits any stroke (for Stroke Eraser) using point-to-segment distance.
     */
    fun findHitStrokeId(page: BoardPage, x: Float, y: Float, threshold: Float = 34f): String? {
        for (stroke in page.strokes.asReversed()) {
            if (stroke.toolType == DrawingToolType.PIXEL_ERASER) continue
            val hitRadius = threshold + stroke.strokeWidth / 2f
            val pts = stroke.points
            if (pts.isEmpty()) continue
            if (pts.size == 1) {
                if (hypot(pts[0].x - x, pts[0].y - y) <= hitRadius) {
                    return stroke.id
                }
                continue
            }
            for (i in 0 until pts.size - 1) {
                val a = pts[i]
                val b = pts[i + 1]
                if (distancePointToSegment(x, y, a.x, a.y, b.x, b.y) <= hitRadius) {
                    return stroke.id
                }
            }
            // For bounding-box shapes (Rectangle, Circle, Triangle, Polygon), also check perimeter/center
            if (stroke.toolType == DrawingToolType.RECTANGLE ||
                stroke.toolType == DrawingToolType.CIRCLE ||
                stroke.toolType == DrawingToolType.TRIANGLE ||
                stroke.toolType == DrawingToolType.POLYGON
            ) {
                val start = pts.first()
                val end = pts.last()
                val left = minOf(start.x, end.x) - hitRadius
                val right = maxOf(start.x, end.x) + hitRadius
                val top = minOf(start.y, end.y) - hitRadius
                val bottom = maxOf(start.y, end.y) + hitRadius
                if (x in left..right && y in top..bottom) {
                    return stroke.id
                }
            }
        }
        return null
    }

    private fun distancePointToSegment(
        px: Float,
        py: Float,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float
    ): Float {
        val abx = bx - ax
        val aby = by - ay
        val lenSq = abx * abx + aby * aby
        if (lenSq <= 0.0001f) {
            return hypot(px - ax, py - ay)
        }
        val t = (((px - ax) * abx + (py - ay) * aby) / lenSq).coerceIn(0f, 1f)
        val projX = ax + t * abx
        val projY = ay + t * aby
        return hypot(px - projX, py - projY)
    }

    /**
     * Finds if a touch point hits any CanvasObject (Text, Math Formula, Image).
     */
    fun findHitObjectId(page: BoardPage, x: Float, y: Float): String? {
        for (obj in page.objects.asReversed()) {
            val halfW = 140f * obj.scale
            val halfH = 75f * obj.scale
            if (x in (obj.x - halfW)..(obj.x + halfW) && y in (obj.y - halfH)..(obj.y + halfH)) {
                return obj.id
            }
        }
        return null
    }
}
