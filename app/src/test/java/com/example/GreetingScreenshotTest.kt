package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.remote.CurrentForecast
import com.example.ui.CurrentWeatherCard
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
    val mockCurrent = CurrentForecast(
        time = "2026-06-11T12:00",
        temperature2m = 23.5,
        relativeHumidity2m = 65.0,
        apparentTemperature = 24.2,
        isDay = 1,
        precipitation = 0.0,
        weatherCode = 0,
        windSpeed10m = 12.4
    )
    composeTestRule.setContent { 
        MyApplicationTheme { 
            CurrentWeatherCard(current = mockCurrent) 
        } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
