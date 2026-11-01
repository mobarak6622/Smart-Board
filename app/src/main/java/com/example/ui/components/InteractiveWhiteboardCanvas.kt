package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.CanvasRenderEngine
import com.example.engine.GeometryInstrumentOverlay
import com.example.localization.AppLanguage
import com.example.localization.StudioStrings
import com.example.model.CanvasStroke
import com.example.model.DrawingToolType
import com.example.model.PointFData
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioSlate900
import com.example.viewmodel.WhiteboardUiState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class GestureTarget {
    NONE,
    HAND_PAN,
    RULER_MOVE,
    RULER_ROTATE,
    PROTRACTOR_MOVE,
    PROTRACTOR_RAY,
    COMPASS_PIVOT,
    COMPASS_RADIUS,
    CANVAS_OBJECT_MOVE,
    STROKE_ERASER,
    DRAW_STROKE
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InteractiveWhiteboardCanvas(
    uiState: WhiteboardUiState,
    strings: StudioStrings,
    onCommitStroke: (CanvasStroke) -> Unit,
    onEraseStrokeAt: (Float, Float) -> Unit,
    onSelectObject: (String?) -> Unit,
    onMoveSelectedObject: (Float, Float) -> Unit,
    onMoveObjectById: (String, Float, Float) -> Unit,
    onScaleSelectedObject: (Float) -> Unit,
    onScaleObjectById: (String, Float) -> Unit,
    onUpdateMathObject: (String, String, String, String) -> Unit,
    onFlattenMathObjectToCanvas: (String) -> Unit,
    onDeleteSelectedObject: () -> Unit,
    onDeleteObjectById: (String) -> Unit,
    onZoomPanChange: (Float, Float, Float) -> Unit,
    onResetZoomPan: () -> Unit,
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onToggleHandPan: () -> Unit = {},
    onUpdateTextObject: (String, String) -> Unit = { _, _ -> },
    onRulerMove: (Float, Float) -> Unit,
    onRulerRotate: (Float, Float) -> Unit,
    onProtractorMove: (Float, Float) -> Unit,
    onProtractorGuide: (Float, Float) -> Unit,
    onCompassPivotMove: (Float, Float) -> Unit,
    onCompassRadiusAdjust: (Float, Float) -> Unit,
    onStampCompassArc: () -> Unit,
    onCanvasTouchStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePoints = remember { mutableStateListOf<PointFData>() }
    var isDrawingActive by remember { mutableStateOf(false) }
    var redrawTick by remember { mutableIntStateOf(0) }

    val zoomScale = uiState.zoomScale
    val panOffsetX = uiState.panOffsetX
    val panOffsetY = uiState.panOffsetY
    val canDrawOnBoard = uiState.canCurrentUserDraw

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("whiteboard_canvas")
                .pointerInput(
                    uiState.activeTool,
                    uiState.selectedColorArgb,
                    uiState.strokeWidth,
                    uiState.strokeAlpha,
                    uiState.polygonSides,
                    uiState.currentPageIndex,
                    uiState.ruler.enabled,
                    uiState.protractor.enabled,
                    uiState.compass.enabled,
                    uiState.studioPreferences.enableTwoFingerZoomPan,
                    canDrawOnBoard
                ) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        onCanvasTouchStarted()

                        var localZoom = uiState.zoomScale.coerceIn(0.5f, 4f)
                        var localPanX = uiState.panOffsetX
                        var localPanY = uiState.panOffsetY

                        // Convert screen touch to world/canvas coordinates
                        val startWorldX = (down.position.x - localPanX) / localZoom
                        val startWorldY = (down.position.y - localPanY) / localZoom

                        var target = if (!canDrawOnBoard) {
                            GestureTarget.HAND_PAN
                        } else {
                            resolveGestureTarget(
                                x = startWorldX,
                                y = startWorldY,
                                uiState = uiState,
                                onSelectObject = onSelectObject
                            )
                        }

                        when (target) {
                            GestureTarget.STROKE_ERASER -> {
                                onEraseStrokeAt(startWorldX, startWorldY)
                            }

                            GestureTarget.DRAW_STROKE -> {
                                activePoints.clear()
                                val snapped = CanvasRenderEngine.snapPointToInstruments(
                                    rawX = startWorldX,
                                    rawY = startWorldY,
                                    ruler = uiState.ruler,
                                    protractor = uiState.protractor
                                )
                                activePoints.add(snapped)
                                isDrawingActive = true
                                redrawTick++
                            }

                            else -> Unit
                        }

                        var prevWorldX = startWorldX
                        var prevWorldY = startWorldY
                        var prevScreenX = down.position.x
                        var prevScreenY = down.position.y
                        var multiTouchMode = false

                        do {
                            val event = awaitPointerEvent()
                            val pressedChanges = event.changes.filter { it.pressed }

                            // 2-Finger Pinch-to-Zoom and Pan ("দুটোই চাই")
                            if (pressedChanges.size >= 2 && uiState.studioPreferences.enableTwoFingerZoomPan) {
                                if (!multiTouchMode) {
                                    multiTouchMode = true
                                    isDrawingActive = false
                                    activePoints.clear()
                                    target = GestureTarget.NONE
                                }
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = false)

                                val oldScale = localZoom
                                val newScale = (oldScale * zoomChange).coerceIn(0.5f, 4.0f)
                                val scaleRatio = if (oldScale > 0.001f) newScale / oldScale else 1f

                                localPanX = centroid.x - (centroid.x - localPanX) * scaleRatio + panChange.x
                                localPanY = centroid.y - (centroid.y - localPanY) * scaleRatio + panChange.y
                                localZoom = newScale

                                onZoomPanChange(localZoom, localPanX, localPanY)
                                event.changes.forEach { it.consume() }
                                continue
                            }

                            if (multiTouchMode) {
                                event.changes.forEach { it.consume() }
                                continue
                            }

                            val change = event.changes.firstOrNull() ?: break
                            val curScreenX = change.position.x
                            val curScreenY = change.position.y
                            val curWorldX = (curScreenX - localPanX) / localZoom
                            val curWorldY = (curScreenY - localPanY) / localZoom

                            val worldDx = curWorldX - prevWorldX
                            val worldDy = curWorldY - prevWorldY
                            val screenDx = curScreenX - prevScreenX
                            val screenDy = curScreenY - prevScreenY

                            if (change.pressed) {
                                when (target) {
                                    GestureTarget.HAND_PAN -> {
                                        localPanX += screenDx
                                        localPanY += screenDy
                                        onZoomPanChange(
                                            localZoom,
                                            localPanX,
                                            localPanY
                                        )
                                    }

                                    GestureTarget.RULER_MOVE -> onRulerMove(worldDx, worldDy)
                                    GestureTarget.RULER_ROTATE -> onRulerRotate(curWorldX, curWorldY)
                                    GestureTarget.PROTRACTOR_MOVE -> onProtractorMove(worldDx, worldDy)
                                    GestureTarget.PROTRACTOR_RAY -> onProtractorGuide(curWorldX, curWorldY)
                                    GestureTarget.COMPASS_PIVOT -> onCompassPivotMove(worldDx, worldDy)
                                    GestureTarget.COMPASS_RADIUS -> onCompassRadiusAdjust(curWorldX, curWorldY)
                                    GestureTarget.CANVAS_OBJECT_MOVE -> onMoveSelectedObject(worldDx, worldDy)
                                    GestureTarget.STROKE_ERASER -> onEraseStrokeAt(curWorldX, curWorldY)
                                    GestureTarget.DRAW_STROKE -> {
                                        if (isShapeTool(uiState.activeTool)) {
                                            val snapped = CanvasRenderEngine.snapPointToInstruments(
                                                rawX = curWorldX,
                                                rawY = curWorldY,
                                                ruler = uiState.ruler,
                                                protractor = uiState.protractor
                                            )
                                            if (activePoints.size >= 2) {
                                                activePoints[1] = snapped
                                            } else {
                                                activePoints.add(snapped)
                                            }
                                        } else {
                                            // Process historical motion samples for ultra-smooth fast handwriting
                                            for (hist in change.historical) {
                                                val hWorldX = (hist.position.x - localPanX) / localZoom
                                                val hWorldY = (hist.position.y - localPanY) / localZoom
                                                val hSnapped = CanvasRenderEngine.snapPointToInstruments(
                                                    rawX = hWorldX,
                                                    rawY = hWorldY,
                                                    ruler = uiState.ruler,
                                                    protractor = uiState.protractor
                                                )
                                                val last = activePoints.lastOrNull()
                                                if (last == null || hypot(hSnapped.x - last.x, hSnapped.y - last.y) >= 1.8f) {
                                                    activePoints.add(hSnapped)
                                                }
                                            }
                                            val snapped = CanvasRenderEngine.snapPointToInstruments(
                                                rawX = curWorldX,
                                                rawY = curWorldY,
                                                ruler = uiState.ruler,
                                                protractor = uiState.protractor
                                            )
                                            val last = activePoints.lastOrNull()
                                            if (last == null || hypot(snapped.x - last.x, snapped.y - last.y) >= 1.8f) {
                                                activePoints.add(snapped)
                                            }
                                        }
                                        redrawTick++
                                    }

                                    GestureTarget.NONE -> Unit
                                }
                                prevWorldX = curWorldX
                                prevWorldY = curWorldY
                                prevScreenX = curScreenX
                                prevScreenY = curScreenY
                                change.consume()
                            }
                        } while (event.changes.any { it.pressed })

                        if (!multiTouchMode && target == GestureTarget.DRAW_STROKE && activePoints.isNotEmpty() && canDrawOnBoard) {
                            val committedStroke = CanvasStroke(
                                toolType = uiState.activeTool,
                                colorArgb = uiState.selectedColorArgb,
                                strokeWidth = uiState.strokeWidth,
                                alpha = uiState.strokeAlpha,
                                points = activePoints.toList(),
                                polygonSides = uiState.polygonSides
                            )
                            isDrawingActive = false
                            activePoints.clear()
                            onCommitStroke(committedStroke)
                        } else {
                            isDrawingActive = false
                            activePoints.clear()
                        }
                    }
                }
        ) {
            // Read redrawTick so Compose invalidates during active path motion
            val tick = redrawTick
            val liveStroke = if (isDrawingActive && activePoints.isNotEmpty() && tick >= 0) {
                CanvasStroke(
                    toolType = uiState.activeTool,
                    colorArgb = uiState.selectedColorArgb,
                    strokeWidth = uiState.strokeWidth,
                    alpha = uiState.strokeAlpha,
                    points = activePoints.toList(),
                    polygonSides = uiState.polygonSides
                )
            } else null

            val nativeCanvas = drawContext.canvas.nativeCanvas
            val saveCount = nativeCanvas.save()
            nativeCanvas.translate(panOffsetX, panOffsetY)
            nativeCanvas.scale(zoomScale, zoomScale)

            CanvasRenderEngine.renderPageToAndroidCanvas(
                canvas = nativeCanvas,
                width = size.width,
                height = size.height,
                page = uiState.currentPage,
                activeStroke = liveStroke,
                selectedObjectId = uiState.selectedObjectId,
                renderUnbakedMathNodes = false
            )

            // Draw interactive Geometry Instruments on top inside world space
            GeometryInstrumentOverlay.drawInstruments(
                canvas = nativeCanvas,
                ruler = uiState.ruler,
                protractor = uiState.protractor,
                compass = uiState.compass
            )
            nativeCanvas.restoreToCount(saveCount)
        }

        // 2. Interactive On-Canvas Compose Math & Text Overlay Component ("দুটোই চাই" — Live Compose Nodes + Stamp to Canvas)
        OnCanvasMathOverlay(
            objects = uiState.currentPage.objects,
            selectedObjectId = uiState.selectedObjectId,
            zoomScale = zoomScale,
            panOffsetX = panOffsetX,
            panOffsetY = panOffsetY,
            strings = strings,
            onSelectObject = onSelectObject,
            onMoveObject = onMoveObjectById,
            onScaleObject = onScaleObjectById,
            onUpdateMathObject = onUpdateMathObject,
            onFlattenMathObjectToCanvas = onFlattenMathObjectToCanvas,
            onDeleteObject = onDeleteObjectById,
            onUpdateTextObject = onUpdateTextObject
        )

        // 3. Floating Quick Zoom, Unzoom & Pan Controller Bar
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = StudioSlate900.copy(alpha = 0.94f),
            tonalElevation = 8.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 82.dp, start = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                // Zoom Out / Unzoom (-)
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out / Unzoom",
                        tint = PureWhite,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Current Zoom Percentage & 100% Reset Clickable Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { onResetZoomPan() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("reset_zoom_pan_pill")
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Reset Zoom",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${(zoomScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Zoom In (+)
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = PureWhite,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Hand Pan Quick Toggle (Pan with 1 finger without drawing)
                val isPanActive = uiState.activeTool == DrawingToolType.HAND_PAN
                IconButton(
                    onClick = onToggleHandPan,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isPanActive) ElectricIndigo else Color.Transparent)
                        .testTag("quick_pan_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PanTool,
                        contentDescription = "Hand Pan Canvas",
                        tint = if (isPanActive) PureWhite else Color(0xFF94A3B8),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // 4. Student View-Only Mode Indicator Pill when inside an online classroom without draw permission
        AnimatedVisibility(
            visible = uiState.activeClassroom != null && !canDrawOnBoard,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 82.dp, end = 14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = StudioSlate900.copy(alpha = 0.92f),
                tonalElevation = 6.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (uiState.language == AppLanguage.BENGALI)
                            "ভিউ-অনলি মোড (জুম ও প্যান সচল)"
                        else
                            "View-Only Mode (Pan & Zoom Active)",
                        style = MaterialTheme.typography.labelMedium,
                        color = PureWhite
                    )
                }
            }
        }

        // 5. Contextual Action Bar for Selected Canvas Object (Text, Stamped Math Formula, or Image)
        val selectedObj = uiState.currentPage.objects.firstOrNull { it.id == uiState.selectedObjectId }
        val showStandardObjectBar = selectedObj != null &&
            (selectedObj.type != com.example.model.CanvasObjectType.MATH_FORMULA || selectedObj.isBakedToCanvas)

        AnimatedVisibility(
            visible = showStandardObjectBar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 86.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = StudioSlate900.copy(alpha = 0.94f),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { onScaleSelectedObject(-0.15f) },
                        modifier = Modifier.testTag("scale_down_object_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = strings.scaleDown,
                            tint = PureWhite
                        )
                    }
                    IconButton(
                        onClick = { onScaleSelectedObject(0.15f) },
                        modifier = Modifier.testTag("scale_up_object_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = strings.scaleUp,
                            tint = PureWhite
                        )
                    }
                    IconButton(
                        onClick = onDeleteSelectedObject,
                        modifier = Modifier.testTag("delete_object_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = strings.deleteSelectedObject,
                            tint = Color(0xFFF87171)
                        )
                    }
                }
            }
        }

        // 6. Contextual Action Button when Compass is enabled so user can stamp the precision circle/arc
        AnimatedVisibility(
            visible = uiState.compass.enabled,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 86.dp, end = 16.dp)
        ) {
            FilledTonalButton(
                onClick = onStampCompassArc,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = ElectricIndigo,
                    contentColor = PureWhite
                ),
                modifier = Modifier.testTag("stamp_compass_circle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = strings.drawCompassArc,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.drawCompassArc,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

private fun isShapeTool(tool: DrawingToolType): Boolean {
    return tool == DrawingToolType.LINE ||
        tool == DrawingToolType.ARROW ||
        tool == DrawingToolType.CIRCLE ||
        tool == DrawingToolType.RECTANGLE ||
        tool == DrawingToolType.TRIANGLE ||
        tool == DrawingToolType.POLYGON
}

private fun resolveGestureTarget(
    x: Float,
    y: Float,
    uiState: WhiteboardUiState,
    onSelectObject: (String?) -> Unit
): GestureTarget {
    if (uiState.activeTool == DrawingToolType.HAND_PAN) {
        return GestureTarget.HAND_PAN
    }

    // 1. Check Compass Handles
    if (uiState.compass.enabled) {
        val c = uiState.compass
        val tipX = c.pivotX + c.radiusPx
        val tipY = c.pivotY
        if (hypot(x - tipX, y - tipY) <= 44f) {
            return GestureTarget.COMPASS_RADIUS
        }
        if (hypot(x - c.pivotX, y - c.pivotY) <= 44f) {
            return GestureTarget.COMPASS_PIVOT
        }
    }

    // 2. Check Protractor Handles
    if (uiState.protractor.enabled) {
        val p = uiState.protractor
        val totalRad = Math.toRadians((p.rotationDegrees - p.guideAngleDegrees).toDouble())
        val handleX = p.centerX + (p.radiusPx + 36f) * cos(totalRad).toFloat()
        val handleY = p.centerY + (p.radiusPx + 36f) * sin(totalRad).toFloat()
        if (hypot(x - handleX, y - handleY) <= 48f) {
            return GestureTarget.PROTRACTOR_RAY
        }
        val distFromCenter = hypot(x - p.centerX, y - p.centerY)
        if (distFromCenter <= p.radiusPx - 28f && y <= p.centerY + 24f) {
            return GestureTarget.PROTRACTOR_MOVE
        }
    }

    // 3. Check Ruler Handles
    if (uiState.ruler.enabled) {
        val r = uiState.ruler
        val rad = Math.toRadians(r.angleDegrees.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val dx = x - r.centerX
        val dy = y - r.centerY
        val localX = dx * cosA + dy * sinA
        val localY = -dx * sinA + dy * cosA
        val halfLen = r.lengthPx / 2f
        if (localX in (halfLen - 64f)..(halfLen + 24f) && localY in -46f..46f) {
            return GestureTarget.RULER_ROTATE
        }
        if (localX in (-halfLen + 20f)..(halfLen - 65f) && localY in -32f..32f) {
            return GestureTarget.RULER_MOVE
        }
    }

    // 4. Check CanvasObject hit if in SELECT_MOVE mode or tapping directly on an object
    val hitObjId = CanvasRenderEngine.findHitObjectId(uiState.currentPage, x, y)
    if (uiState.activeTool == DrawingToolType.SELECT_MOVE) {
        onSelectObject(hitObjId)
        return if (hitObjId != null) GestureTarget.CANVAS_OBJECT_MOVE else GestureTarget.NONE
    } else if (hitObjId != null && hitObjId == uiState.selectedObjectId) {
        return GestureTarget.CANVAS_OBJECT_MOVE
    } else if (uiState.selectedObjectId != null) {
        onSelectObject(null)
    }

    // 5. Eraser or Drawing Tool
    if (uiState.activeTool == DrawingToolType.STROKE_ERASER) {
        return GestureTarget.STROKE_ERASER
    }

    return GestureTarget.DRAW_STROKE
}
