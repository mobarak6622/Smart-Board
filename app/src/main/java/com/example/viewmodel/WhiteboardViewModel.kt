package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ClassroomRepository
import com.example.data.WhiteboardProjectEntity
import com.example.data.WhiteboardRepository
import com.example.engine.CanvasRenderEngine
import com.example.engine.ExportShareHelper
import com.example.localization.AppLanguage
import com.example.model.BackgroundType
import com.example.model.BoardPage
import com.example.model.CanvasObject
import com.example.model.CanvasObjectType
import com.example.model.CanvasStroke
import com.example.model.ClassroomMember
import com.example.model.ClassroomSession
import com.example.model.CompassState
import com.example.model.DrawingToolType
import com.example.model.MathFormulaType
import com.example.model.ProtractorState
import com.example.model.RulerState
import com.example.model.StudioPreferences
import com.example.ui.theme.DrawingPaletteColors
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.hypot

data class WhiteboardUiState(
    val projectId: String = UUID.randomUUID().toString(),
    val projectTitle: String = "SmartBoard Lesson 1",
    val pages: List<BoardPage> = listOf(BoardPage()),
    val currentPageIndex: Int = 0,
    val activeTool: DrawingToolType = DrawingToolType.PEN,
    val selectedColorArgb: Long = DrawingPaletteColors.first(),
    val strokeWidth: Float = 8f,
    val strokeAlpha: Float = 1f,
    val polygonSides: Int = 5,
    val zoomScale: Float = 1f,
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f,
    val studioPreferences: StudioPreferences = StudioPreferences(),
    val ruler: RulerState = RulerState(),
    val protractor: ProtractorState = ProtractorState(),
    val compass: CompassState = CompassState(),
    val selectedObjectId: String? = null,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val isSaving: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val statusBannerMessage: String? = null,
    val currentUserUid: String? = null,
    val currentUserName: String? = null,
    val currentUserEmail: String? = null,
    val activeClassroom: ClassroomSession? = null,
    val classroomMembers: List<ClassroomMember> = emptyList(),
    val isClassroomConnected: Boolean = false,
    val classroomError: String? = null
) {
    val currentPage: BoardPage
        get() = pages.getOrElse(currentPageIndex) { BoardPage() }

    val canCurrentUserDraw: Boolean
        get() {
            val session = activeClassroom ?: return true
            val uid = currentUserUid ?: return false
            if (session.teacherUid == uid) return true
            if (session.allowStudentDrawing) return true
            return classroomMembers.any { it.userId == uid && it.canDraw }
        }
}

class WhiteboardViewModel(
    private val repository: WhiteboardRepository,
    private val classroomRepository: ClassroomRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WhiteboardUiState(
            studioPreferences = runCatching { repository.loadStudioPreferences() }
                .getOrDefault(StudioPreferences())
        )
    )
    val uiState: StateFlow<WhiteboardUiState> = _uiState.asStateFlow()

    val savedProjects: StateFlow<List<WhiteboardProjectEntity>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Per-page Undo/Redo history stacks
    private val undoStacks = mutableMapOf<String, ArrayDeque<BoardPage>>()
    private val redoStacks = mutableMapOf<String, ArrayDeque<BoardPage>>()

    private var saveJob: Job? = null
    private var bannerJob: Job? = null
    private var classroomSessionJob: Job? = null
    private var classroomMembersJob: Job? = null
    private var classroomSyncJob: Job? = null
    private var lastSyncedPagesJson: String = ""

    init {
        refreshAuthUser()
        viewModelScope.launch {
            repository.allProjects.collect { list ->
                if (list.isNotEmpty() && _uiState.value.pages.size == 1 &&
                    _uiState.value.currentPage.strokes.isEmpty() &&
                    _uiState.value.currentPage.objects.isEmpty() &&
                    _uiState.value.projectTitle == "SmartBoard Lesson 1"
                ) {
                    val latest = list.first()
                    if (latest.id != _uiState.value.projectId) {
                        loadProjectEntity(latest)
                    }
                } else if (list.isEmpty()) {
                    triggerAutoSave(immediate = true)
                }
            }
        }
    }

    private fun loadProjectEntity(entity: WhiteboardProjectEntity) {
        val loadedPages = WhiteboardRepository.deserializePages(entity.pagesJson)
        undoStacks.clear()
        redoStacks.clear()
        _uiState.update {
            it.copy(
                projectId = entity.id,
                projectTitle = entity.title,
                pages = loadedPages,
                currentPageIndex = 0,
                selectedObjectId = null,
                canUndo = false,
                canRedo = false,
                zoomScale = 1f,
                panOffsetX = 0f,
                panOffsetY = 0f
            )
        }
    }

    fun selectProject(entity: WhiteboardProjectEntity) {
        loadProjectEntity(entity)
    }

    fun createNewProject(title: String) {
        val cleanTitle = title.trim().ifBlank { "Untitled Board" }
        val newId = UUID.randomUUID().toString()
        val firstPage = BoardPage()
        undoStacks.clear()
        redoStacks.clear()
        _uiState.update {
            it.copy(
                projectId = newId,
                projectTitle = cleanTitle,
                pages = listOf(firstPage),
                currentPageIndex = 0,
                selectedObjectId = null,
                canUndo = false,
                canRedo = false,
                zoomScale = 1f,
                panOffsetX = 0f,
                panOffsetY = 0f
            )
        }
        triggerAutoSave(immediate = true)
    }

    fun renameCurrentProject(newTitle: String) {
        val clean = newTitle.trim().ifBlank { return }
        _uiState.update { it.copy(projectTitle = clean) }
        triggerAutoSave(immediate = true)
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            if (_uiState.value.projectId == projectId) {
                createNewProject("SmartBoard Lesson")
            }
        }
    }

    fun toggleLanguage() {
        _uiState.update {
            val next = if (it.language == AppLanguage.ENGLISH) AppLanguage.BENGALI else AppLanguage.ENGLISH
            it.copy(language = next)
        }
    }

    fun updateStudioPreferences(prefs: StudioPreferences) {
        _uiState.update { it.copy(studioPreferences = prefs) }
        runCatching { repository.saveStudioPreferences(prefs) }
    }

    fun updateZoomPan(scale: Float, panX: Float, panY: Float) {
        _uiState.update {
            it.copy(
                zoomScale = scale.coerceIn(0.5f, 4.0f),
                panOffsetX = panX,
                panOffsetY = panY
            )
        }
    }

    fun resetZoomPan() {
        _uiState.update {
            it.copy(
                zoomScale = 1f,
                panOffsetX = 0f,
                panOffsetY = 0f
            )
        }
    }

    fun selectTool(tool: DrawingToolType) {
        _uiState.update {
            it.copy(
                activeTool = tool,
                selectedObjectId = if (tool == DrawingToolType.SELECT_MOVE) it.selectedObjectId else null
            )
        }
    }

    fun selectColor(colorArgb: Long) {
        _uiState.update { state ->
            val updatedPages = if (state.selectedObjectId != null) {
                state.pages.mapIndexed { idx, p ->
                    if (idx != state.currentPageIndex) p
                    else p.copy(
                        objects = p.objects.map { obj ->
                            if (obj.id == state.selectedObjectId) obj.copy(colorArgb = colorArgb) else obj
                        }
                    )
                }
            } else {
                state.pages
            }
            state.copy(
                selectedColorArgb = colorArgb,
                pages = updatedPages,
                activeTool = if (state.activeTool == DrawingToolType.PIXEL_ERASER ||
                    state.activeTool == DrawingToolType.STROKE_ERASER ||
                    state.activeTool == DrawingToolType.HAND_PAN
                ) DrawingToolType.PEN else state.activeTool
            )
        }
        if (_uiState.value.selectedObjectId != null) {
            triggerAutoSave()
        }
    }

    fun setStrokeWidth(width: Float) {
        _uiState.update { it.copy(strokeWidth = width.coerceIn(2f, 64f)) }
    }

    fun setStrokeAlpha(alpha: Float) {
        _uiState.update { it.copy(strokeAlpha = alpha.coerceIn(0.1f, 1f)) }
    }

    fun setPolygonSides(sides: Int) {
        _uiState.update { it.copy(polygonSides = sides.coerceIn(5, 8)) }
    }

    fun setPageBackground(backgroundType: BackgroundType) {
        if (!_uiState.value.canCurrentUserDraw) return
        pushUndoSnapshot()
        updateCurrentPage { page ->
            page.copy(backgroundType = backgroundType)
        }
    }

    fun commitStroke(stroke: CanvasStroke) {
        if (stroke.points.isEmpty() || !_uiState.value.canCurrentUserDraw) return
        pushUndoSnapshot()
        updateCurrentPage { page ->
            page.copy(strokes = page.strokes + stroke)
        }
    }

    fun eraseStrokeAt(x: Float, y: Float) {
        if (!_uiState.value.canCurrentUserDraw) return
        val currentPage = _uiState.value.currentPage
        val hitId = CanvasRenderEngine.findHitStrokeId(currentPage, x, y) ?: return
        pushUndoSnapshot()
        updateCurrentPage { page ->
            page.copy(strokes = page.strokes.filterNot { it.id == hitId })
        }
    }

    fun clearCurrentPage(confirmMessage: String) {
        if (!_uiState.value.canCurrentUserDraw) return
        val current = _uiState.value.currentPage
        if (current.strokes.isEmpty() && current.objects.isEmpty()) return
        pushUndoSnapshot()
        updateCurrentPage { page ->
            page.copy(strokes = emptyList(), objects = emptyList())
        }
        showBanner(confirmMessage)
    }

    fun undo() {
        if (!_uiState.value.canCurrentUserDraw) return
        val pageId = _uiState.value.currentPage.id
        val undoStack = undoStacks[pageId] ?: return
        if (undoStack.isEmpty()) return
        val currentSnapshot = deepCopyPage(_uiState.value.currentPage)
        val previousPage = undoStack.removeLast()
        val redoStack = redoStacks.getOrPut(pageId) { ArrayDeque() }
        redoStack.addLast(currentSnapshot)

        replaceCurrentPageWithoutUndo(previousPage)
    }

    fun redo() {
        if (!_uiState.value.canCurrentUserDraw) return
        val pageId = _uiState.value.currentPage.id
        val redoStack = redoStacks[pageId] ?: return
        if (redoStack.isEmpty()) return
        val currentSnapshot = deepCopyPage(_uiState.value.currentPage)
        val nextPage = redoStack.removeLast()
        val undoStack = undoStacks.getOrPut(pageId) { ArrayDeque() }
        undoStack.addLast(currentSnapshot)

        replaceCurrentPageWithoutUndo(nextPage)
    }

    // Geometry Instruments
    fun toggleRuler() {
        _uiState.update { it.copy(ruler = it.ruler.copy(enabled = !it.ruler.enabled)) }
    }

    fun toggleProtractor() {
        _uiState.update { it.copy(protractor = it.protractor.copy(enabled = !it.protractor.enabled)) }
    }

    fun toggleCompass() {
        _uiState.update { it.copy(compass = it.compass.copy(enabled = !it.compass.enabled)) }
    }

    fun updateRulerMove(dx: Float, dy: Float) {
        _uiState.update {
            it.copy(
                ruler = it.ruler.copy(
                    centerX = it.ruler.centerX + dx,
                    centerY = it.ruler.centerY + dy
                )
            )
        }
    }

    fun updateRulerAngleToward(touchX: Float, touchY: Float) {
        _uiState.update { state ->
            val deg = Math.toDegrees(
                atan2(
                    (touchY - state.ruler.centerY).toDouble(),
                    (touchX - state.ruler.centerX).toDouble()
                )
            ).toFloat()
            state.copy(ruler = state.ruler.copy(angleDegrees = deg))
        }
    }

    fun updateProtractorMove(dx: Float, dy: Float) {
        _uiState.update {
            it.copy(
                protractor = it.protractor.copy(
                    centerX = it.protractor.centerX + dx,
                    centerY = it.protractor.centerY + dy
                )
            )
        }
    }

    fun updateProtractorGuideAngle(touchX: Float, touchY: Float) {
        _uiState.update { state ->
            val p = state.protractor
            val rawAngle = Math.toDegrees(
                atan2(
                    -(touchY - p.centerY).toDouble(),
                    (touchX - p.centerX).toDouble()
                )
            ).toFloat()
            val clamped = rawAngle.coerceIn(0f, 180f)
            state.copy(protractor = p.copy(guideAngleDegrees = clamped))
        }
    }

    fun updateCompassPivot(dx: Float, dy: Float) {
        _uiState.update {
            it.copy(
                compass = it.compass.copy(
                    pivotX = it.compass.pivotX + dx,
                    pivotY = it.compass.pivotY + dy
                )
            )
        }
    }

    fun updateCompassRadius(touchX: Float, touchY: Float) {
        _uiState.update { state ->
            val c = state.compass
            val dist = hypot(touchX - c.pivotX, touchY - c.pivotY).coerceIn(60f, 480f)
            state.copy(compass = c.copy(radiusPx = dist))
        }
    }

    fun stampCompassCircle() {
        val state = _uiState.value
        if (!state.compass.enabled || !state.canCurrentUserDraw) return
        val arcStroke = CanvasRenderEngine.generateCompassArcStroke(
            compass = state.compass,
            colorArgb = state.selectedColorArgb,
            strokeWidth = state.strokeWidth
        )
        commitStroke(arcStroke)
    }

    // Text, Math & Image Canvas Objects
    fun addTextObject(text: String) {
        if (!_uiState.value.canCurrentUserDraw) return
        val clean = text.trim().ifBlank { return }
        pushUndoSnapshot()
        val newObj = CanvasObject(
            type = CanvasObjectType.TEXT,
            x = 540f,
            y = 650f,
            colorArgb = _uiState.value.selectedColorArgb,
            text = clean
        )
        updateCurrentPage(selectObjectId = newObj.id) { page ->
            page.copy(objects = page.objects + newObj)
        }
    }

    fun addMathFormulaObject(
        mathType: MathFormulaType,
        primaryArg: String,
        secondaryArg: String = "",
        tertiaryArg: String = ""
    ) {
        if (!_uiState.value.canCurrentUserDraw) return
        pushUndoSnapshot()
        val newObj = CanvasObject(
            type = CanvasObjectType.MATH_FORMULA,
            x = 540f,
            y = 580f,
            colorArgb = _uiState.value.selectedColorArgb,
            mathType = mathType,
            primaryArg = primaryArg,
            secondaryArg = secondaryArg,
            tertiaryArg = tertiaryArg,
            isBakedToCanvas = false
        )
        updateCurrentPage(selectObjectId = newObj.id) { page ->
            page.copy(objects = page.objects + newObj)
        }
    }

    fun updateMathObject(
        objectId: String,
        primaryArg: String,
        secondaryArg: String,
        tertiaryArg: String
    ) {
        if (!_uiState.value.canCurrentUserDraw) return
        pushUndoSnapshot()
        updateCurrentPage(selectObjectId = objectId) { page ->
            page.copy(
                objects = page.objects.map { obj ->
                    if (obj.id == objectId) {
                        obj.copy(
                            primaryArg = primaryArg,
                            secondaryArg = secondaryArg,
                            tertiaryArg = tertiaryArg
                        )
                    } else obj
                }
            )
        }
    }

    fun flattenMathObjectToCanvas(objectId: String) {
        if (!_uiState.value.canCurrentUserDraw) return
        pushUndoSnapshot()
        updateCurrentPage(selectObjectId = null) { page ->
            page.copy(
                objects = page.objects.map { obj ->
                    if (obj.id == objectId) obj.copy(isBakedToCanvas = true) else obj
                }
            )
        }
        val msg = if (_uiState.value.language == AppLanguage.BENGALI)
            "গণিতের সূত্রটি ক্যানভাসে স্ট্যাম্প করা হয়েছে"
        else
            "Formula stamped onto canvas vector layer"
        showBanner(msg)
    }

    fun addImageFromUri(uri: Uri) {
        if (!_uiState.value.canCurrentUserDraw) return
        viewModelScope.launch {
            val savedPath = repository.copyUriToLocalImage(uri) ?: return@launch
            pushUndoSnapshot()
            val newObj = CanvasObject(
                type = CanvasObjectType.IMAGE,
                x = 540f,
                y = 720f,
                imagePath = savedPath
            )
            updateCurrentPage(selectObjectId = newObj.id) { page ->
                page.copy(objects = page.objects + newObj)
            }
        }
    }

    fun selectObject(objectId: String?) {
        _uiState.update { it.copy(selectedObjectId = objectId) }
    }

    fun moveSelectedObject(dx: Float, dy: Float) {
        val selectedId = _uiState.value.selectedObjectId ?: return
        moveObjectById(selectedId, dx, dy)
    }

    fun moveObjectById(objectId: String, dx: Float, dy: Float) {
        if (!_uiState.value.canCurrentUserDraw) return
        _uiState.update { state ->
            val updatedPages = state.pages.mapIndexed { idx, p ->
                if (idx != state.currentPageIndex) p
                else p.copy(
                    objects = p.objects.map { obj ->
                        if (obj.id == objectId) obj.copy(x = obj.x + dx, y = obj.y + dy) else obj
                    }
                )
            }
            state.copy(pages = updatedPages, selectedObjectId = objectId)
        }
        triggerAutoSave()
    }

    fun scaleSelectedObject(factorDelta: Float) {
        val selectedId = _uiState.value.selectedObjectId ?: return
        scaleObjectById(selectedId, factorDelta)
    }

    fun scaleObjectById(objectId: String, factorDelta: Float) {
        if (!_uiState.value.canCurrentUserDraw) return
        pushUndoSnapshot()
        updateCurrentPage(selectObjectId = objectId) { page ->
            page.copy(
                objects = page.objects.map { obj ->
                    if (obj.id == objectId) {
                        obj.copy(scale = (obj.scale + factorDelta).coerceIn(0.4f, 3.2f))
                    } else obj
                }
            )
        }
    }

    fun deleteSelectedObject() {
        val selectedId = _uiState.value.selectedObjectId ?: return
        deleteObjectById(selectedId)
    }

    fun deleteObjectById(objectId: String) {
        if (!_uiState.value.canCurrentUserDraw) return
        pushUndoSnapshot()
        updateCurrentPage(selectObjectId = null) { page ->
            page.copy(objects = page.objects.filterNot { it.id == objectId })
        }
    }

    // Multi-Page Management
    fun goToPreviousPage() {
        _uiState.update { state ->
            val prevIdx = (state.currentPageIndex - 1).coerceAtLeast(0)
            val pageId = state.pages[prevIdx].id
            state.copy(
                currentPageIndex = prevIdx,
                selectedObjectId = null,
                canUndo = undoStacks[pageId]?.isNotEmpty() == true,
                canRedo = redoStacks[pageId]?.isNotEmpty() == true
            )
        }
        syncBoardToActiveClassroom()
    }

    fun goToNextPage() {
        _uiState.update { state ->
            val nextIdx = (state.currentPageIndex + 1).coerceAtMost(state.pages.lastIndex)
            val pageId = state.pages[nextIdx].id
            state.copy(
                currentPageIndex = nextIdx,
                selectedObjectId = null,
                canUndo = undoStacks[pageId]?.isNotEmpty() == true,
                canRedo = redoStacks[pageId]?.isNotEmpty() == true
            )
        }
        syncBoardToActiveClassroom()
    }

    fun selectPage(index: Int) {
        _uiState.update { state ->
            val validIdx = index.coerceIn(0, state.pages.lastIndex)
            val pageId = state.pages[validIdx].id
            state.copy(
                currentPageIndex = validIdx,
                selectedObjectId = null,
                canUndo = undoStacks[pageId]?.isNotEmpty() == true,
                canRedo = redoStacks[pageId]?.isNotEmpty() == true
            )
        }
        syncBoardToActiveClassroom()
    }

    fun addNewPage() {
        if (!_uiState.value.canCurrentUserDraw) return
        _uiState.update { state ->
            val newPage = BoardPage(backgroundType = state.currentPage.backgroundType)
            val newList = state.pages + newPage
            state.copy(
                pages = newList,
                currentPageIndex = newList.lastIndex,
                selectedObjectId = null,
                canUndo = false,
                canRedo = false
            )
        }
        triggerAutoSave(immediate = true)
    }

    fun duplicatePage(index: Int) {
        if (!_uiState.value.canCurrentUserDraw) return
        _uiState.update { state ->
            val source = state.pages.getOrNull(index) ?: return@update state
            val copyPage = source.copy(
                id = UUID.randomUUID().toString(),
                strokes = source.strokes.map { it.copy(id = UUID.randomUUID().toString()) },
                objects = source.objects.map { it.copy(id = UUID.randomUUID().toString()) }
            )
            val mutable = state.pages.toMutableList()
            mutable.add(index + 1, copyPage)
            state.copy(
                pages = mutable,
                currentPageIndex = index + 1,
                selectedObjectId = null,
                canUndo = false,
                canRedo = false
            )
        }
        triggerAutoSave(immediate = true)
    }

    fun deletePage(index: Int) {
        if (!_uiState.value.canCurrentUserDraw) return
        _uiState.update { state ->
            if (state.pages.size <= 1) {
                val cleared = BoardPage(backgroundType = state.currentPage.backgroundType)
                return@update state.copy(pages = listOf(cleared), currentPageIndex = 0)
            }
            val mutable = state.pages.toMutableList()
            val removed = mutable.removeAt(index)
            undoStacks.remove(removed.id)
            redoStacks.remove(removed.id)
            val nextIdx = state.currentPageIndex.coerceAtMost(mutable.lastIndex)
            val pageId = mutable[nextIdx].id
            state.copy(
                pages = mutable,
                currentPageIndex = nextIdx,
                selectedObjectId = null,
                canUndo = undoStacks[pageId]?.isNotEmpty() == true,
                canRedo = redoStacks[pageId]?.isNotEmpty() == true
            )
        }
        triggerAutoSave(immediate = true)
    }

    // Export & Share
    fun exportCurrentPagePng(
        context: Context,
        shareAfterExport: Boolean,
        successMessage: String
    ) {
        val state = _uiState.value
        viewModelScope.launch {
            val file = ExportShareHelper.exportPageToPng(
                context = context,
                page = state.currentPage,
                projectTitle = state.projectTitle,
                pageIndex = state.currentPageIndex
            )
            if (file != null) {
                showBanner("$successMessage (${file.name})")
                if (shareAfterExport) {
                    ExportShareHelper.shareExportedFile(context, file, "image/png")
                }
            }
        }
    }

    fun exportAllPagesPdf(
        context: Context,
        shareAfterExport: Boolean,
        successMessage: String
    ) {
        val state = _uiState.value
        viewModelScope.launch {
            val file = ExportShareHelper.exportAllPagesToPdf(
                context = context,
                pages = state.pages,
                projectTitle = state.projectTitle
            )
            if (file != null) {
                showBanner("$successMessage (${file.name})")
                if (shareAfterExport) {
                    ExportShareHelper.shareExportedFile(context, file, "application/pdf")
                }
            }
        }
    }

    // Real-Time Online Classroom Lifecycle & Sync
    fun refreshAuthUser() {
        val user = runCatching { Firebase.auth.currentUser }.getOrNull()
        _uiState.update {
            it.copy(
                currentUserUid = user?.uid,
                currentUserName = user?.displayName ?: user?.email?.substringBefore("@"),
                currentUserEmail = user?.email,
                classroomError = null
            )
        }
    }

    fun onUserSignedOut() {
        cancelClassroomListeners()
        _uiState.update {
            it.copy(
                currentUserUid = null,
                currentUserName = null,
                currentUserEmail = null,
                activeClassroom = null,
                classroomMembers = emptyList(),
                isClassroomConnected = false,
                classroomError = null
            )
        }
    }

    fun createLiveClassroom(title: String) {
        val repo = classroomRepository ?: return
        refreshAuthUser()
        if (_uiState.value.currentUserUid == null) {
            _uiState.update { it.copy(classroomError = "Please sign in with Google first.") }
            return
        }
        val state = _uiState.value
        val pagesJson = WhiteboardRepository.serializePages(state.pages)
        lastSyncedPagesJson = pagesJson

        viewModelScope.launch {
            val result = repo.createClassroom(
                title = title,
                pagesJson = pagesJson,
                currentPageIndex = state.currentPageIndex
            )
            result.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        activeClassroom = session,
                        isClassroomConnected = true,
                        classroomError = null
                    )
                }
                startObservingClassroom(session.roomCode)
                showBanner("Live Classroom ${session.roomCode} started")
            }.onFailure { err ->
                _uiState.update {
                    it.copy(classroomError = err.localizedMessage ?: "Failed to create classroom")
                }
            }
        }
    }

    fun joinLiveClassroom(roomCode: String) {
        val repo = classroomRepository ?: return
        refreshAuthUser()
        if (_uiState.value.currentUserUid == null) {
            _uiState.update { it.copy(classroomError = "Please sign in with Google first.") }
            return
        }
        val cleanCode = roomCode.trim().uppercase()
        viewModelScope.launch {
            val result = repo.joinClassroom(cleanCode)
            result.onSuccess { session ->
                val remotePages = WhiteboardRepository.deserializePages(session.pagesJson)
                lastSyncedPagesJson = session.pagesJson
                _uiState.update {
                    it.copy(
                        projectTitle = session.title,
                        pages = remotePages,
                        currentPageIndex = session.currentPageIndex.coerceIn(0, remotePages.lastIndex),
                        activeClassroom = session,
                        isClassroomConnected = true,
                        classroomError = null
                    )
                }
                startObservingClassroom(session.roomCode)
                showBanner("Joined Classroom ${session.roomCode}")
            }.onFailure { err ->
                _uiState.update {
                    it.copy(classroomError = err.localizedMessage ?: "Could not join classroom $cleanCode")
                }
            }
        }
    }

    fun toggleGlobalStudentDrawing(allow: Boolean) {
        val repo = classroomRepository ?: return
        val session = _uiState.value.activeClassroom ?: return
        viewModelScope.launch {
            repo.setGlobalStudentDrawing(session.roomCode, allow)
        }
    }

    fun toggleStudentPermission(studentUid: String, canDraw: Boolean) {
        val repo = classroomRepository ?: return
        val session = _uiState.value.activeClassroom ?: return
        viewModelScope.launch {
            repo.setStudentCanDraw(session.roomCode, studentUid, canDraw)
        }
    }

    fun leaveOrEndClassroom() {
        val repo = classroomRepository ?: return
        val session = _uiState.value.activeClassroom ?: return
        val isTeacher = session.teacherUid == _uiState.value.currentUserUid
        cancelClassroomListeners()
        viewModelScope.launch {
            repo.leaveClassroom(session.roomCode, isTeacher)
            _uiState.update {
                it.copy(
                    activeClassroom = null,
                    classroomMembers = emptyList(),
                    isClassroomConnected = false,
                    classroomError = null
                )
            }
            showBanner(if (isTeacher) "Classroom ended" else "Left classroom")
        }
    }

    private fun startObservingClassroom(roomCode: String) {
        val repo = classroomRepository ?: return
        if (_uiState.value.currentUserUid == null) return

        cancelClassroomListeners()

        classroomSessionJob = viewModelScope.launch {
            repo.observeClassroom(roomCode)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isClassroomConnected = false,
                            classroomError = e.localizedMessage
                        )
                    }
                }
                .collect { session ->
                    if (session == null || session.status == "ended") {
                        _uiState.update {
                            it.copy(
                                activeClassroom = null,
                                classroomMembers = emptyList(),
                                isClassroomConnected = false
                            )
                        }
                        cancelClassroomListeners()
                        showBanner("Live classroom session ended")
                        return@collect
                    }

                    val shouldApplyRemoteBoard = session.pagesJson.isNotBlank() &&
                        session.pagesJson != lastSyncedPagesJson
                    if (shouldApplyRemoteBoard) {
                        lastSyncedPagesJson = session.pagesJson
                        val remotePages = WhiteboardRepository.deserializePages(session.pagesJson)
                        val validIdx = session.currentPageIndex.coerceIn(0, remotePages.lastIndex)
                        _uiState.update {
                            it.copy(
                                activeClassroom = session,
                                pages = remotePages,
                                currentPageIndex = validIdx,
                                isClassroomConnected = true,
                                classroomError = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                activeClassroom = session,
                                currentPageIndex = session.currentPageIndex.coerceIn(0, it.pages.lastIndex),
                                isClassroomConnected = true,
                                classroomError = null
                            )
                        }
                    }
                }
        }

        classroomMembersJob = viewModelScope.launch {
            repo.observeMembers(roomCode)
                .catch { e ->
                    _uiState.update { it.copy(classroomError = e.localizedMessage) }
                }
                .collect { members ->
                    _uiState.update { it.copy(classroomMembers = members) }
                }
        }
    }

    private fun cancelClassroomListeners() {
        classroomSessionJob?.cancel()
        classroomSessionJob = null
        classroomMembersJob?.cancel()
        classroomMembersJob = null
        classroomSyncJob?.cancel()
        classroomSyncJob = null
    }

    private fun syncBoardToActiveClassroom() {
        val repo = classroomRepository ?: return
        val state = _uiState.value
        val session = state.activeClassroom ?: return
        val uid = state.currentUserUid ?: return
        if (!state.canCurrentUserDraw) return

        val isTeacher = session.teacherUid == uid
        classroomSyncJob?.cancel()
        classroomSyncJob = viewModelScope.launch {
            delay(180)
            val latestState = _uiState.value
            val json = WhiteboardRepository.serializePages(latestState.pages)
            lastSyncedPagesJson = json
            repo.syncBoardState(
                roomCode = session.roomCode,
                pagesJson = json,
                currentPageIndex = latestState.currentPageIndex,
                isTeacher = isTeacher
            )
        }
    }

    private fun deepCopyPage(page: BoardPage): BoardPage {
        return page.copy(
            strokes = page.strokes.map { s -> s.copy(points = s.points.toList()) },
            objects = page.objects.map { o -> o.copy() }
        )
    }

    private fun pushUndoSnapshot() {
        val current = deepCopyPage(_uiState.value.currentPage)
        val stack = undoStacks.getOrPut(current.id) { ArrayDeque() }
        if (stack.size >= 35) {
            stack.removeFirst()
        }
        stack.addLast(current)
        redoStacks[current.id]?.clear()
    }

    private fun updateCurrentPage(
        selectObjectId: String? = _uiState.value.selectedObjectId,
        transform: (BoardPage) -> BoardPage
    ) {
        _uiState.update { state ->
            val currentIdx = state.currentPageIndex
            val updatedPage = transform(state.currentPage)
            val newPages = state.pages.mapIndexed { index, page ->
                if (index == currentIdx) updatedPage else page
            }
            val pageId = updatedPage.id
            state.copy(
                pages = newPages,
                selectedObjectId = selectObjectId,
                canUndo = undoStacks[pageId]?.isNotEmpty() == true,
                canRedo = redoStacks[pageId]?.isNotEmpty() == true
            )
        }
        triggerAutoSave()
        syncBoardToActiveClassroom()
    }

    private fun replaceCurrentPageWithoutUndo(newPage: BoardPage) {
        _uiState.update { state ->
            val currentIdx = state.currentPageIndex
            val newPages = state.pages.mapIndexed { index, page ->
                if (index == currentIdx) newPage else page
            }
            val pageId = newPage.id
            state.copy(
                pages = newPages,
                selectedObjectId = null,
                canUndo = undoStacks[pageId]?.isNotEmpty() == true,
                canRedo = redoStacks[pageId]?.isNotEmpty() == true
            )
        }
        triggerAutoSave()
        syncBoardToActiveClassroom()
    }

    private fun triggerAutoSave(immediate: Boolean = false) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            if (!immediate) delay(350)
            val state = _uiState.value
            repository.saveProject(
                id = state.projectId,
                title = state.projectTitle,
                pages = state.pages
            )
            _uiState.update { it.copy(isSaving = false) }
        }
        if (immediate) {
            syncBoardToActiveClassroom()
        }
    }

    private fun showBanner(message: String) {
        bannerJob?.cancel()
        bannerJob = viewModelScope.launch {
            _uiState.update { it.copy(statusBannerMessage = message) }
            delay(2800)
            _uiState.update { it.copy(statusBannerMessage = null) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelClassroomListeners()
    }

    class Factory(
        private val repository: WhiteboardRepository,
        private val classroomRepository: ClassroomRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WhiteboardViewModel(repository, classroomRepository) as T
        }
    }
}
