package com.amischolar.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class StudentWorkspaceInstrumentedTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun studentWorkspaceOpensWithLocalCoursesTasksAndFocus() {
        composeRule.onNodeWithText("Open student workspace", substring = true).performClick()
        composeRule.onNodeWithText("Student workspace").assertIsDisplayed()
        composeRule.onNodeWithText("Your coursework and study progress stays on this device.").assertExists()
        composeRule.onNodeWithText("Focus").performClick()
        composeRule.onNodeWithText("Pomodoro focus · timer recovery is saved locally").assertExists()
    }
}
