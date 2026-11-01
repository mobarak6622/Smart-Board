package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ClassroomRepository
import com.example.data.SmartBoardDatabase
import com.example.data.WhiteboardRepository
import com.example.localization.LocalizationProvider
import com.example.ui.components.DockPanelType
import com.example.ui.components.FloatingBottomDock
import com.example.ui.components.InteractiveWhiteboardCanvas
import com.example.ui.components.LiveClassroomSheet
import com.example.ui.components.MathStudioSheet
import com.example.ui.components.PageManagerSheet
import com.example.ui.components.ProjectDrawerSheet
import com.example.ui.components.StudioSettingsDialog
import com.example.ui.components.TextInsertDialog
import com.example.ui.components.TopStudioBar
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SmartBoardProTheme
import com.example.ui.theme.StudioSlate900
import com.example.viewmodel.WhiteboardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = SmartBoardDatabase.getDatabase(applicationContext)
        val repository = WhiteboardRepository(database.projectDao(), applicationContext)
        val classroomRepository = ClassroomRepository(applicationContext)

        setContent {
            SmartBoardProTheme {
                val whiteboardViewModel: WhiteboardViewModel = viewModel(
                    factory = WhiteboardViewModel.Factory(repository, classroomRepository)
                )
                SmartBoardProApp(viewModel = whiteboardViewModel)
            }
        }
    }
}

@Composable
fun SmartBoardProApp(
    viewModel: WhiteboardViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val strings = remember(uiState.language) {
        LocalizationProvider.forLanguage(uiState.language)
    }

    var activeDockPanel by remember { mutableStateOf(DockPanelType.NONE) }
    var showQuickColorSizeStrip by remember { mutableStateOf(true) }
    var isFocusMode by remember { mutableStateOf(false) }
    var showMathSheet by remember { mutableStateOf(false) }
    var showPageManagerSheet by remember { mutableStateOf(false) }
    var showProjectDrawerSheet by remember { mutableStateOf(false) }
    var showTextInsertDialog by remember { mutableStateOf(false) }
    var showStudioSettingsDialog by remember { mutableStateOf(false) }
    var showLiveClassroomSheet by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker for Image Insertion
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.addImageFromUri(uri)
            activeDockPanel = DockPanelType.NONE
        }
    }

    // Back handler to dismiss any open popup panel or exit Focus Mode before exiting the app
    BackHandler(enabled = activeDockPanel != DockPanelType.NONE || isFocusMode) {
        if (activeDockPanel != DockPanelType.NONE) {
            activeDockPanel = DockPanelType.NONE
        } else if (isFocusMode) {
            isFocusMode = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full-screen Native Android Canvas Engine + Interactive On-Canvas Compose Math Overlay
        InteractiveWhiteboardCanvas(
            uiState = uiState,
            strings = strings,
            onCommitStroke = viewModel::commitStroke,
            onEraseStrokeAt = viewModel::eraseStrokeAt,
            onSelectObject = viewModel::selectObject,
            onMoveSelectedObject = viewModel::moveSelectedObject,
            onMoveObjectById = viewModel::moveObjectById,
            onScaleSelectedObject = viewModel::scaleSelectedObject,
            onScaleObjectById = viewModel::scaleObjectById,
            onUpdateMathObject = viewModel::updateMathObject,
            onFlattenMathObjectToCanvas = viewModel::flattenMathObjectToCanvas,
            onDeleteSelectedObject = viewModel::deleteSelectedObject,
            onDeleteObjectById = viewModel::deleteObjectById,
            onZoomPanChange = viewModel::updateZoomPan,
            onResetZoomPan = viewModel::resetZoomPan,
            onRulerMove = viewModel::updateRulerMove,
            onRulerRotate = viewModel::updateRulerAngleToward,
            onProtractorMove = viewModel::updateProtractorMove,
            onProtractorGuide = viewModel::updateProtractorGuideAngle,
            onCompassPivotMove = viewModel::updateCompassPivot,
            onCompassRadiusAdjust = viewModel::updateCompassRadius,
            onStampCompassArc = viewModel::stampCompassCircle,
            onCanvasTouchStarted = {
                if (activeDockPanel != DockPanelType.NONE) {
                    activeDockPanel = DockPanelType.NONE
                }
            }
        )

        // 2. Top Floating Studio Bar (Supports Focus Mode Collapse, Live Classroom & Settings)
        TopStudioBar(
            projectTitle = uiState.projectTitle,
            currentPageIndex = uiState.currentPageIndex,
            totalPages = uiState.pages.size,
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            isSaving = uiState.isSaving,
            isFocusMode = isFocusMode,
            language = uiState.language,
            strings = strings,
            activeRoomCode = uiState.activeClassroom?.roomCode,
            isClassroomConnected = uiState.isClassroomConnected,
            onOpenProjectDrawer = { showProjectDrawerSheet = true },
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onClearCanvas = { viewModel.clearCurrentPage(strings.clearConfirmMessage) },
            onPrevPage = viewModel::goToPreviousPage,
            onNextPage = viewModel::goToNextPage,
            onOpenPageManager = { showPageManagerSheet = true },
            onOpenLiveClassroom = {
                viewModel.refreshAuthUser()
                showLiveClassroomSheet = true
            },
            onOpenStudioSettings = { showStudioSettingsDialog = true },
            onToggleLanguage = viewModel::toggleLanguage,
            onToggleFocusMode = {
                isFocusMode = !isFocusMode
                if (isFocusMode) {
                    showQuickColorSizeStrip = false
                    activeDockPanel = DockPanelType.NONE
                }
            },
            onExportPng = { share ->
                viewModel.exportCurrentPagePng(context, share, strings.exportSuccessPng)
            },
            onExportPdf = { share ->
                viewModel.exportAllPagesPdf(context, share, strings.exportSuccessPdf)
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. Status Notification Pill (Auto-save / Export / Clear / Classroom notifications)
        AnimatedVisibility(
            visible = uiState.statusBannerMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 74.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = StudioSlate900.copy(alpha = 0.92f),
                tonalElevation = 8.dp
            ) {
                Text(
                    text = uiState.statusBannerMessage.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = PureWhite,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // 4. Dual-Mode Floating Bottom Tool Dock & Contextual Popup Panels
        FloatingBottomDock(
            uiState = uiState,
            strings = strings,
            activePanel = activeDockPanel,
            showQuickStrip = showQuickColorSizeStrip,
            onToggleQuickStrip = {
                showQuickColorSizeStrip = !showQuickColorSizeStrip
                if (showQuickColorSizeStrip) {
                    activeDockPanel = DockPanelType.NONE
                }
            },
            onTogglePanel = { panel ->
                activeDockPanel = if (activeDockPanel == panel) DockPanelType.NONE else panel
            },
            onDismissPanel = { activeDockPanel = DockPanelType.NONE },
            onSelectTool = viewModel::selectTool,
            onSelectColor = viewModel::selectColor,
            onStrokeWidthChange = viewModel::setStrokeWidth,
            onStrokeAlphaChange = viewModel::setStrokeAlpha,
            onPolygonSidesChange = viewModel::setPolygonSides,
            onToggleRuler = viewModel::toggleRuler,
            onToggleProtractor = viewModel::toggleProtractor,
            onToggleCompass = viewModel::toggleCompass,
            onSelectBackground = viewModel::setPageBackground,
            onOpenMathStudio = {
                activeDockPanel = DockPanelType.NONE
                showMathSheet = true
            },
            onOpenTextDialog = {
                activeDockPanel = DockPanelType.NONE
                showTextInsertDialog = true
            },
            onPickGalleryImage = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onClearCanvas = {
                viewModel.clearCurrentPage(strings.clearConfirmMessage)
                activeDockPanel = DockPanelType.NONE
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 5. Mathematics & Formula Studio Sheet
        if (showMathSheet) {
            MathStudioSheet(
                language = uiState.language,
                strings = strings,
                selectedColorArgb = uiState.selectedColorArgb,
                onInsertMathFormula = { type, primary, secondary, tertiary ->
                    viewModel.addMathFormulaObject(type, primary, secondary, tertiary)
                },
                onDismiss = { showMathSheet = false }
            )
        }

        // 6. Multi-Page Manager Sheet
        if (showPageManagerSheet) {
            PageManagerSheet(
                pages = uiState.pages,
                currentPageIndex = uiState.currentPageIndex,
                strings = strings,
                onSelectPage = viewModel::selectPage,
                onAddPage = viewModel::addNewPage,
                onDuplicatePage = viewModel::duplicatePage,
                onDeletePage = viewModel::deletePage,
                onExportAllPdf = {
                    viewModel.exportAllPagesPdf(context, true, strings.exportSuccessPdf)
                },
                onDismiss = { showPageManagerSheet = false }
            )
        }

        // 7. Saved Whiteboards Project Drawer Sheet
        if (showProjectDrawerSheet) {
            ProjectDrawerSheet(
                projects = savedProjects,
                activeProjectId = uiState.projectId,
                strings = strings,
                onSelectProject = viewModel::selectProject,
                onCreateProject = viewModel::createNewProject,
                onDeleteProject = viewModel::deleteProject,
                onDismiss = { showProjectDrawerSheet = false }
            )
        }

        // 8. Bilingual Text Insertion Dialog
        if (showTextInsertDialog) {
            TextInsertDialog(
                strings = strings,
                onConfirmText = { text ->
                    viewModel.addTextObject(text)
                },
                onDismiss = { showTextInsertDialog = false }
            )
        }

        // 9. Customizable Studio Toolbar & Gesture Settings Dialog
        if (showStudioSettingsDialog) {
            StudioSettingsDialog(
                preferences = uiState.studioPreferences,
                language = uiState.language,
                strings = strings,
                onUpdatePreferences = viewModel::updateStudioPreferences,
                onDismiss = { showStudioSettingsDialog = false }
            )
        }

        // 10. Real-Time Online Classroom Sheet (Google Sign-In + Firestore Live Sync)
        if (showLiveClassroomSheet) {
            LiveClassroomSheet(
                language = uiState.language,
                strings = strings,
                currentUserUid = uiState.currentUserUid,
                currentUserName = uiState.currentUserName,
                currentUserEmail = uiState.currentUserEmail,
                activeClassroom = uiState.activeClassroom,
                classroomMembers = uiState.classroomMembers,
                isClassroomConnected = uiState.isClassroomConnected,
                classroomError = uiState.classroomError,
                onAuthSuccess = viewModel::refreshAuthUser,
                onSignOutSuccess = viewModel::onUserSignedOut,
                onCreateClassroom = viewModel::createLiveClassroom,
                onJoinClassroom = viewModel::joinLiveClassroom,
                onToggleGlobalStudentDrawing = viewModel::toggleGlobalStudentDrawing,
                onToggleStudentPermission = viewModel::toggleStudentPermission,
                onLeaveOrEndClassroom = viewModel::leaveOrEndClassroom,
                onDismiss = { showLiveClassroomSheet = false }
            )
        }
    }
}
