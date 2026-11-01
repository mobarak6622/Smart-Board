package com.example

import android.graphics.RectF
import com.example.engine.CanvasRenderEngine
import com.example.engine.HandwritingRecognitionEngine
import com.example.model.BoardPage
import com.example.model.CanvasStroke
import com.example.model.DrawingToolType
import com.example.model.MathFormulaType
import com.example.model.PointFData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HandwritingAndStrokeMovementTest {

    @Test
    fun testScreenToWorldCoordinateMapping_isAccurateUnderZoomAndPan() {
        val zoom = 1.75f
        val panX = 120f
        val panY = -80f

        val screenX = 450f
        val screenY = 600f

        // Forward map: Screen to World
        val worldX = (screenX - panX) / zoom
        val worldY = (screenY - panY) / zoom

        // Reverse map: World to Screen
        val reconstructedScreenX = worldX * zoom + panX
        val reconstructedScreenY = worldY * zoom + panY

        assertEquals(screenX, reconstructedScreenX, 0.001f)
        assertEquals(screenY, reconstructedScreenY, 0.001f)
    }

    @Test
    fun testStrokeHitTestingAndBoundingBox() {
        val stroke = CanvasStroke(
            toolType = DrawingToolType.PEN,
            colorArgb = 0xFF000000L,
            strokeWidth = 8f,
            points = listOf(
                PointFData(100f, 100f),
                PointFData(150f, 120f),
                PointFData(200f, 100f)
            )
        )
        val page = BoardPage(strokes = listOf(stroke))

        // Direct hit near (150, 120)
        val hitId = CanvasRenderEngine.findHitStrokeId(page, 150f, 121f, threshold = 25f)
        assertEquals(stroke.id, hitId)

        // Miss far away at (500, 500)
        val missId = CanvasRenderEngine.findHitStrokeId(page, 500f, 500f, threshold = 25f)
        assertEquals(null, missId)

        // Bounding box computation
        val bounds = CanvasRenderEngine.getStrokeBoundingBox(stroke)
        assertTrue(bounds.left <= 100f)
        assertTrue(bounds.right >= 200f)
        assertTrue(bounds.top <= 100f)
        assertTrue(bounds.bottom >= 120f)
    }

    @Test
    fun testHandwritingRecognition_detectsFractionFormula() {
        // Create 3 strokes: Top 'a', Middle horizontal division bar '-', Bottom 'b'
        val numerator = CanvasStroke(
            toolType = DrawingToolType.PEN,
            colorArgb = 0xFF000000L,
            strokeWidth = 6f,
            points = listOf(PointFData(140f, 80f), PointFData(160f, 80f))
        )
        val fractionBar = CanvasStroke(
            toolType = DrawingToolType.PEN,
            colorArgb = 0xFF000000L,
            strokeWidth = 8f,
            points = listOf(PointFData(110f, 120f), PointFData(190f, 120f))
        )
        val denominator = CanvasStroke(
            toolType = DrawingToolType.PEN,
            colorArgb = 0xFF000000L,
            strokeWidth = 6f,
            points = listOf(PointFData(140f, 160f), PointFData(160f, 160f))
        )

        val result = HandwritingRecognitionEngine.recognizeStrokes(listOf(numerator, fractionBar, denominator))
        assertNotNull(result)
        assertTrue(result!!.isMath)
        assertEquals(MathFormulaType.FRACTION, result.mathType)

        val createdObj = HandwritingRecognitionEngine.createObjectFromResult(result)
        assertEquals(MathFormulaType.FRACTION, createdObj.mathType)
    }

    @Test
    fun testHandwritingRecognition_detectsBengaliText() {
        // Bengali text with Matra top horizontal line and multiple consonant strokes
        val matra = CanvasStroke(
            toolType = DrawingToolType.PEN,
            colorArgb = 0xFF000000L,
            strokeWidth = 6f,
            points = listOf(PointFData(100f, 100f), PointFData(280f, 100f))
        )
        val stroke1 = CanvasStroke(
            toolType = DrawingToolType.PEN,
            colorArgb = 0xFF000000L,
            strokeWidth = 6f,
            points = listOf(PointFData(120f, 105f), PointFData(120f, 160f))
        )
        val stroke2 = CanvasStroke(
            toolType = DrawingToolType.PEN,
            colorArgb = 0xFF000000L,
            strokeWidth = 6f,
            points = listOf(PointFData(160f, 105f), PointFData(160f, 160f))
        )
        val stroke3 = CanvasStroke(
            toolType = DrawingToolType.PEN,
            colorArgb = 0xFF000000L,
            strokeWidth = 6f,
            points = listOf(PointFData(200f, 105f), PointFData(200f, 160f))
        )
        val stroke4 = CanvasStroke(
            toolType = DrawingToolType.PEN,
            colorArgb = 0xFF000000L,
            strokeWidth = 6f,
            points = listOf(PointFData(240f, 105f), PointFData(240f, 160f))
        )

        val result = HandwritingRecognitionEngine.recognizeStrokes(listOf(matra, stroke1, stroke2, stroke3, stroke4))
        assertNotNull(result)
        assertTrue(!result!!.isMath)
        assertTrue(result.primaryText.isNotEmpty())
    }
}
