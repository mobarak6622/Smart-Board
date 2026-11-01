package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.WhiteboardProjectEntity
import com.example.localization.StudioStrings
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioSlate200
import com.example.ui.theme.StudioSlate700
import com.example.ui.theme.StudioSlate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDrawerSheet(
    projects: List<WhiteboardProjectEntity>,
    activeProjectId: String,
    strings: StudioStrings,
    onSelectProject: (WhiteboardProjectEntity) -> Unit,
    onCreateProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newTitleInput by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Custom Studio Hero Banner Illustration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(StudioSlate900, ElectricIndigo)
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(104.dp)) {
                    // Decorative coordinate grid & creative mathematical wave
                    val step = 36f
                    var x = step
                    while (x < size.width) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.5f
                        )
                        x += step
                    }
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.22f),
                        radius = 64f,
                        center = Offset(size.width - 70f, 52f)
                    )
                    drawLine(
                        color = Color(0xFFF59E0B),
                        start = Offset(size.width - 130f, 82f),
                        end = Offset(size.width - 24f, 24f),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.appTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = PureWhite
                        )
                        Text(
                            text = strings.projectsDrawerTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.cancelBtn,
                            tint = PureWhite
                        )
                    }
                }
            }

            // Create New Whiteboard Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = newTitleInput,
                    onValueChange = { newTitleInput = it },
                    placeholder = { Text(strings.projectNameHint) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_project_title_input")
                )
                Button(
                    onClick = {
                        val title = newTitleInput.ifBlank {
                            "${strings.untitledBoard} ${projects.size + 1}"
                        }
                        onCreateProject(title)
                        newTitleInput = ""
                        onDismiss()
                    },
                    modifier = Modifier.testTag("create_project_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.createBtn)
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                items(projects, key = { it.id }) { proj ->
                    val isSelected = proj.id == activeProjectId
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) ElectricIndigo else StudioSlate200,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                onSelectProject(proj)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (isSelected) ElectricIndigo else StudioSlate700
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = proj.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${proj.pageCount} ${strings.pageLabel} • ${dateFormat.format(Date(proj.updatedAt))}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StudioSlate700
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteProject(proj.id) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = strings.deletePage,
                                    tint = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextInsertDialog(
    strings: StudioStrings,
    onConfirmText: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.insertTextTitle) },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                placeholder = { Text(strings.enterTextHint) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insert_text_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmText(textValue)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_insert_text_button")
            ) {
                Text(strings.addTextBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelBtn)
            }
        }
    )
}
