package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.AppLanguage
import com.example.localization.StudioStrings
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioSlate700
import com.example.ui.theme.StudioSlate900

@Composable
fun TopStudioBar(
    projectTitle: String,
    currentPageIndex: Int,
    totalPages: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    isSaving: Boolean,
    isFocusMode: Boolean,
    language: AppLanguage,
    strings: StudioStrings,
    activeRoomCode: String? = null,
    isClassroomConnected: Boolean = false,
    onOpenProjectDrawer: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClearCanvas: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenPageManager: () -> Unit,
    onOpenLiveClassroom: () -> Unit = {},
    onOpenStudioSettings: () -> Unit = {},
    onToggleLanguage: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onExportPng: (Boolean) -> Unit,
    onExportPdf: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showExportMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        AnimatedContent(
            targetState = isFocusMode,
            transitionSpec = {
                (slideInVertically(animationSpec = spring()) { -it } + fadeIn())
                    .togetherWith(slideOutVertically(animationSpec = spring()) { -it } + fadeOut())
            },
            label = "TopStudioBarFocusTransition",
            modifier = Modifier.align(Alignment.TopEnd)
        ) { focusActive ->
            if (focusActive) {
                // Ultra-Compact Floating Pill in Focus Mode (Maximizes Canvas Area to 94%+)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = StudioSlate900.copy(alpha = 0.92f),
                    tonalElevation = 8.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = Color(0xFF334155),
                        shape = RoundedCornerShape(22.dp)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = onUndo,
                            enabled = canUndo,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("undo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (canUndo) PureWhite else StudioSlate700,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onRedo,
                            enabled = canRedo,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("redo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo",
                                tint = if (canRedo) PureWhite else StudioSlate700,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .clickable { onOpenPageManager() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("page_manager_button")
                        ) {
                            Text(
                                text = "${currentPageIndex + 1}/$totalPages",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }

                        IconButton(
                            onClick = onToggleFocusMode,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("focus_mode_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = strings.exitFocusMode,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                // Standard Sleek Tablet Studio Capsule Bar
                Surface(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color(0xFF334155),
                            shape = RoundedCornerShape(22.dp)
                        ),
                    shape = RoundedCornerShape(22.dp),
                    color = StudioSlate900.copy(alpha = 0.94f),
                    tonalElevation = 8.dp,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Project Drawer + Title + Auto-save Status Dot
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onOpenProjectDrawer,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("project_drawer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = strings.projectsDrawerTitle,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .widthIn(max = 128.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onOpenProjectDrawer() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = projectTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PureWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isSaving) strings.saving else strings.autoSaved,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        // Center: Undo, Redo, Clear Canvas
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = onUndo,
                                enabled = canUndo,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("undo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo",
                                    tint = if (canUndo) PureWhite else StudioSlate700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = onRedo,
                                enabled = canRedo,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("redo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Redo,
                                    contentDescription = "Redo",
                                    tint = if (canRedo) PureWhite else StudioSlate700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = onClearCanvas,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("clear_canvas_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = strings.clearCanvas,
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Right: Page Stepper + Language Toggle + Export + Focus Mode Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                            ) {
                                IconButton(
                                    onClick = onPrevPage,
                                    enabled = currentPageIndex > 0,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("prev_page_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                        contentDescription = "Previous Page",
                                        tint = if (currentPageIndex > 0) PureWhite else StudioSlate700,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onOpenPageManager() }
                                        .padding(horizontal = 5.dp, vertical = 4.dp)
                                        .testTag("page_manager_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = strings.pageManagerTitle,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${currentPageIndex + 1}/$totalPages",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                }

                                IconButton(
                                    onClick = onNextPage,
                                    enabled = currentPageIndex < totalPages - 1,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("next_page_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                        contentDescription = "Next Page",
                                        tint = if (currentPageIndex < totalPages - 1) PureWhite else StudioSlate700,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            // Live Online Classroom Button (with real-time status badge)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (activeRoomCode != null) Color(0xFF065F46) else Color(0xFF1E293B))
                                    .clickable { onOpenLiveClassroom() }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("live_classroom_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "Live Classroom",
                                    tint = if (activeRoomCode != null) EmeraldSuccess else Color(0xFF38BDF8),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activeRoomCode ?: (if (language == AppLanguage.BENGALI) "ক্লাস" else "Live"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                                if (activeRoomCode != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isClassroomConnected) EmeraldSuccess else Color(0xFFF59E0B))
                                    )
                                }
                            }

                            // Bengali / English Language Pill
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(ElectricIndigo)
                                    .clickable { onToggleLanguage() }
                                    .padding(horizontal = 9.dp, vertical = 6.dp)
                                    .testTag("language_toggle_button")
                            ) {
                                Text(
                                    text = language.badge,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            }

                            // Customizable Toolbar & Gesture Settings Button
                            IconButton(
                                onClick = onOpenStudioSettings,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("studio_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Toolbar & Gesture Settings",
                                    tint = PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Export & Share Dropdown
                            Box {
                                IconButton(
                                    onClick = { showExportMenu = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("export_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.IosShare,
                                        contentDescription = "Export and Share",
                                        tint = PureWhite,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showExportMenu,
                                    onDismissRequest = { showExportMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(strings.exportPng) },
                                        leadingIcon = {
                                            Icon(Icons.Default.SaveAlt, contentDescription = null)
                                        },
                                        onClick = {
                                            showExportMenu = false
                                            onExportPng(false)
                                        },
                                        modifier = Modifier.testTag("export_png_item")
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.sharePng) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Share, contentDescription = null)
                                        },
                                        onClick = {
                                            showExportMenu = false
                                            onExportPng(true)
                                        },
                                        modifier = Modifier.testTag("share_png_item")
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text(strings.exportPdf) },
                                        leadingIcon = {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                        },
                                        onClick = {
                                            showExportMenu = false
                                            onExportPdf(false)
                                        },
                                        modifier = Modifier.testTag("export_pdf_item")
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.sharePdf) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Share, contentDescription = null)
                                        },
                                        onClick = {
                                            showExportMenu = false
                                            onExportPdf(true)
                                        },
                                        modifier = Modifier.testTag("share_pdf_item")
                                    )
                                }
                            }

                            // Focus Mode Collapse Button
                            IconButton(
                                onClick = onToggleFocusMode,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("focus_mode_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = strings.focusMode,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
