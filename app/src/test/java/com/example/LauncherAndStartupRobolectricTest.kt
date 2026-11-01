package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LauncherAndStartupRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun packageManager_resolvesLaunchIntentAndLauncherCategory_andMainActivityRenders() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageManager = context.packageManager

        // 1. Verify PackageManager.getLaunchIntentForPackage returns non-null (enables 'Open' button in Android Settings > Manage apps)
        val launchIntent = packageManager.getLaunchIntentForPackage(context.packageName)
        assertNotNull(
            "getLaunchIntentForPackage must return a non-null Intent so Android Settings enables the Open button",
            launchIntent
        )
        assertEquals(MainActivity::class.java.name, launchIntent?.component?.className)

        // 2. Verify ACTION_MAIN + CATEGORY_LAUNCHER resolves MainActivity (displays icon in Android Home Screen Launcher)
        val launcherQuery = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(context.packageName)
        }
        val resolvedActivities = packageManager.queryIntentActivities(launcherQuery, 0)
        assertTrue(
            "MainActivity must be resolved for ACTION_MAIN + CATEGORY_LAUNCHER so the app icon appears in the launcher menu",
            resolvedActivities.any { it.activityInfo.name == MainActivity::class.java.name && it.activityInfo.enabled && it.activityInfo.exported }
        )

        // 3. Verify MainActivity boots and renders the interactive whiteboard canvas & studio controls cleanly
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("whiteboard_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("project_drawer_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("undo_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("focus_mode_toggle_button").assertExists()
    }
}
