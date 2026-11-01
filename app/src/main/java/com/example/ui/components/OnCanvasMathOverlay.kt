package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.StudioStrings
import com.example.model.CanvasObject
import com.example.model.CanvasObjectType
import com.example.model.MathFormulaType
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioSlate200
import com.example.ui.theme.StudioSlate700
import com.example.ui.theme.StudioSlate900
import kotlin.math.roundToInt

/**
 * Interactive On-Canvas Compose Math Overlay Component.
 * Renders mathematical formulas (Fractions, Square/Cube Roots, Integrals, Summations,
 * Powers, Subscripts, and Equations) using custom Jetpack Compose layout nodes directly
 * over the canvas, while also allowing one-tap stamping/flattening into static canvas strokes.
 */
@Composable
fun OnCanvasMathOverlay(
    objects: List<CanvasObject>,
    selectedObjectId: String?,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    strings: StudioStrings,
    onSelectObject: (String?) -> Unit,
    onMoveObject: (String, Float, Float) -> Unit,
    onScaleObject: (String, Float) -> Unit,
    onUpdateMathObject: (String, String, String, String) -> Unit,
    onFlattenMathObjectToCanvas: (String) -> Unit,
    onDeleteObject: (String) -> Unit,
    onUpdateTextObject: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val mathNodes = remember(objects) {
        objects.filter { it.type == CanvasObjectType.MATH_FORMULA && !it.isBakedToCanvas }
    }
    val textNodes = remember(objects) {
        objects.filter { it.type == CanvasObjectType.TEXT && !it.isBakedToCanvas }
    }
    var inlineEditingObjectId by remember { mutableStateOf<String?>(null) }
    var editingTextObject by remember { mutableStateOf<CanvasObject?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        mathNodes.forEach { mathObj ->
            val isSelected = mathObj.id == selectedObjectId
            val screenX = (mathObj.x * zoomScale + panOffsetX).roundToInt()
            val screenY = (mathObj.y * zoomScale + panOffsetY).roundToInt()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset { IntOffset(screenX - 110, screenY - 56) }
                    .scale((mathObj.scale * zoomScale).coerceIn(0.45f, 3.5f))
                    .pointerInput(mathObj.id, zoomScale) {
                        detectDragGestures(
                            onDragStart = { onSelectObject(mathObj.id) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onMoveObject(
                                    mathObj.id,
                                    dragAmount.x / zoomScale.coerceAtLeast(0.25f),
                                    dragAmount.y / zoomScale.coerceAtLeast(0.25f)
                                )
                            }
                        )
                    }
                    .testTag("canvas_math_node_${mathObj.id}")
            ) {
                // Floating Mini Action Toolbar when this Math Node is Selected
                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StudioSlate900.copy(alpha = 0.95f),
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    inlineEditingObjectId =
                                        if (inlineEditingObjectId == mathObj.id) null else mathObj.id
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("edit_math_node_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Formula Inline",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { onScaleObject(mathObj.id, -0.15f) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = strings.scaleDown,
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { onScaleObject(mathObj.id, 0.15f) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = strings.scaleUp,
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Stamp / Flatten to Native Canvas Vector Layer ("দুটোই চাই")
                            IconButton(
                                onClick = { onFlattenMathObjectToCanvas(mathObj.id) },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("stamp_math_node_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Stamp to Canvas",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDeleteObject(mathObj.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = strings.deleteSelectedObject,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Custom Compose Math Node Surface
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PureWhite.copy(alpha = 0.96f),
                    tonalElevation = if (isSelected) 8.dp else 3.dp,
                    shadowElevation = if (isSelected) 8.dp else 2.dp,
                    modifier = Modifier
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) ElectricIndigo else StudioSlate200,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectObject(mathObj.id) }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ComposeMathFormulaNode(mathObj = mathObj)
                    }
                }
            }
        }

        // 2. Interactive On-Canvas Text Nodes (Drag to Move, Double-Tap to Edit, Scale & Delete)
        textNodes.forEach { textObj ->
            val isSelected = textObj.id == selectedObjectId
            val screenX = (textObj.x * zoomScale + panOffsetX).roundToInt()
            val screenY = (textObj.y * zoomScale + panOffsetY).roundToInt()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset { IntOffset(screenX - 100, screenY - 45) }
                    .scale((textObj.scale * zoomScale).coerceIn(0.45f, 3.5f))
                    .pointerInput(textObj.id) {
                        detectTapGestures(
                            onTap = { onSelectObject(textObj.id) },
                            onDoubleTap = {
                                onSelectObject(textObj.id)
                                editingTextObject = textObj
                            }
                        )
                    }
                    .pointerInput(textObj.id, zoomScale) {
                        detectDragGestures(
                            onDragStart = { onSelectObject(textObj.id) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onMoveObject(
                                    textObj.id,
                                    dragAmount.x / zoomScale.coerceAtLeast(0.25f),
                                    dragAmount.y / zoomScale.coerceAtLeast(0.25f)
                                )
                            }
                        )
                    }
                    .testTag("canvas_text_node_${textObj.id}")
            ) {
                // Floating Action Toolbar when Selected
                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StudioSlate900.copy(alpha = 0.95f),
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { editingTextObject = textObj },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("edit_text_node_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Text",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onScaleObject(textObj.id, -0.15f) },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("scale_down_text_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = strings.scaleDown,
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onScaleObject(textObj.id, 0.15f) },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("scale_up_text_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = strings.scaleUp,
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onFlattenMathObjectToCanvas(textObj.id) },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("stamp_text_node_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Stamp to Canvas",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteObject(textObj.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = strings.deleteSelectedObject,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Text Display Surface (Double-click to edit hint, drag to move)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFFEFF6FF) else Color.White.copy(alpha = 0.94f),
                    tonalElevation = if (isSelected) 8.dp else 2.dp,
                    shadowElevation = if (isSelected) 6.dp else 2.dp,
                    modifier = Modifier
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) ElectricIndigo else Color(0xFFCBD5E1),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = textObj.text.ifBlank { "Text" },
                        color = Color(textObj.colorArgb),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Inline On-Canvas Formula Quick Editor Card
        val editingObj = mathNodes.firstOrNull { it.id == inlineEditingObjectId }
        if (editingObj != null) {
            InlineMathNodeEditorCard(
                mathObj = editingObj,
                strings = strings,
                onSave = { p, s, t ->
                    onUpdateMathObject(editingObj.id, p, s, t)
                    inlineEditingObjectId = null
                },
                onDismiss = { inlineEditingObjectId = null },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        }

        // Dialog for editing text on double-tap or Edit icon
        if (editingTextObject != null) {
            val target = editingTextObject!!
            var editValue by remember(target.id) { mutableStateOf(target.text) }
            AlertDialog(
                onDismissRequest = { editingTextObject = null },
                title = { Text(strings.insertTextTitle) },
                text = {
                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onUpdateTextObject(target.id, editValue)
                            editingTextObject = null
                        }
                    ) {
                        Text(strings.addTextBtn)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingTextObject = null }) {
                        Text(strings.cancelBtn)
                    }
                }
            )
        }
    }
}

@Composable
fun ComposeMathFormulaNode(mathObj: CanvasObject) {
    val inkColor = Color(mathObj.colorArgb)
    val density = LocalDensity.current

    when (mathObj.mathType) {
        MathFormulaType.FRACTION -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(IntrinsicSize.Max)
            ) {
                Text(
                    text = mathObj.primaryArg.ifBlank { "a" },
                    color = inkColor,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                HorizontalDivider(
                    thickness = 2.dp,
                    color = inkColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )
                Text(
                    text = mathObj.secondaryArg.ifBlank { "b" },
                    color = inkColor,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }

        MathFormulaType.SQUARE_ROOT, MathFormulaType.CUBE_ROOT -> {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mathObj.mathType == MathFormulaType.CUBE_ROOT) {
                    Text(
                        text = "3",
                        color = inkColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .drawBehind {
                            val strokePx = with(density) { 2.dp.toPx() }
                            val path = Path().apply {
                                moveTo(0f, size.height * 0.55f)
                                lineTo(size.width * 0.08f, size.height * 0.88f)
                                lineTo(size.width * 0.17f, strokePx)
                                lineTo(size.width, strokePx)
                            }
                            drawPath(
                                path = path,
                                color = inkColor,
                                style = Stroke(
                                    width = strokePx,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                        .padding(start = 18.dp, top = 5.dp, end = 4.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = mathObj.primaryArg.ifBlank { "x" },
                        color = inkColor,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        MathFormulaType.POWER -> {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = mathObj.primaryArg.ifBlank { "x" },
                    color = inkColor,
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    text = mathObj.secondaryArg.ifBlank { "n" },
                    color = inkColor,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        MathFormulaType.SUBSCRIPT -> {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = mathObj.primaryArg.ifBlank { "x" },
                    color = inkColor,
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = mathObj.secondaryArg.ifBlank { "i" },
                    color = inkColor,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        MathFormulaType.INTEGRAL, MathFormulaType.SUMMATION -> {
            val symbol = if (mathObj.mathType == MathFormulaType.INTEGRAL) "∫" else "∑"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = mathObj.tertiaryArg.ifBlank { "b" },
                        color = inkColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic
                    )
                    Text(
                        text = symbol,
                        color = inkColor,
                        fontSize = 30.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = mathObj.secondaryArg.ifBlank { "a" },
                        color = inkColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic
                    )
                }
                Text(
                    text = mathObj.primaryArg.ifBlank { "f(x) dx" },
                    color = inkColor,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        MathFormulaType.EQUATION -> {
            Text(
                text = mathObj.primaryArg.ifBlank { "x = y" },
                color = inkColor,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun InlineMathNodeEditorCard(
    mathObj: CanvasObject,
    strings: StudioStrings,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var primary by remember(mathObj.id) { mutableStateOf(mathObj.primaryArg) }
    var secondary by remember(mathObj.id) { mutableStateOf(mathObj.secondaryArg) }
    var tertiary by remember(mathObj.id) { mutableStateOf(mathObj.tertiaryArg) }

    Surface(
        modifier = modifier
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .border(1.5.dp, ElectricIndigo, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = PureWhite,
        tonalElevation = 12.dp,
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Functions,
                        contentDescription = null,
                        tint = ElectricIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.mathStudioTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = strings.cancelBtn, tint = StudioSlate700)
                }
            }

            OutlinedTextField(
                value = primary,
                onValueChange = { primary = it },
                label = {
                    Text(
                        when (mathObj.mathType) {
                            MathFormulaType.FRACTION -> strings.numerator
                            MathFormulaType.SQUARE_ROOT, MathFormulaType.CUBE_ROOT -> strings.radicand
                            MathFormulaType.POWER, MathFormulaType.SUBSCRIPT -> strings.baseText
                            else -> strings.expression
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (mathObj.mathType == MathFormulaType.FRACTION ||
                mathObj.mathType == MathFormulaType.POWER ||
                mathObj.mathType == MathFormulaType.SUBSCRIPT ||
                mathObj.mathType == MathFormulaType.INTEGRAL ||
                mathObj.mathType == MathFormulaType.SUMMATION
            ) {
                OutlinedTextField(
                    value = secondary,
                    onValueChange = { secondary = it },
                    label = {
                        Text(
                            when (mathObj.mathType) {
                                MathFormulaType.FRACTION -> strings.denominator
                                MathFormulaType.INTEGRAL, MathFormulaType.SUMMATION -> strings.lowerBound
                                else -> strings.exponentOrSub
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (mathObj.mathType == MathFormulaType.INTEGRAL ||
                mathObj.mathType == MathFormulaType.SUMMATION
            ) {
                OutlinedTextField(
                    value = tertiary,
                    onValueChange = { tertiary = it },
                    label = { Text(strings.upperBound) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = { onSave(primary, secondary, tertiary) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.insertToCanvas)
            }
        }
    }
}
