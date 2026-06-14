package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.JournalEntry
import com.example.ui.screens.JournalItemCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val mockEntry = JournalEntry(
        title = "A Peaceful Mind",
        content = "Spent the morning sitting by the lake watching ripples cascade. The cool summer air felt immensely restorative.",
        mood = "Calm",
        sentimentScore = 4.5f,
        aiSummary = "Reflecting near nature brings deep neural alignment and calm, enhancing overall restorative metrics."
    )

    composeTestRule.setContent { 
        MyApplicationTheme { 
            JournalItemCard(entry = mockEntry, onDelete = {}) 
        } 
    } 

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
