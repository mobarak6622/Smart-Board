package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.CanvasRenderEngine
import com.example.localization.AppLanguage
import com.example.localization.StudioStrings
import com.example.model.CanvasObject
import com.example.model.CanvasObjectType
import com.example.model.MathCatalog
import com.example.model.MathFormulaType
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioSlate200
import com.example.ui.theme.StudioSlate700

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MathStudioSheet(
    language: AppLanguage,
    strings: StudioStrings,
    selectedColorArgb: Long,
    onInsertMathFormula: (MathFormulaType, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedMathType by remember { mutableStateOf(MathFormulaType.FRACTION) }

    var primaryArg by remember { mutableStateOf("x² + 1") }
    var secondaryArg by remember { mutableStateOf("2x") }
    var tertiaryArg by remember { mutableStateOf("n") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.mathStudioTitle,
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.cancelBtn)
                }
            }

            // Live Vector Formula Preview Card
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, StudioSlate200, RoundedCornerShape(16.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    val previewObj = CanvasObject(
                        type = CanvasObjectType.MATH_FORMULA,
                        x = size.width / 2f,
                        y = size.height / 2f,
                        scale = 1.05f,
                        colorArgb = selectedColorArgb,
                        mathType = selectedMathType,
                        primaryArg = primaryArg,
                        secondaryArg = secondaryArg,
                        tertiaryArg = tertiaryArg
                    )
                    CanvasRenderEngine.drawCanvasObject(
                        canvas = drawContext.canvas.nativeCanvas,
                        obj = previewObj,
                        isSelected = false
                    )
                }
            }

            // 4 Math Tabs
            val tabs = listOf(
                strings.mathTabTemplates,
                strings.mathTabFormulas,
                strings.mathTabGreek,
                strings.mathTabSymbols
            )
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = PureWhite
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("math_tab_$idx")
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // Structured Templates
                    val templates = listOf(
                        MathFormulaType.FRACTION to strings.fraction,
                        MathFormulaType.SQUARE_ROOT to strings.squareRoot,
                        MathFormulaType.CUBE_ROOT to strings.cubeRoot,
                        MathFormulaType.POWER to strings.power,
                        MathFormulaType.SUBSCRIPT to strings.subscript,
                        MathFormulaType.INTEGRAL to strings.integral,
                        MathFormulaType.SUMMATION to strings.summation,
                        MathFormulaType.EQUATION to strings.equation
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        templates.forEach { (type, label) ->
                            FilterChip(
                                selected = selectedMathType == type,
                                onClick = {
                                    selectedMathType = type
                                    when (type) {
                                        MathFormulaType.FRACTION -> {
                                            primaryArg = "a + b"
                                            secondaryArg = "c"
                                        }
                                        MathFormulaType.SQUARE_ROOT, MathFormulaType.CUBE_ROOT -> {
                                            primaryArg = "x² + y²"
                                        }
                                        MathFormulaType.POWER -> {
                                            primaryArg = "(x + y)"
                                            secondaryArg = "n"
                                        }
                                        MathFormulaType.SUBSCRIPT -> {
                                            primaryArg = "a"
                                            secondaryArg = "n+1"
                                        }
                                        MathFormulaType.INTEGRAL -> {
                                            primaryArg = "x² dx"
                                            secondaryArg = "0"
                                            tertiaryArg = "1"
                                        }
                                        MathFormulaType.SUMMATION -> {
                                            primaryArg = "i²"
                                            secondaryArg = "i=1"
                                            tertiaryArg = "n"
                                        }
                                        MathFormulaType.EQUATION -> {
                                            primaryArg = "f(x) = ax² + bx + c"
                                        }
                                    }
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricIndigo,
                                    selectedLabelColor = PureWhite
                                ),
                                modifier = Modifier.testTag("math_template_${type.name.lowercase()}")
                            )
                        }
                    }

                    // Dynamic Input Fields for Selected Template
                    when (selectedMathType) {
                        MathFormulaType.FRACTION -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = primaryArg,
                                    onValueChange = { primaryArg = it },
                                    label = { Text(strings.numerator) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = secondaryArg,
                                    onValueChange = { secondaryArg = it },
                                    label = { Text(strings.denominator) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        MathFormulaType.SQUARE_ROOT, MathFormulaType.CUBE_ROOT -> {
                            OutlinedTextField(
                                value = primaryArg,
                                onValueChange = { primaryArg = it },
                                label = { Text(strings.radicand) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        MathFormulaType.POWER, MathFormulaType.SUBSCRIPT -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = primaryArg,
                                    onValueChange = { primaryArg = it },
                                    label = { Text(strings.baseText) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = secondaryArg,
                                    onValueChange = { secondaryArg = it },
                                    label = { Text(strings.exponentOrSub) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        MathFormulaType.INTEGRAL, MathFormulaType.SUMMATION -> {
                            OutlinedTextField(
                                value = primaryArg,
                                onValueChange = { primaryArg = it },
                                label = { Text(strings.expression) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = secondaryArg,
                                    onValueChange = { secondaryArg = it },
                                    label = { Text(strings.lowerBound) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = tertiaryArg,
                                    onValueChange = { tertiaryArg = it },
                                    label = { Text(strings.upperBound) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        MathFormulaType.EQUATION -> {
                            OutlinedTextField(
                                value = primaryArg,
                                onValueChange = { primaryArg = it },
                                label = { Text(strings.expression) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                1 -> {
                    // Algebraic, Geometry, Trig & Calculus Presets
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MathCatalog.formulaPresets.forEachIndexed { idx, preset ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, StudioSlate200, RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedMathType = preset.mathType
                                        primaryArg = preset.primaryArg
                                        secondaryArg = preset.secondaryArg
                                        tertiaryArg = preset.tertiaryArg
                                    }
                                    .testTag("formula_preset_$idx")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (language == AppLanguage.BENGALI) preset.titleBn else preset.titleEn,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = if (language == AppLanguage.BENGALI) preset.categoryBn else preset.categoryEn,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = StudioSlate700
                                        )
                                    }
                                    Text(
                                        text = if (preset.secondaryArg.isNotEmpty())
                                            "${preset.primaryArg} / ${preset.secondaryArg}"
                                        else preset.primaryArg,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ElectricIndigo,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Greek Symbols Grid
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MathCatalog.greekSymbols.forEach { (sym, name) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .clickable {
                                        primaryArg += sym
                                    }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(sym, style = MaterialTheme.typography.titleLarge, color = ElectricIndigo)
                                    Text(name, style = MaterialTheme.typography.labelSmall, color = StudioSlate700)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Mathematical Operators & Relations Grid
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MathCatalog.mathOperators.forEach { (sym, name) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .clickable {
                                        primaryArg += " $sym "
                                    }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(sym, style = MaterialTheme.typography.titleLarge, color = ElectricIndigo)
                                    Text(name, style = MaterialTheme.typography.labelSmall, color = StudioSlate700)
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onInsertMathFormula(selectedMathType, primaryArg, secondaryArg, tertiaryArg)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("insert_math_formula_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.insertToCanvas)
            }
        }
    }
}
