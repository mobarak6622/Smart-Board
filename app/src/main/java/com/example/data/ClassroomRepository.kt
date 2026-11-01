package com.example.data

import android.content.Context
import com.example.R
import com.example.model.ClassroomMember
import com.example.model.ClassroomSession
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class ClassroomRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Live Classroom.")
    }

    private fun currentUserDisplayName(): String {
        val user = auth.currentUser
        return user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Participant"
    }

    suspend fun createClassroom(
        title: String,
        pagesJson: String,
        currentPageIndex: Int,
        explicitRoomCode: String? = null
    ): Result<ClassroomSession> {
        return try {
            val uid = requireUserId()
            val teacherName = currentUserDisplayName().take(80)
            val roomCode = explicitRoomCode ?: generateRoomCode()
            val cleanTitle = title.trim().ifBlank { "Live SmartBoard Class" }.take(120)
            val safePagesJson = pagesJson.ifBlank { "[]" }

            val docRef = db.collection("classrooms").document(roomCode)
            val payload = mapOf(
                "roomCode" to roomCode,
                "title" to cleanTitle,
                "teacherUid" to uid,
                "teacherName" to teacherName,
                "status" to "active",
                "allowStudentDrawing" to false,
                "currentPageIndex" to currentPageIndex.coerceIn(0, 200),
                "participantCount" to 1,
                "pagesJson" to safePagesJson,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            val memberRef = docRef.collection("members").document(uid)
            val memberPayload = mapOf(
                "userId" to uid,
                "roomCode" to roomCode,
                "displayName" to teacherName,
                "role" to "teacher",
                "canDraw" to true,
                "joinedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            memberRef.set(memberPayload).await()

            val created = docRef.get().await()
                .toObject(ClassroomSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: ClassroomSession(
                    id = roomCode,
                    roomCode = roomCode,
                    title = cleanTitle,
                    teacherUid = uid,
                    teacherName = teacherName,
                    status = "active",
                    allowStudentDrawing = false,
                    currentPageIndex = currentPageIndex,
                    participantCount = 1,
                    pagesJson = safePagesJson
                )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "classrooms")
            Result.failure(e)
        }
    }

    suspend fun joinClassroom(roomCodeInput: String): Result<ClassroomSession> {
        val roomCode = roomCodeInput.trim().uppercase()
        val path = "classrooms/$roomCode"
        return try {
            val uid = requireUserId()
            val displayName = currentUserDisplayName().take(80)
            val docRef = db.collection("classrooms").document(roomCode)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(IllegalArgumentException("Classroom $roomCode not found"))
            }
            val session = snapshot.toObject(
                ClassroomSession::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("Invalid classroom data"))

            if (session.status != "active") {
                return Result.failure(IllegalStateException("Classroom $roomCode has ended"))
            }

            val isTeacher = session.teacherUid == uid
            val memberRef = docRef.collection("members").document(uid)
            val alreadyExists = try {
                memberRef.get().await().exists()
            } catch (e: Exception) {
                false
            }
            if (!alreadyExists) {
                val memberPayload = mapOf(
                    "userId" to uid,
                    "roomCode" to roomCode,
                    "displayName" to displayName,
                    "role" to if (isTeacher) "teacher" else "student",
                    "canDraw" to isTeacher,
                    "joinedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                memberRef.set(memberPayload).await()
                if (!isTeacher) {
                    docRef.update(
                        mapOf(
                            "participantCount" to (session.participantCount + 1).coerceAtMost(500),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(session)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    fun observeClassroom(roomCode: String): Flow<ClassroomSession?> {
        val path = "classrooms/$roomCode"
        return db.collection("classrooms").document(roomCode)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        ClassroomSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else null
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeMembers(roomCode: String): Flow<List<ClassroomMember>> {
        val path = "classrooms/$roomCode/members"
        return db.collection("classrooms").document(roomCode)
            .collection("members")
            .whereEqualTo("roomCode", roomCode)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(
                    ClassroomMember::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun syncBoardState(
        roomCode: String,
        pagesJson: String,
        currentPageIndex: Int,
        isTeacher: Boolean
    ): Result<Unit> {
        val path = "classrooms/$roomCode"
        return try {
            requireUserId()
            val docRef = db.collection("classrooms").document(roomCode)
            val updates = if (isTeacher) {
                mapOf(
                    "pagesJson" to pagesJson,
                    "currentPageIndex" to currentPageIndex.coerceIn(0, 200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            } else {
                mapOf(
                    "pagesJson" to pagesJson,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setGlobalStudentDrawing(roomCode: String, allow: Boolean): Result<Unit> {
        val path = "classrooms/$roomCode"
        return try {
            requireUserId()
            db.collection("classrooms").document(roomCode)
                .update(
                    mapOf(
                        "allowStudentDrawing" to allow,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setStudentCanDraw(
        roomCode: String,
        studentUid: String,
        canDraw: Boolean
    ): Result<Unit> {
        val path = "classrooms/$roomCode/members/$studentUid"
        return try {
            requireUserId()
            db.collection("classrooms").document(roomCode)
                .collection("members").document(studentUid)
                .update(
                    mapOf(
                        "canDraw" to canDraw,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveClassroom(roomCode: String, isTeacher: Boolean): Result<Unit> {
        val path = "classrooms/$roomCode"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("classrooms").document(roomCode)
            if (isTeacher) {
                docRef.update(
                    mapOf(
                        "status" to "ended",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.collection("members").document(uid).delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    companion object {
        fun generateRoomCode(): String {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val suffix = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
            return "SB$suffix"
        }
    }
}
