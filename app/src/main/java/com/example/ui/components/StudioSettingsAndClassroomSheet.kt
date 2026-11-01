package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.localization.AppLanguage
import com.example.localization.StudioStrings
import com.example.model.ClassroomMember
import com.example.model.ClassroomSession
import com.example.model.StudioPreferences
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioSlate200
import com.example.ui.theme.StudioSlate700
import com.example.ui.theme.StudioSlate900
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 1. Customizable Studio & Toolbar Settings Dialog ("সেটিংস থেকে আমার মন মতো টুলস চালু করার ব্যবস্থা")
 */
@Composable
fun StudioSettingsDialog(
    preferences: StudioPreferences,
    language: AppLanguage,
    strings: StudioStrings,
    onUpdatePreferences: (StudioPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    val isBn = language == AppLanguage.BENGALI

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = ElectricIndigo)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBn) "টুলবার ও জুম সেটিংস" else "Toolbar & Gesture Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isBn) "ক্যানভাস জুম এবং প্যান নিয়ন্ত্রণ" else "Canvas Zoom & Pan Controls",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricIndigo
                )

                PreferenceToggleRow(
                    label = if (isBn) "দুই আঙুলে পিঞ্চ জুম ও প্যান" else "Two-Finger Pinch Zoom & Pan",
                    checked = preferences.enableTwoFingerZoomPan,
                    onCheckedChange = {
                        onUpdatePreferences(preferences.copy(enableTwoFingerZoomPan = it))
                    },
                    testTag = "pref_two_finger_zoom"
                )

                PreferenceToggleRow(
                    label = if (isBn) "হ্যান্ড প্যান/জুম টুল বাটন দেখান" else "Show Hand Pan / Zoom Tool in Dock",
                    checked = preferences.showHandPanTool,
                    onCheckedChange = {
                        onUpdatePreferences(preferences.copy(showHandPanTool = it))
                    },
                    testTag = "pref_hand_pan_tool"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = if (isBn) "পছন্দমতো নিচের টুলবার বাটন চালু/বন্ধ করুন" else "Customize Visible Bottom Toolbar Tools",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricIndigo
                )

                PreferenceToggleRow(
                    label = strings.tabDraw,
                    checked = preferences.showBrushesTab,
                    onCheckedChange = { onUpdatePreferences(preferences.copy(showBrushesTab = it)) }
                )
                PreferenceToggleRow(
                    label = strings.tabEraser,
                    checked = preferences.showEraserTab,
                    onCheckedChange = { onUpdatePreferences(preferences.copy(showEraserTab = it)) }
                )
                PreferenceToggleRow(
                    label = strings.tabShapes,
                    checked = preferences.showShapesTab,
                    onCheckedChange = { onUpdatePreferences(preferences.copy(showShapesTab = it)) }
                )
                PreferenceToggleRow(
                    label = strings.tabGeometry,
                    checked = preferences.showGeometryTab,
                    onCheckedChange = { onUpdatePreferences(preferences.copy(showGeometryTab = it)) }
                )
                PreferenceToggleRow(
                    label = strings.tabMath,
                    checked = preferences.showMathTab,
                    onCheckedChange = { onUpdatePreferences(preferences.copy(showMathTab = it)) }
                )
                PreferenceToggleRow(
                    label = strings.tabInsert,
                    checked = preferences.showInsertTab,
                    onCheckedChange = { onUpdatePreferences(preferences.copy(showInsertTab = it)) }
                )
                PreferenceToggleRow(
                    label = strings.tabBackground,
                    checked = preferences.showBackgroundTab,
                    onCheckedChange = { onUpdatePreferences(preferences.copy(showBackgroundTab = it)) }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(if (isBn) "সম্পন্ন" else "Done")
            }
        }
    )
}

@Composable
private fun PreferenceToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier
        )
    }
}

/**
 * 2. Real-Time Online Classroom Sheet (Powered by Firebase Auth Google Sign-In & Cloud Firestore)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveClassroomSheet(
    language: AppLanguage,
    strings: StudioStrings,
    currentUserUid: String?,
    currentUserName: String?,
    currentUserEmail: String?,
    activeClassroom: ClassroomSession?,
    classroomMembers: List<ClassroomMember>,
    isClassroomConnected: Boolean,
    classroomError: String?,
    onAuthSuccess: () -> Unit,
    onSignOutSuccess: () -> Unit,
    onCreateClassroom: (String) -> Unit,
    onJoinClassroom: (String) -> Unit,
    onToggleGlobalStudentDrawing: (Boolean) -> Unit,
    onToggleStudentPermission: (String, Boolean) -> Unit,
    onLeaveOrEndClassroom: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isBn = language == AppLanguage.BENGALI

    var classTitleInput by remember { mutableStateOf("Physics & Math Live Class") }
    var joinCodeInput by remember { mutableStateOf("") }
    var authErrorMsg by remember { mutableStateOf<String?>(null) }

    // Attempt silent auto sign-in when opening Classroom Sheet if not signed in
    LaunchedEffect(currentUserUid) {
        if (currentUserUid == null) {
            attemptSilentGoogleSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = onAuthSuccess,
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = ElectricIndigo,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isBn) "লাইভ অনলাইন ক্লাসরুম" else "Live Online Classroom",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (isBn)
                                "রিয়েল-টাইম হোয়াইটবোর্ড সিঙ্ক ও শিক্ষার্থী নিয়ন্ত্রণ"
                            else
                                "Real-time Firebase Firestore whiteboard sync",
                            style = MaterialTheme.typography.labelMedium,
                            color = StudioSlate700
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.cancelBtn)
                }
            }

            if (classroomError != null || authErrorMsg != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = classroomError ?: authErrorMsg.orEmpty(),
                        color = CrimsonDanger,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Auth Gate for Online Classroom (While keeping local whiteboard 100% available offline)
            if (currentUserUid == null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioSlate200, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = ElectricIndigo,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (isBn)
                                "লাইভ ক্লাসরুম শুরু বা যোগ দিতে গুগল দিয়ে সাইন ইন করুন"
                            else
                                "Sign in with Google to Host or Join a Live Classroom",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBn)
                                "অফলাইন ড্রয়িং সাইন ইন ছাড়াই সবসময় কাজ করবে। অনলাইন রিয়েল-টাইম ক্লাসের জন্য নিরাপদ গুগল যাচাইকরণ প্রয়োজন।"
                            else
                                "Your offline whiteboard always works without login. Real-time classroom sync uses verified Google Sign-In.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StudioSlate700
                        )

                        GoogleSignInButton(
                            onAuthSuccess = {
                                authErrorMsg = null
                                onAuthSuccess()
                            },
                            onAuthError = { err ->
                                authErrorMsg = err
                            }
                        )
                    }
                }
            } else if (activeClassroom == null) {
                // Signed-In User: Create Classroom (Teacher) OR Join Classroom (Student)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = currentUserName ?: "Teacher / Student",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUserEmail.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioSlate700
                            )
                        }
                        TextButton(
                            onClick = {
                                signOutGoogle(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = onSignOutSuccess,
                                    scope = scope
                                )
                            }
                        ) {
                            Text(if (isBn) "সাইন আউট" else "Sign Out")
                        }
                    }
                }

                // 1. Teacher: Create Live Classroom Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioSlate200, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isBn) "শিক্ষক: নতুন ক্লাসরুম তৈরি করুন" else "Teacher: Create a Live Classroom",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricIndigo
                        )
                        OutlinedTextField(
                            value = classTitleInput,
                            onValueChange = { classTitleInput = it },
                            label = { Text(if (isBn) "ক্লাসের বিষয় / নাম" else "Lesson Title") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_classroom_title_input")
                        )
                        Button(
                            onClick = { onCreateClassroom(classTitleInput) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_classroom_button")
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBn) "লাইভ ক্লাসরুম শুরু করুন" else "Start Live Classroom & Generate ID")
                        }
                    }
                }

                // 2. Student: Join Existing Classroom by Code
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioSlate200, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isBn) "শিক্ষার্থী: ক্লাসরুম আইডি দিয়ে যোগ দিন" else "Student: Join with Classroom ID",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { joinCodeInput = it.uppercase() },
                                placeholder = { Text("e.g. SB4829") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("join_classroom_code_input")
                            )
                            Button(
                                onClick = {
                                    if (joinCodeInput.isNotBlank()) {
                                        onJoinClassroom(joinCodeInput)
                                    }
                                },
                                modifier = Modifier.testTag("join_classroom_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isBn) "যোগ দিন" else "Join")
                            }
                        }
                    }
                }
            } else {
                // Active Live Classroom Dashboard (Teacher or Student View)
                val isTeacher = activeClassroom.teacherUid == currentUserUid
                val myMemberDoc = classroomMembers.firstOrNull { it.userId == currentUserUid }
                val canIDraw = isTeacher || activeClassroom.allowStudentDrawing || (myMemberDoc?.canDraw == true)

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = StudioSlate900,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isClassroomConnected) EmeraldSuccess else Color(0xFFF59E0B))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isClassroomConnected) "LIVE • REAL-TIME SYNCED" else "RECONNECTING...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSuccess
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (canIDraw) ElectricIndigo else Color(0xFF334155)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (canIDraw) Icons.Default.Edit else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when {
                                            isTeacher -> if (isBn) "শিক্ষক (হোস্ট)" else "Teacher (Host)"
                                            canIDraw -> if (isBn) "আঁকার অনুমতি আছে" else "Drawing Enabled"
                                            else -> if (isBn) "শুধু দেখার মোড" else "View-Only Mode"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PureWhite
                                    )
                                }
                            }
                        }

                        Text(
                            text = activeClassroom.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ID: ${activeClassroom.roomCode}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(activeClassroom.roomCode))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Classroom ID",
                                        tint = PureWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onLeaveOrEndClassroom,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isTeacher) (if (isBn) "ক্লাস শেষ করুন" else "End Class")
                                    else (if (isBn) "বের হন" else "Leave")
                                )
                            }
                        }
                    }
                }

                // Teacher Global Permission Switch
                if (isTeacher) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StudioSlate200, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBn) "সব শিক্ষার্থীকে বোর্ডে আঁকার অনুমতি দিন" else "Allow All Students to Draw",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isBn)
                                        "বন্ধ থাকলে শিক্ষার্থীরা শুধু দেখার মোডে (View-Only) থাকবে"
                                    else
                                        "When off, students are in View-Only mode unless individually permitted below",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioSlate700
                                )
                            }
                            Switch(
                                checked = activeClassroom.allowStudentDrawing,
                                onCheckedChange = onToggleGlobalStudentDrawing,
                                modifier = Modifier.testTag("toggle_all_students_draw_switch")
                            )
                        }
                    }
                }

                // Live Classroom Participant Roster
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                    Text(
                        text = if (isBn)
                            "অংশগ্রহণকারী তালিকা (${classroomMembers.size})"
                        else
                            "Participants (${classroomMembers.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(classroomMembers, key = { it.userId }) { member ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, StudioSlate200, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (member.role == "teacher") Icons.Default.School else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (member.role == "teacher") ElectricIndigo else StudioSlate700
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = member.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (member.role == "teacher") "Teacher" else if (member.canDraw || activeClassroom.allowStudentDrawing) "Student • Can Draw" else "Student • View-Only",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StudioSlate700
                                        )
                                    }
                                }

                                if (isTeacher && member.role == "student") {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isBn) "আঁকা" else "Draw",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = member.canDraw,
                                            onCheckedChange = { allowed ->
                                                onToggleStudentPermission(member.userId, allowed)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mandatory Google Sign-In via Jetpack CredentialManager (GetSignInWithGoogleOption & GetGoogleIdOption)
 */
fun attemptSilentGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

@Composable
fun GoogleSignInButton(
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isLoading = true
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                },
                onAuthError = { errorMsg ->
                    isLoading = false
                    onAuthError(errorMsg)
                },
                scope = coroutineScope,
                onAuthCancelled = { isLoading = false }
            )
        },
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("google_sign_in_button")
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text("Sign in with Google")
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutGoogle(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}
