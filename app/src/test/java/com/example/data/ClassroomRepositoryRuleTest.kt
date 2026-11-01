package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ClassroomRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun uniqueRoomCode(): String =
        "SB" + UUID.randomUUID().toString().replace("-", "").take(6).uppercase()

    @Test
    fun createAndJoinClassroom_teacherAndStudentSync_succeeds() = runBlocking {
        val roomCode = uniqueRoomCode()
        val teacherUid = signInTestUser("teacher@smartboard.test")
        val teacherRepo = ClassroomRepository(firestore, auth)

        val created = withTimeout(5000L) {
            teacherRepo.createClassroom(
                title = "Algebra 101",
                pagesJson = "[{\"id\":\"page1\"}]",
                currentPageIndex = 0,
                explicitRoomCode = roomCode
            ).getOrThrow()
        }
        assertEquals(roomCode, created.roomCode)
        assertEquals(teacherUid, created.teacherUid)

        // Student joins
        val studentUid = signInTestUser("student@smartboard.test")
        val studentRepo = ClassroomRepository(firestore, auth)
        val joined = withTimeout(5000L) {
            studentRepo.joinClassroom(roomCode).getOrThrow()
        }
        assertEquals("Algebra 101", joined.title)

        // Student is in view-only mode by default -> syncBoardState should fail with PERMISSION_DENIED
        val syncAttempt = withTimeout(5000L) {
            studentRepo.syncBoardState(
                roomCode = roomCode,
                pagesJson = "[{\"id\":\"hacked\"}]",
                currentPageIndex = 0,
                isTeacher = false
            )
        }
        assertTrue(syncAttempt.isFailure)

        // Teacher grants individual drawing permission to student
        signInTestUser("teacher@smartboard.test")
        val grantResult = withTimeout(5000L) {
            teacherRepo.setStudentCanDraw(roomCode, studentUid, true)
        }
        assertTrue(grantResult.isSuccess)

        // Now student CAN sync strokes
        signInTestUser("student@smartboard.test")
        val allowedSync = withTimeout(5000L) {
            studentRepo.syncBoardState(
                roomCode = roomCode,
                pagesJson = "[{\"id\":\"student_stroke\"}]",
                currentPageIndex = 0,
                isTeacher = false
            )
        }
        assertTrue(allowedSync.isSuccess)
    }

    @Test
    fun observeClassroom_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val roomCode = uniqueRoomCode()
        auth.signOut()
        val repo = ClassroomRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(3000L) {
                repo.observeClassroom(roomCode).first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull(caught)
        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        assertFalse(auth.currentUser != null)
    }
}
