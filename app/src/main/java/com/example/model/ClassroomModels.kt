package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ClassroomSession(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val title: String = "",
    val teacherUid: String = "",
    val teacherName: String = "",
    val status: String = "active",
    val allowStudentDrawing: Boolean = false,
    val currentPageIndex: Int = 0,
    val participantCount: Int = 1,
    val pagesJson: String = "[]",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ClassroomMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roomCode: String = "",
    val displayName: String = "",
    val role: String = "student",
    val canDraw: Boolean = false,
    @ServerTimestamp val joinedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class StudioPreferences(
    val enableTwoFingerZoomPan: Boolean = true,
    val showHandPanTool: Boolean = true,
    val showBrushesTab: Boolean = true,
    val showEraserTab: Boolean = true,
    val showShapesTab: Boolean = true,
    val showGeometryTab: Boolean = true,
    val showMathTab: Boolean = true,
    val showInsertTab: Boolean = true,
    val showBackgroundTab: Boolean = true
)
