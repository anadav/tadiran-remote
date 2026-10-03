package zone.amit.tadiranremote

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the remote on the JVM. Record with `./gradlew recordRoborazziDebug`. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @Composable
    private fun Remote(state: AcState, status: String) = AppTheme {
        RemoteContent(state, status, statusIsError = false, {}, {}, {}, {}, {})
    }

    @Test
    fun cool() = captureRoboImage("../screenshots/cool.png") {
        Remote(AcState(power = true, mode = Mode.Cool, fan = Fan.Auto, temp = 24), "Sent: Cool · Auto · 24°")
    }

    @Test
    @Config(qualifiers = "+night")
    fun heatDark() = captureRoboImage("../screenshots/heat_dark.png") {
        Remote(AcState(power = true, mode = Mode.Heat, fan = Fan.Low, temp = 21), "Sent: Heat · Low · 21°")
    }

    @Test
    fun dry() = captureRoboImage("../screenshots/dry.png") {
        Remote(AcState(power = true, mode = Mode.Dry, fan = Fan.Auto, temp = 26), "Sent: Dry · 26°")
    }

    @Test
    fun off() = captureRoboImage("../screenshots/off.png") {
        Remote(AcState(power = false), "Sent: Off")
    }
}
