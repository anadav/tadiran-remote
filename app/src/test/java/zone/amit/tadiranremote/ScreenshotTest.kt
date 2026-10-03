package zone.amit.tadiranremote

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the remote on the JVM. Record with `./gradlew recordRoborazziDebug`. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    private object NoActions : RemoteActions {
        override fun setMode(mode: Mode) {}
        override fun setFan(fan: Fan) {}
        override fun stepTemp(delta: Int) {}
        override fun setSwing(on: Boolean) {}
        override fun setTurbo(on: Boolean) {}
        override fun togglePower() {}
        override fun resend() {}
        override fun setTimer(minutes: Int, turnOn: Boolean) {}
        override fun cancelTimer() {}
    }

    @Composable
    private fun Remote(state: AcState, status: String, timer: AcTimer? = null) = AppTheme {
        RemoteContent(state, timer, status, statusIsError = false, NoActions, onOpenLab = {})
    }

    @Test
    fun cool() = captureRoboImage("../screenshots/cool.png") {
        Remote(AcState(power = true, mode = Mode.Cool, fan = Fan.Auto, temp = 24), "Sent: Cool · Auto · 24°")
    }

    @Test
    @Config(qualifiers = "+night")
    fun heatDark() = captureRoboImage("../screenshots/heat_dark.png") {
        Remote(
            AcState(power = true, mode = Mode.Heat, fan = Fan.Low, temp = 21, swing = true),
            "Sent: Heat · Low · 21° · swing",
            // 23:30 local time on some day; only the clock time is shown.
            timer = AcTimer(java.util.Calendar.getInstance().apply { set(2026, 9, 3, 23, 30) }.timeInMillis, turnOn = false),
        )
    }

    @Test
    fun dry() = captureRoboImage("../screenshots/dry.png") {
        Remote(AcState(power = true, mode = Mode.Dry, fan = Fan.Auto, temp = 26), "Sent: Dry · 26°")
    }

    @Test
    fun lab() = captureRoboImage("build/screenshots/lab.png") {
        AppTheme { LabScreen(RemoteViewModel(RuntimeEnvironment.getApplication()), onBack = {}) }
    }

    @Test
    fun off() = captureRoboImage("../screenshots/off.png") {
        Remote(AcState(power = false), "Sent: Off")
    }
}
