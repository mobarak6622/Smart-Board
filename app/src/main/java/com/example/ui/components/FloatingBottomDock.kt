package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PanoramaFishEye
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.StudioStrings
import com.example.model.BackgroundType
import com.example.model.DrawingToolType
import com.example.ui.theme.DrawingPaletteColors
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioSlate200
import com.example.ui.theme.StudioSlate700
import com.example.ui.theme.StudioSlate900
import com.example.viewmodel.WhiteboardUiState
import kotlin.math.roundToInt
import kotlin.math.sin

enum class DockPanelType {
    NONE,
    BRUSHES,
    ERASERS,
    SHAPES,
    GEOMETRY,
    BACKGROUNDS,
    INSERT
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingBottomDock(
    uiState: WhiteboardUiState,
    strings: StudioStrings,
    activePanel: DockPanelType,
    showQuickStrip: Boolean,
    onToggleQuickStrip: () -> Unit,
    onTogglePanel: (DockPanelType) -> Unit,
    onDismissPanel: () -> Unit,
    onSelectTool: (DrawingToolType) -> Unit,
    onSelectColor: (Long) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onStrokeAlphaChange: (Float) -> Unit,
    onPolygonSidesChange: (Int) -> Unit,
    onToggleRuler: () -> Unit,
    onToggleProtractor: () -> Unit,
    onToggleCompass: () -> Unit,
    onSelectBackground: (BackgroundType) -> Unit,
    onOpenMathStudio: () -> Unit,
    onOpenTextDialog: () -> Unit,
    onPickGalleryImage: () -> Unit,
    onClearCanvas: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Contextual Popup Tool Menu Card (Responsive & Scrollable for Small Screens)
        AnimatedVisibility(
            visible = activePanel != DockPanelType.NONE,
            enter = slideInVertically(animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)) { it / 2 } + fadeIn(),
            exit = slideOutVertically(animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)) { it / 2 } + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .padding(bottom = 6.dp)
                    .border(1.dp, StudioSlate200, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = PureWhite.copy(alpha = 0.98f),
                tonalElevation = 12.dp,
                shadowElevation = 14.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (activePanel) {
                        DockPanelType.BRUSHES -> BrushesPopupPanel(
                            uiState = uiState,
                            strings = strings,
                            showCustomColorPicker = showCustomColorPicker,
                            onToggleCustomColorPicker = { showCustomColorPicker = !showCustomColorPicker },
                            onSelectTool = onSelectTool,
                            onSelectColor = onSelectColor,
                            onStrokeWidthChange = onStrokeWidthChange,
                            onStrokeAlphaChange = onStrokeAlphaChange,
                            onClose = onDismissPanel
                        )

                        DockPanelType.ERASERS -> ErasersPopupPanel(
                            uiState = uiState,
                            strings = strings,
                            onSelectTool = onSelectTool,
                            onStrokeWidthChange = onStrokeWidthChange,
                            onClearCanvas = onClearCanvas,
                            onClose = onDismissPanel
                        )

                        DockPanelType.SHAPES -> ShapesPopupPanel(
                            uiState = uiState,
                            strings = strings,
                            onSelectTool = onSelectTool,
                            onPolygonSidesChange = onPolygonSidesChange,
                            onClose = onDismissPanel
                        )

                        DockPanelType.GEOMETRY -> GeometryPopupPanel(
                            uiState = uiState,
                            strings = strings,
                            onToggleRuler = onToggleRuler,
                            onToggleProtractor = onToggleProtractor,
                            onToggleCompass = onToggleCompass,
                            onClose = onDismissPanel
                        )

                        DockPanelType.BACKGROUNDS -> BackgroundsPopupPanel(
                            currentBg = uiState.currentPage.backgroundType,
                            strings = strings,
                            onSelectBackground = onSelectBackground,
                            onClose = onDismissPanel
                        )

                        DockPanelType.INSERT -> InsertPopupPanel(
                            uiState = uiState,
                            strings = strings,
                            onSelectMoveTool = { onSelectTool(DrawingToolType.SELECT_MOVE) },
                            onOpenTextDialog = onOpenTextDialog,
                            onPickGalleryImage = onPickGalleryImage,
                            onClose = onDismissPanel
                        )

                        DockPanelType.NONE -> Unit
                    }
                }
            }
        }

        // 2. Dual-Mode Quick Color Palette & Pen Thickness Strip ("দুটোই চাই" — Toggleable inline strip)
        AnimatedVisibility(
            visible = showQuickStrip && activePanel == DockPanelType.NONE,
            enter = expandVertically(animationSpec = spring()) + fadeIn(),
            exit = shrinkVertically(animationSpec = spring()) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .fillMaxWidth()
                    .padding(bottom = 5.dp)
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = StudioSlate900.copy(alpha = 0.94f),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Horizontal Scrollable Professional Color Palette
                    Row(
                        modifier = Modifier
                            .weight(1.15f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrawingPaletteColors.forEachIndexed { idx, argb ->
                            val isSelected = uiState.selectedColorArgb == argb
                            val swatchSize by animateDpAsState(
                                targetValue = if (isSelected) 26.dp else 22.dp,
                                label = "QuickSwatchSize"
                            )
                            Box(
                                modifier = Modifier
                                    .size(swatchSize)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) PureWhite else Color(0xFF475569),
                                        shape = CircleShape
                                    )
                                    .clickable { onSelectColor(argb) }
                                    .testTag("quick_color_swatch_$idx")
                            )
                        }

                        // Open Full Color Picker in Brushes Panel
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ElectricIndigo)
                                .clickable {
                                    showCustomColorPicker = true
                                    onTogglePanel(DockPanelType.BRUSHES)
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = strings.customColor,
                                tint = PureWhite,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color(0xFF334155))
                    )

                    // Compact Pen Thickness Slider with Live Size Dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(0.95f)
                    ) {
                        val dotSize = (uiState.strokeWidth / 2.6f).coerceIn(4f, 18f).dp
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(dotSize)
                                    .clip(CircleShape)
                                    .background(Color(uiState.selectedColorArgb))
                                    .border(1.dp, PureWhite, CircleShape)
                            )
                        }
                        Slider(
                            value = uiState.strokeWidth,
                            onValueChange = onStrokeWidthChange,
                            valueRange = 2f..56f,
                            colors = SliderDefaults.colors(
                                thumbColor = PureWhite,
                                activeTrackColor = Color(0xFF38BDF8),
                                inactiveTrackColor = Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .testTag("quick_thickness_slider")
                        )
                        Text(
                            text = "${uiState.strokeWidth.roundToInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(22.dp)
                        )
                    }
                }
            }
        }

        // 3. Ultra-Compact Primary Bottom Dock
        Surface(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = StudioSlate900.copy(alpha = 0.96f),
            tonalElevation = 10.dp,
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val prefs = uiState.studioPreferences

                if (prefs.showBrushesTab) {
                    DockTabButton(
                        icon = Icons.Default.Brush,
                        label = strings.tabDraw,
                        selected = activePanel == DockPanelType.BRUSHES || isBrushTool(uiState.activeTool),
                        badgeColor = Color(uiState.selectedColorArgb),
                        onClick = { onTogglePanel(DockPanelType.BRUSHES) },
                        testTag = "dock_brushes_button"
                    )
                }

                if (prefs.showEraserTab) {
                    DockTabButton(
                        icon = Icons.Default.CleaningServices,
                        label = strings.tabEraser,
                        selected = activePanel == DockPanelType.ERASERS ||
                            uiState.activeTool == DrawingToolType.PIXEL_ERASER ||
                            uiState.activeTool == DrawingToolType.STROKE_ERASER,
                        onClick = { onTogglePanel(DockPanelType.ERASERS) },
                        testTag = "dock_eraser_button"
                    )
                }

                if (prefs.showShapesTab) {
                    DockTabButton(
                        icon = Icons.Default.Category,
                        label = strings.tabShapes,
                        selected = activePanel == DockPanelType.SHAPES || isShapeTool(uiState.activeTool),
                        onClick = { onTogglePanel(DockPanelType.SHAPES) },
                        testTag = "dock_shapes_button"
                    )
                }

                if (prefs.showGeometryTab) {
                    DockTabButton(
                        icon = Icons.Default.Straighten,
                        label = strings.tabGeometry,
                        selected = activePanel == DockPanelType.GEOMETRY ||
                            uiState.ruler.enabled ||
                            uiState.protractor.enabled ||
                            uiState.compass.enabled,
                        onClick = { onTogglePanel(DockPanelType.GEOMETRY) },
                        testTag = "dock_geometry_button"
                    )
                }

                if (prefs.showMathTab) {
                    DockTabButton(
                        icon = Icons.Default.Functions,
                        label = strings.tabMath,
                        selected = false,
                        onClick = onOpenMathStudio,
                        testTag = "dock_math_button"
                    )
                }

                if (prefs.showInsertTab) {
                    DockTabButton(
                        icon = Icons.Default.TextFields,
                        label = strings.tabInsert,
                        selected = activePanel == DockPanelType.INSERT ||
                            uiState.activeTool == DrawingToolType.SELECT_MOVE,
                        onClick = { onTogglePanel(DockPanelType.INSERT) },
                        testTag = "dock_insert_button"
                    )
                }

                if (prefs.showBackgroundTab) {
                    DockTabButton(
                        icon = Icons.Default.GridOn,
                        label = strings.tabBackground,
                        selected = activePanel == DockPanelType.BACKGROUNDS,
                        onClick = { onTogglePanel(DockPanelType.BACKGROUNDS) },
                        testTag = "dock_background_button"
                    )
                }

                if (prefs.showHandPanTool) {
                    DockTabButton(
                        icon = Icons.Default.OpenWith,
                        label = if (uiState.language == com.example.localization.AppLanguage.BENGALI) "প্যান" else "Pan",
                        selected = uiState.activeTool == DrawingToolType.HAND_PAN,
                        onClick = {
                            onDismissPanel()
                            if (uiState.activeTool == DrawingToolType.HAND_PAN) {
                                onSelectTool(DrawingToolType.PEN)
                            } else {
                                onSelectTool(DrawingToolType.HAND_PAN)
                            }
                        },
                        testTag = "dock_hand_pan_button"
                    )
                }

                // Toggle Quick Color & Thickness Strip Button
                DockTabButton(
                    icon = if (showQuickStrip) Icons.Default.Tune else Icons.Default.Palette,
                    label = "${uiState.strokeWidth.roundToInt()}px",
                    selected = showQuickStrip,
                    badgeColor = Color(uiState.selectedColorArgb),
                    onClick = onToggleQuickStrip,
                    testTag = "quick_bar_toggle_button"
                )
            }
        }
    }
}

@Composable
private fun DockTabButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badgeColor: Color? = null,
    onClick: () -> Unit,
    testTag: String
) {
    val bg = if (selected) ElectricIndigo else Color.Transparent
    val contentColor = if (selected) PureWhite else Color(0xFFCBD5E1)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .testTag(testTag)
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
            if (badgeColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(badgeColor)
                        .border(1.dp, PureWhite, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun PopupPanelHeader(
    title: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = StudioSlate900
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = StudioSlate700,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrushesPopupPanel(
    uiState: WhiteboardUiState,
    strings: StudioStrings,
    showCustomColorPicker: Boolean,
    onToggleCustomColorPicker: () -> Unit,
    onSelectTool: (DrawingToolType) -> Unit,
    onSelectColor: (Long) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onStrokeAlphaChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PopupPanelHeader(title = strings.tabDraw, onClose = onClose)

        // 6 Brush Modes Row
        val brushTools = listOf(
            Triple(DrawingToolType.PEN, strings.pen, Icons.Default.Edit),
            Triple(DrawingToolType.PENCIL, strings.pencil, Icons.Default.Create),
            Triple(DrawingToolType.BRUSH, strings.brush, Icons.Default.Brush),
            Triple(DrawingToolType.HIGHLIGHTER, strings.highlighter, Icons.Default.FormatColorFill),
            Triple(DrawingToolType.MARKER, strings.marker, Icons.Default.Edit),
            Triple(DrawingToolType.NEON_PEN, strings.neonPen, Icons.Default.AutoFixHigh)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            brushTools.forEach { (tool, name, icon) ->
                FilterChip(
                    selected = uiState.activeTool == tool,
                    onClick = { onSelectTool(tool) },
                    label = { Text(name, style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricIndigo,
                        selectedLabelColor = PureWhite,
                        selectedLeadingIconColor = PureWhite
                    ),
                    modifier = Modifier.testTag("tool_${tool.name.lowercase()}")
                )
            }
        }

        // Live Stroke Preview Wave
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF8FAFC))
                .border(1.dp, StudioSlate200, RoundedCornerShape(10.dp))
        ) {
            val strokeColor = Color(uiState.selectedColorArgb).copy(alpha = uiState.strokeAlpha)
            var prevX = 24f
            var prevY = size.height / 2f
            val endX = size.width - 24f
            var x = 28f
            while (x <= endX) {
                val progress = (x - 24f) / (endX - 24f).coerceAtLeast(1f)
                val y = (size.height / 2f) + sin(progress * Math.PI * 2.5).toFloat() * 7f
                drawLine(
                    color = strokeColor,
                    start = Offset(prevX, prevY),
                    end = Offset(x, y),
                    strokeWidth = uiState.strokeWidth,
                    cap = StrokeCap.Round
                )
                prevX = x
                prevY = y
                x += 8f
            }
        }

        // 24 Curated Color Swatches + Custom Color Picker Button
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DrawingPaletteColors.forEachIndexed { idx, argb ->
                val isSelected = uiState.selectedColorArgb == argb
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) ElectricIndigo else StudioSlate200,
                            shape = CircleShape
                        )
                        .clickable { onSelectColor(argb) }
                        .testTag("color_swatch_$idx")
                )
            }

            // Custom Color Picker Trigger
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(StudioSlate900)
                    .clickable { onToggleCustomColorPicker() }
                    .testTag("custom_color_picker_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = strings.customColor,
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (showCustomColorPicker) {
            CustomHsvColorPicker(
                strings = strings,
                onApplyColor = { customArgb ->
                    onSelectColor(customArgb)
                    onToggleCustomColorPicker()
                }
            )
        }

        HorizontalDivider(color = StudioSlate200)

        // Stroke Thickness & Opacity Sliders
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${strings.thickness}: ${uiState.strokeWidth.roundToInt()}px",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(132.dp)
            )
            Slider(
                value = uiState.strokeWidth,
                onValueChange = onStrokeWidthChange,
                valueRange = 2f..56f,
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .testTag("thickness_slider")
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${strings.opacity}: ${(uiState.strokeAlpha * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(132.dp)
            )
            Slider(
                value = uiState.strokeAlpha,
                onValueChange = onStrokeAlphaChange,
                valueRange = 0.15f..1f,
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .testTag("opacity_slider")
            )
        }
    }
}

@Composable
private fun CustomHsvColorPicker(
    strings: StudioStrings,
    onApplyColor: (Long) -> Unit
) {
    var hue by remember { mutableFloatStateOf(210f) }
    var saturation by remember { mutableFloatStateOf(0.85f) }
    var value by remember { mutableFloatStateOf(0.92f) }

    val previewInt = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
    val previewLong = (previewInt.toLong() and 0xFFFFFFFFL)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF1F5F9))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.customColor, style = MaterialTheme.typography.titleSmall)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(previewLong))
                    .border(1.dp, StudioSlate700, CircleShape)
            )
        }
        Text("${strings.hue}: ${hue.roundToInt()}°", style = MaterialTheme.typography.labelSmall)
        Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f, modifier = Modifier.height(24.dp))
        Text("${strings.saturation}: ${(saturation * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall)
        Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f, modifier = Modifier.height(24.dp))
        Text("${strings.brightness}: ${(value * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall)
        Slider(value = value, onValueChange = { value = it }, valueRange = 0.1f..1f, modifier = Modifier.height(24.dp))
        Button(
            onClick = { onApplyColor(previewLong) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.applyColor)
        }
    }
}

@Composable
private fun ErasersPopupPanel(
    uiState: WhiteboardUiState,
    strings: StudioStrings,
    onSelectTool: (DrawingToolType) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onClearCanvas: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PopupPanelHeader(title = strings.tabEraser, onClose = onClose)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = uiState.activeTool == DrawingToolType.STROKE_ERASER,
                onClick = { onSelectTool(DrawingToolType.STROKE_ERASER) },
                label = { Text(strings.strokeEraser) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricIndigo,
                    selectedLabelColor = PureWhite
                ),
                modifier = Modifier.testTag("tool_stroke_eraser")
            )
            FilterChip(
                selected = uiState.activeTool == DrawingToolType.PIXEL_ERASER,
                onClick = { onSelectTool(DrawingToolType.PIXEL_ERASER) },
                label = { Text(strings.pixelEraser) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricIndigo,
                    selectedLabelColor = PureWhite
                ),
                modifier = Modifier.testTag("tool_pixel_eraser")
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${strings.thickness}: ${uiState.strokeWidth.roundToInt()}px",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(132.dp)
            )
            Slider(
                value = uiState.strokeWidth,
                onValueChange = onStrokeWidthChange,
                valueRange = 8f..64f,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedButton(
            onClick = onClearCanvas,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("panel_clear_canvas_button")
        ) {
            Text(strings.clearCanvas, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShapesPopupPanel(
    uiState: WhiteboardUiState,
    strings: StudioStrings,
    onSelectTool: (DrawingToolType) -> Unit,
    onPolygonSidesChange: (Int) -> Unit,
    onClose: () -> Unit
) {
    val shapes = listOf(
        Triple(DrawingToolType.LINE, strings.straightLine, Icons.Default.HorizontalRule),
        Triple(DrawingToolType.ARROW, strings.arrow, Icons.AutoMirrored.Filled.CallMade),
        Triple(DrawingToolType.CIRCLE, strings.circle, Icons.Default.PanoramaFishEye),
        Triple(DrawingToolType.RECTANGLE, strings.rectangle, Icons.Default.CropSquare),
        Triple(DrawingToolType.TRIANGLE, strings.triangle, Icons.Default.ChangeHistory),
        Triple(DrawingToolType.POLYGON, strings.polygon, Icons.Default.Hexagon)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PopupPanelHeader(title = strings.tabShapes, onClose = onClose)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            shapes.forEach { (tool, label, icon) ->
                FilterChip(
                    selected = uiState.activeTool == tool,
                    onClick = { onSelectTool(tool) },
                    label = { Text(label) },
                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricIndigo,
                        selectedLabelColor = PureWhite,
                        selectedLeadingIconColor = PureWhite
                    ),
                    modifier = Modifier.testTag("tool_${tool.name.lowercase()}")
                )
            }
        }

        if (uiState.activeTool == DrawingToolType.POLYGON) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("${strings.polygonSidesLabel}:", style = MaterialTheme.typography.labelLarge)
                (5..8).forEach { sides ->
                    FilterChip(
                        selected = uiState.polygonSides == sides,
                        onClick = { onPolygonSidesChange(sides) },
                        label = { Text("$sides") }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeometryPopupPanel(
    uiState: WhiteboardUiState,
    strings: StudioStrings,
    onToggleRuler: () -> Unit,
    onToggleProtractor: () -> Unit,
    onToggleCompass: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PopupPanelHeader(title = strings.tabGeometry, onClose = onClose)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = uiState.ruler.enabled,
                onClick = onToggleRuler,
                label = { Text(strings.ruler) },
                leadingIcon = { Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricIndigo,
                    selectedLabelColor = PureWhite,
                    selectedLeadingIconColor = PureWhite
                ),
                modifier = Modifier.testTag("toggle_ruler_chip")
            )

            FilterChip(
                selected = uiState.protractor.enabled,
                onClick = onToggleProtractor,
                label = { Text(strings.protractor) },
                leadingIcon = { Icon(Icons.Default.TrackChanges, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricIndigo,
                    selectedLabelColor = PureWhite,
                    selectedLeadingIconColor = PureWhite
                ),
                modifier = Modifier.testTag("toggle_protractor_chip")
            )

            FilterChip(
                selected = uiState.compass.enabled,
                onClick = onToggleCompass,
                label = { Text(strings.compass) },
                leadingIcon = { Icon(Icons.Default.Architecture, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricIndigo,
                    selectedLabelColor = PureWhite,
                    selectedLeadingIconColor = PureWhite
                ),
                modifier = Modifier.testTag("toggle_compass_chip")
            )
        }

        Text(
            text = strings.snapHint,
            style = MaterialTheme.typography.bodyMedium,
            color = StudioSlate700
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackgroundsPopupPanel(
    currentBg: BackgroundType,
    strings: StudioStrings,
    onSelectBackground: (BackgroundType) -> Unit,
    onClose: () -> Unit
) {
    val options = listOf(
        BackgroundType.PLAIN_WHITE to strings.bgPlainWhite,
        BackgroundType.SQUARE_GRID to strings.bgGrid,
        BackgroundType.DOT_GRID to strings.bgDots,
        BackgroundType.RULED_LINES to strings.bgRuled,
        BackgroundType.COORDINATE_GRAPH to strings.bgCoordinateGraph
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PopupPanelHeader(title = strings.tabBackground, onClose = onClose)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (bgType, label) ->
                FilterChip(
                    selected = currentBg == bgType,
                    onClick = { onSelectBackground(bgType) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricIndigo,
                        selectedLabelColor = PureWhite
                    ),
                    modifier = Modifier.testTag("bg_${bgType.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun InsertPopupPanel(
    uiState: WhiteboardUiState,
    strings: StudioStrings,
    onSelectMoveTool: () -> Unit,
    onOpenTextDialog: () -> Unit,
    onPickGalleryImage: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PopupPanelHeader(title = strings.tabInsert, onClose = onClose)

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onOpenTextDialog,
                modifier = Modifier
                    .weight(1f)
                    .testTag("insert_text_button")
            ) {
                Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addTextBtn)
            }

            OutlinedButton(
                onClick = onPickGalleryImage,
                modifier = Modifier
                    .weight(1f)
                    .testTag("insert_image_button")
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addImageBtn)
            }
        }

        FilterChip(
            selected = uiState.activeTool == DrawingToolType.SELECT_MOVE,
            onClick = onSelectMoveTool,
            label = { Text(strings.selectMove) },
            leadingIcon = { Icon(Icons.Default.OpenWith, contentDescription = null, modifier = Modifier.size(16.dp)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ElectricIndigo,
                selectedLabelColor = PureWhite,
                selectedLeadingIconColor = PureWhite
            ),
            modifier = Modifier.testTag("tool_select_move")
        )
    }
}

private fun isBrushTool(tool: DrawingToolType): Boolean {
    return tool == DrawingToolType.PEN ||
        tool == DrawingToolType.PENCIL ||
        tool == DrawingToolType.BRUSH ||
        tool == DrawingToolType.HIGHLIGHTER ||
        tool == DrawingToolType.MARKER ||
        tool == DrawingToolType.NEON_PEN
}

private fun isShapeTool(tool: DrawingToolType): Boolean {
    return tool == DrawingToolType.LINE ||
        tool == DrawingToolType.ARROW ||
        tool == DrawingToolType.CIRCLE ||
        tool == DrawingToolType.RECTANGLE ||
        tool == DrawingToolType.TRIANGLE ||
        tool == DrawingToolType.POLYGON
}
