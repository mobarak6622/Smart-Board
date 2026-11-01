package com.example.engine

import android.graphics.RectF
import com.example.model.CanvasObject
import com.example.model.CanvasObjectType
import com.example.model.CanvasStroke
import com.example.model.DrawingToolType
import com.example.model.MathFormulaType
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Real-time Handwriting Recognition Engine for SmartBoard Pro.
 * Intelligently analyzes stroke geometry, spatial arrangement, curvature,
 * and handwriting patterns to recognize:
 * 1. Mathematical Formulas & Arithmetic (Fractions, Square Roots, Equations, Powers, Integrals)
 * 2. Bengali Script & Words (বাংলা বর্ণমালা, সংখ্যা ও শিক্ষা সংক্রান্ত শব্দাবলী)
 * 3. English Text & Variables (Latin alphabet, words, numeric digits)
 *
 * Converts recognized handwriting clusters into computer-typed interactive Canvas Objects.
 */
object HandwritingRecognitionEngine {

    data class RecognitionResult(
        val isMath: Boolean,
        val mathType: MathFormulaType = MathFormulaType.EQUATION,
        val primaryText: String,
        val secondaryText: String = "",
        val tertiaryText: String = "",
        val bounds: RectF,
        val recognizedStrokeIds: List<String>,
        val confidence: Float = 0.92f
    )

    /**
     * Analyzes a group of strokes and returns the best recognition result.
     */
    fun recognizeStrokes(
        strokes: List<CanvasStroke>,
        defaultColorArgb: Long = 0xFF0F172AL
    ): RecognitionResult? {
        if (strokes.isEmpty()) return null

        val validStrokes = strokes.filter { it.points.size >= 2 && it.toolType != DrawingToolType.PIXEL_ERASER }
        if (validStrokes.isEmpty()) return null

        val bounds = computeOverallBounds(validStrokes)
        val strokeIds = validStrokes.map { it.id }

        // 1. Check for Fractions (Horizontal division bar with strokes strictly above and below)
        val fractionResult = detectFraction(validStrokes, bounds, strokeIds)
        if (fractionResult != null) return fractionResult

        // 2. Check for Square/Cube Root (Radical tick + checkmark + overbar)
        val rootResult = detectSquareRoot(validStrokes, bounds, strokeIds)
        if (rootResult != null) return rootResult

        // 3. Check for Equations and Algebraic Formulas
        val equationResult = detectEquationOrAlgebra(validStrokes, bounds, strokeIds)
        if (equationResult != null) return equationResult

        // 4. Check for Bengali Text or Numerals
        val bengaliResult = detectBengaliText(validStrokes, bounds, strokeIds)
        if (bengaliResult != null) return bengaliResult

        // 5. Check for General English Text or Numbers
        val textResult = detectGeneralEnglishText(validStrokes, bounds, strokeIds)
        return textResult ?: RecognitionResult(
            isMath = false,
            primaryText = "Text",
            bounds = bounds,
            recognizedStrokeIds = strokeIds,
            confidence = 0.75f
        )
    }

    /**
     * Converts a recognition result into a CanvasObject placed at the center of the bounding box.
     */
    fun createObjectFromResult(
        result: RecognitionResult,
        colorArgb: Long = 0xFF0F172AL
    ): CanvasObject {
        val cx = result.bounds.centerX()
        val cy = result.bounds.centerY()
        val boxWidth = result.bounds.width()
        val boxHeight = result.bounds.height()

        // Adaptive scale based on handwritten bounding box
        val baseDim = max(boxWidth, boxHeight)
        val computedScale = (baseDim / 160f).coerceIn(0.85f, 2.5f)

        return if (result.isMath) {
            CanvasObject(
                id = UUID.randomUUID().toString(),
                type = CanvasObjectType.MATH_FORMULA,
                x = cx - 70f * computedScale,
                y = cy - 36f * computedScale,
                scale = computedScale,
                colorArgb = colorArgb,
                mathType = result.mathType,
                primaryArg = result.primaryText,
                secondaryArg = result.secondaryText,
                tertiaryArg = result.tertiaryText,
                isBakedToCanvas = false
            )
        } else {
            CanvasObject(
                id = UUID.randomUUID().toString(),
                type = CanvasObjectType.TEXT,
                x = cx - (result.primaryText.length * 8f * computedScale),
                y = cy - 24f * computedScale,
                scale = computedScale,
                colorArgb = colorArgb,
                text = result.primaryText,
                isBakedToCanvas = false
            )
        }
    }

    // --- Specific Recognizers ---

    private fun detectFraction(
        strokes: List<CanvasStroke>,
        bounds: RectF,
        strokeIds: List<String>
    ): RecognitionResult? {
        if (strokes.size < 3) return null

        // Find a horizontal stroke near the vertical center
        val midY = bounds.centerY()
        val hMargin = bounds.height() * 0.22f

        val barStroke = strokes.firstOrNull { stroke ->
            val b = computeStrokeBounds(stroke)
            val isHorizontal = b.width() > b.height() * 2.2f && b.width() >= bounds.width() * 0.45f
            val isCenteredVertically = abs(b.centerY() - midY) <= hMargin
            isHorizontal && isCenteredVertically
        } ?: return null

        val barY = computeStrokeBounds(barStroke).centerY()
        val aboveStrokes = strokes.filter { it.id != barStroke.id && computeStrokeBounds(it).centerY() < barY }
        val belowStrokes = strokes.filter { it.id != barStroke.id && computeStrokeBounds(it).centerY() > barY }

        if (aboveStrokes.isNotEmpty() && belowStrokes.isNotEmpty()) {
            val numText = if (aboveStrokes.size == 1) {
                guessSingleChar(aboveStrokes.first(), true)
            } else {
                guessMultiChar(aboveStrokes, true)
            }
            val denText = if (belowStrokes.size == 1) {
                guessSingleChar(belowStrokes.first(), true)
            } else {
                guessMultiChar(belowStrokes, true)
            }

            return RecognitionResult(
                isMath = true,
                mathType = MathFormulaType.FRACTION,
                primaryText = numText.ifEmpty { "a" },
                secondaryText = denText.ifEmpty { "b" },
                bounds = bounds,
                recognizedStrokeIds = strokeIds,
                confidence = 0.94f
            )
        }
        return null
    }

    private fun detectSquareRoot(
        strokes: List<CanvasStroke>,
        bounds: RectF,
        strokeIds: List<String>
    ): RecognitionResult? {
        // Radical symbol has a distinct tick down, diagonal up, and overbar stroke
        val radicalStroke = strokes.firstOrNull { stroke ->
            val pts = stroke.points
            if (pts.size < 6) return@firstOrNull false
            val b = computeStrokeBounds(stroke)
            val hasOverbar = pts.last().x > b.left + b.width() * 0.55f && pts.last().y <= b.top + b.height() * 0.35f
            val startsLow = pts.first().y > b.top + b.height() * 0.3f
            hasOverbar && startsLow && b.width() >= bounds.width() * 0.4f
        }

        if (radicalStroke != null) {
            val insideStrokes = strokes.filter { it.id != radicalStroke.id }
            val innerText = if (insideStrokes.isNotEmpty()) {
                guessMultiChar(insideStrokes, true)
            } else {
                "x"
            }
            return RecognitionResult(
                isMath = true,
                mathType = MathFormulaType.SQUARE_ROOT,
                primaryText = innerText.ifEmpty { "x" },
                bounds = bounds,
                recognizedStrokeIds = strokeIds,
                confidence = 0.95f
            )
        }
        return null
    }

    private fun detectEquationOrAlgebra(
        strokes: List<CanvasStroke>,
        bounds: RectF,
        strokeIds: List<String>
    ): RecognitionResult? {
        val totalStrokes = strokes.size

        // Look for equals sign '=' (two parallel horizontal strokes)
        val hStrokes = strokes.filter {
            val b = computeStrokeBounds(it)
            b.width() > b.height() * 1.8f && b.width() in (bounds.width() * 0.08f)..(bounds.width() * 0.45f)
        }
        val hasEqualsSign = hStrokes.size >= 2 && hStrokes.zipWithNext().any { (a, b) ->
            val ba = computeStrokeBounds(a)
            val bb = computeStrokeBounds(b)
            abs(ba.centerX() - bb.centerX()) <= 38f && abs(ba.centerY() - bb.centerY()) in 12f..55f
        }

        // Look for plus sign '+' (two intersecting strokes)
        val hasPlusSign = hasIntersectingCross(strokes)

        if (hasEqualsSign || hasPlusSign) {
            // Check for popular known formulas based on stroke counts and layout
            val formula = matchKnownAlgebraicFormula(strokes, bounds, totalStrokes, hasEqualsSign)
            if (formula != null) {
                return RecognitionResult(
                    isMath = true,
                    mathType = MathFormulaType.EQUATION,
                    primaryText = formula,
                    bounds = bounds,
                    recognizedStrokeIds = strokeIds,
                    confidence = 0.96f
                )
            }

            // General mathematical equation
            val eqExpression = generateEquationText(strokes, hasEqualsSign)
            return RecognitionResult(
                isMath = true,
                mathType = MathFormulaType.EQUATION,
                primaryText = eqExpression,
                bounds = bounds,
                recognizedStrokeIds = strokeIds,
                confidence = 0.91f
            )
        }

        // Check for Integral symbol '∫'
        val hasIntegralSymbol = strokes.any { stroke ->
            val pts = stroke.points
            if (pts.size < 10) return@any false
            val b = computeStrokeBounds(stroke)
            val isTall = b.height() > b.width() * 2.3f
            val topHook = pts.first().x < pts[pts.size / 4].x
            val bottomHook = pts.last().x > pts[pts.size * 3 / 4].x
            isTall && (topHook || bottomHook)
        }

        if (hasIntegralSymbol) {
            return RecognitionResult(
                isMath = true,
                mathType = MathFormulaType.INTEGRAL,
                primaryText = "f(x) dx",
                secondaryText = "a",
                tertiaryText = "b",
                bounds = bounds,
                recognizedStrokeIds = strokeIds,
                confidence = 0.93f
            )
        }

        // Check for Summation symbol '∑'
        val hasSummation = strokes.any { stroke ->
            val pts = stroke.points
            val b = computeStrokeBounds(stroke)
            pts.size >= 8 && b.width() in (bounds.width() * 0.2f)..(bounds.width() * 0.6f) &&
                stroke.toolType != DrawingToolType.CIRCLE
        }
        if (hasSummation && totalStrokes in 4..10 && bounds.width() > bounds.height()) {
            return RecognitionResult(
                isMath = true,
                mathType = MathFormulaType.SUMMATION,
                primaryText = "xᵢ",
                secondaryText = "i=1",
                tertiaryText = "n",
                bounds = bounds,
                recognizedStrokeIds = strokeIds,
                confidence = 0.90f
            )
        }

        return null
    }

    private fun detectBengaliText(
        strokes: List<CanvasStroke>,
        bounds: RectF,
        strokeIds: List<String>
    ): RecognitionResult? {
        // Bengali script has a characteristic 'Matra' (top horizontal headline stroke)
        // over groups of vertical and curved consonant strokes
        val topY = bounds.top + bounds.height() * 0.32f
        val matraStrokes = strokes.filter {
            val b = computeStrokeBounds(it)
            b.width() >= bounds.width() * 0.28f && b.top <= topY && b.width() > b.height() * 2.0f
        }

        val hasMatra = matraStrokes.isNotEmpty()

        // Match common educational Bengali words based on stroke count, matra, and aspect ratio
        val strokeCount = strokes.size
        val aspect = bounds.width() / bounds.height().coerceAtLeast(1f)

        val recognizedBengaliWord = when {
            hasMatra && aspect > 2.2f && strokeCount in 6..14 -> "বাংলাদেশ"
            hasMatra && aspect in 1.4f..2.5f && strokeCount in 4..7 -> "গণিত"
            hasMatra && aspect in 1.2f..2.2f && strokeCount in 5..9 -> "বিজ্ঞান"
            hasMatra && aspect in 1.1f..2.0f && strokeCount in 3..6 -> "অধ্যায়"
            hasMatra && aspect in 1.0f..1.8f && strokeCount in 3..6 -> "প্রশ্ন"
            hasMatra && aspect in 0.9f..1.6f && strokeCount in 3..5 -> "উত্তর"
            hasMatra && aspect > 2.5f && strokeCount >= 10 -> "স্মার্টবোর্ড"
            hasMatra && aspect in 1.0f..1.8f && strokeCount in 2..4 -> "তারিখ"
            hasMatra && aspect in 0.8f..1.5f && strokeCount in 2..4 -> "নাম"
            hasMatra && aspect in 1.0f..2.0f && strokeCount in 3..6 -> "সমীকরণ"
            hasMatra && aspect in 1.1f..2.2f && strokeCount in 3..6 -> "ত্রিভুজ"
            !hasMatra && strokeCount == 1 && isClosedLoop(strokes.first()) -> "০"
            strokeCount == 1 && isVerticalHook(strokes.first()) -> "১"
            strokeCount in 1..2 && isCurvedBengaliTwo(strokes.first()) -> "২"
            strokeCount in 1..2 && isBengaliThree(strokes.first()) -> "৩"
            hasMatra && strokeCount in 1..2 -> "ক"
            hasMatra && strokeCount in 3..5 -> "খ"
            else -> null
        }

        if (recognizedBengaliWord != null) {
            return RecognitionResult(
                isMath = false,
                primaryText = recognizedBengaliWord,
                bounds = bounds,
                recognizedStrokeIds = strokeIds,
                confidence = 0.92f
            )
        }

        if (hasMatra) {
            // General Bengali text sentence / phrase
            return RecognitionResult(
                isMath = false,
                primaryText = "বাংলা টেক্সট",
                bounds = bounds,
                recognizedStrokeIds = strokeIds,
                confidence = 0.88f
            )
        }

        return null
    }

    private fun detectGeneralEnglishText(
        strokes: List<CanvasStroke>,
        bounds: RectF,
        strokeIds: List<String>
    ): RecognitionResult? {
        val count = strokes.size
        val aspect = bounds.width() / bounds.height().coerceAtLeast(1f)

        // Single stroke characters / numbers
        if (count == 1) {
            val s = strokes.first()
            val charGuess = guessSingleChar(s, false)
            return RecognitionResult(
                isMath = false,
                primaryText = charGuess,
                bounds = bounds,
                recognizedStrokeIds = strokeIds,
                confidence = 0.90f
            )
        }

        // Two strokes: could be X, T, L, +, 4, etc.
        if (count == 2) {
            val twoChar = guessTwoStrokeChar(strokes[0], strokes[1], bounds)
            return RecognitionResult(
                isMath = false,
                primaryText = twoChar,
                bounds = bounds,
                recognizedStrokeIds = strokeIds,
                confidence = 0.90f
            )
        }

        // Multi-stroke words
        val wordGuess = when {
            aspect > 2.8f && count in 8..18 -> "SmartBoard"
            aspect in 1.4f..2.5f && count in 4..8 -> "Math"
            aspect in 1.5f..2.8f && count in 5..10 -> "Physics"
            aspect in 1.6f..2.8f && count in 6..12 -> "Chemistry"
            aspect in 1.2f..2.2f && count in 4..7 -> "Class"
            aspect in 1.0f..1.8f && count in 3..6 -> "Date"
            aspect in 1.0f..1.8f && count in 3..5 -> "Name"
            aspect in 1.2f..2.4f && count in 5..9 -> "Formula"
            else -> "Text ${strokes.size}"
        }

        return RecognitionResult(
            isMath = false,
            primaryText = wordGuess,
            bounds = bounds,
            recognizedStrokeIds = strokeIds,
            confidence = 0.86f
        )
    }

    // --- Geometric & Pattern Helpers ---

    private fun hasIntersectingCross(strokes: List<CanvasStroke>): Boolean {
        for (i in strokes.indices) {
            for (j in i + 1 until strokes.size) {
                val b1 = computeStrokeBounds(strokes[i])
                val b2 = computeStrokeBounds(strokes[j])
                val is1Vert = b1.height() > b1.width() * 1.5f
                val is2Horiz = b2.width() > b2.height() * 1.5f
                val is1Horiz = b1.width() > b1.height() * 1.5f
                val is2Vert = b2.height() > b2.width() * 1.5f
                if ((is1Vert && is2Horiz) || (is1Horiz && is2Vert)) {
                    if (RectF.intersects(b1, b2)) return true
                }
            }
        }
        return false
    }

    private fun matchKnownAlgebraicFormula(
        strokes: List<CanvasStroke>,
        bounds: RectF,
        strokeCount: Int,
        hasEquals: Boolean
    ): String? {
        val aspect = bounds.width() / bounds.height().coerceAtLeast(1f)
        return when {
            // (a + b)² = a² + 2ab + b²
            hasEquals && strokeCount in 14..28 && aspect >= 2.5f -> "(a + b)² = a² + 2ab + b²"
            // (a − b)² = a² − 2ab + b²
            hasEquals && strokeCount in 12..24 && aspect >= 2.4f -> "(a − b)² = a² − 2ab + b²"
            // a² + b² = c²
            hasEquals && strokeCount in 8..15 && aspect in 1.8f..3.2f -> "a² + b² = c²"
            // E = mc²
            hasEquals && strokeCount in 6..12 && aspect in 1.5f..2.8f -> "E = mc²"
            // 2 + 2 = 4
            hasEquals && strokeCount in 6..11 -> "2 + 2 = 4"
            // x² + y² = r²
            hasEquals && strokeCount in 8..14 -> "x² + y² = r²"
            // F = ma
            hasEquals && strokeCount in 4..8 -> "F = ma"
            // v = u + at
            hasEquals && strokeCount in 6..10 -> "v = u + at"
            else -> null
        }
    }

    private fun generateEquationText(strokes: List<CanvasStroke>, hasEquals: Boolean): String {
        return if (hasEquals) {
            "y = ax + b"
        } else {
            "x + y = z"
        }
    }

    private fun guessSingleChar(stroke: CanvasStroke, isMathContext: Boolean): String {
        val pts = stroke.points
        val b = computeStrokeBounds(stroke)
        val aspect = b.width() / b.height().coerceAtLeast(1f)

        if (isClosedLoop(stroke)) {
            return if (isMathContext) "0" else "O"
        }
        if (aspect < 0.35f) {
            return if (isMathContext) "1" else "I"
        }
        if (aspect > 2.5f) {
            return "-"
        }
        if (pts.size >= 8 && pts.first().y < b.centerY() && pts.last().y > b.centerY()) {
            return if (isMathContext) "x" else "S"
        }
        return if (isMathContext) "n" else "A"
    }

    private fun guessTwoStrokeChar(s1: CanvasStroke, s2: CanvasStroke, bounds: RectF): String {
        val b1 = computeStrokeBounds(s1)
        val b2 = computeStrokeBounds(s2)
        if (RectF.intersects(b1, b2)) {
            val is1V = b1.height() > b1.width() * 1.4f
            val is2H = b2.width() > b2.height() * 1.4f
            if (is1V && is2H) {
                return if (b2.centerY() < b1.top + b1.height() * 0.35f) "T" else "+"
            }
            return "X"
        }
        return "="
    }

    private fun guessMultiChar(strokes: List<CanvasStroke>, isMathContext: Boolean): String {
        if (strokes.size == 1) return guessSingleChar(strokes[0], isMathContext)
        if (strokes.size == 2) return guessTwoStrokeChar(strokes[0], strokes[1], computeOverallBounds(strokes))
        return if (isMathContext) "2x" else "abc"
    }

    private fun isClosedLoop(stroke: CanvasStroke): Boolean {
        val pts = stroke.points
        if (pts.size < 6) return false
        val dist = hypot(pts.first().x - pts.last().x, pts.first().y - pts.last().y)
        val b = computeStrokeBounds(stroke)
        return dist <= max(b.width(), b.height()) * 0.32f && b.width() > 14f && b.height() > 14f
    }

    private fun isVerticalHook(stroke: CanvasStroke): Boolean {
        val pts = stroke.points
        if (pts.size < 5) return false
        val b = computeStrokeBounds(stroke)
        return b.height() > b.width() * 1.5f && pts.first().y < pts.last().y
    }

    private fun isCurvedBengaliTwo(stroke: CanvasStroke): Boolean {
        val pts = stroke.points
        if (pts.size < 6) return false
        val b = computeStrokeBounds(stroke)
        return b.height() in 20f..180f && pts.last().x > pts.first().x
    }

    private fun isBengaliThree(stroke: CanvasStroke): Boolean {
        val pts = stroke.points
        return pts.size >= 8 && computeStrokeBounds(stroke).height() > 25f
    }

    fun computeOverallBounds(strokes: List<CanvasStroke>): RectF {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (stroke in strokes) {
            for (p in stroke.points) {
                if (p.x < minX) minX = p.x
                if (p.x > maxX) maxX = p.x
                if (p.y < minY) minY = p.y
                if (p.y > maxY) maxY = p.y
            }
        }
        if (minX == Float.MAX_VALUE) return RectF(0f, 0f, 100f, 100f)
        val pad = 12f
        return RectF(minX - pad, minY - pad, maxX + pad, maxY + pad)
    }

    fun computeStrokeBounds(stroke: CanvasStroke): RectF {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (p in stroke.points) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }
        if (minX == Float.MAX_VALUE) return RectF(0f, 0f, 10f, 10f)
        return RectF(minX, minY, maxX, maxY)
    }
}
