package com.hayhak.currencyconverter

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ConverterToolsSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun toolsOpenAndTabsAreReachable() {
        compose.onNodeWithText(compose.activity.getString(R.string.tools_title)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.tools_percent)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.tools_history)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.tools_history_note)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.tools_travel)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.tools_country)).assertIsDisplayed()
    }
}
