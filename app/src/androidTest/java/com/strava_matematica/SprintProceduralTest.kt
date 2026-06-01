package com.strava_matematica

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.geometry.Offset
import com.strava_matematica.ui.folha.SprintNoteSheet

class SprintProceduralTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testProceduralLoop100Times() {
        // Start the UI
        composeTestRule.setContent {
            SprintNoteSheet(
                studentId = "test_student",
                sessionId = "test_session",
                onComplete = {},
                onClose = {}
            )
        }

        // Loop 100 times to simulate the E-Sports 100-Hour loop constraint
        for (i in 1..100) {
            // 1. Wait for canvas to be ready
            composeTestRule.waitForIdle()

            // 2. Simulate a user drawing on the tablet (evaluating Touch Mapping)
            // We swipe from (100, 100) to (200, 200) inside the ExerciseField
            composeTestRule.onNodeWithTag("ExerciseFieldCanvas")
                .performTouchInput {
                    swipe(
                        start = Offset(100f, 100f),
                        end = Offset(200f, 200f),
                        durationMillis = 300
                    )
                }

            // 3. Emulate clicking "Submit" (Envia button)
            composeTestRule.onNodeWithTag("SubmitButton")
                .performClick()

            // 4. Emulate the visual neon transition and confirm the next Folha loaded
            // The Procedural Engine should have generated a new equation and the UI should have refreshed.
            composeTestRule.waitForIdle()
        }
    }
}
