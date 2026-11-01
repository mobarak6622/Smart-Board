package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.data.WhiteboardRepository
import com.example.engine.CanvasRenderEngine
import com.example.model.BackgroundType
import com.example.model.BoardPage
import com.example.model.CanvasObject
import com.example.model.CanvasObjectType
import com.example.model.CanvasStroke
import com.example.model.DrawingToolType
import com.example.model.MathFormulaType
import com.example.model.PointFData
import com.example.model.ProtractorState
import com.example.model.RulerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SmartBoard Pro", appName)
    }

    @Test
    fun `serialize and deserialize multi-page board with strokes and math objects`() {
        val stroke = CanvasStroke(
            toolType = DrawingToolType.NEON_PEN,
            colorArgb = 0xFF2563EBL,
            strokeWidth = 10f,
            alpha = 0.9f,
            points = listOf(PointFData(100f, 120f), PointFData(200f, 240f))
        )
        val mathObj = CanvasObject(
            type = CanvasObjectType.MATH_FORMULA,
            x = 400f,
            y = 500f,
            mathType = MathFormulaType.FRACTION,
            primaryArg = "a + b",
            secondaryArg = "2c"
        )
        val page = BoardPage(
            backgroundType = BackgroundType.COORDINATE_GRAPH,
            strokes = listOf(stroke),
            objects = listOf(mathObj)
        )

        val json = WhiteboardRepository.serializePages(listOf(page))
        val restored = WhiteboardRepository.deserializePages(json)

        assertEquals(1, restored.size)
        assertEquals(BackgroundType.COORDINATE_GRAPH, restored[0].backgroundType)
        assertEquals(1, restored[0].strokes.size)
        assertEquals(DrawingToolType.NEON_PEN, restored[0].strokes[0].toolType)
        assertEquals(1, restored[0].objects.size)
        assertEquals("a + b", restored[0].objects[0].primaryArg)
    }

    @Test
    fun `render page to native android canvas and verify ruler snapping`() {
        val bmp = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val page = BoardPage(
            backgroundType = BackgroundType.SQUARE_GRID,
            strokes = listOf(
                CanvasStroke(
                    toolType = DrawingToolType.PEN,
                    colorArgb = 0xFF0F172AL,
                    strokeWidth = 6f,
                    points = listOf(PointFData(50f, 50f), PointFData(250f, 250f))
                )
            )
        )
        CanvasRenderEngine.renderPageToAndroidCanvas(canvas, 600f, 800f, page)
        assertNotNull(bmp)

        val ruler = RulerState(enabled = true, centerX = 300f, centerY = 400f, angleDegrees = 0f, lengthPx = 400f)
        val snapped = CanvasRenderEngine.snapPointToInstruments(
            rawX = 320f,
            rawY = 350f,
            ruler = ruler,
            protractor = ProtractorState(enabled = false)
        )
        assertTrue(snapped.y < 360f)

        // Verify point-to-segment Stroke Eraser hit detection along a fast long stroke
        val hitId = CanvasRenderEngine.findHitStrokeId(page, 150f, 152f)
        assertEquals(page.strokes.first().id, hitId)
    }
}
