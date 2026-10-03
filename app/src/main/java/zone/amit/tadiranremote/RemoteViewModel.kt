package zone.amit.tadiranremote

import android.app.Application
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Rapid taps (e.g. + + +) only send the state they end on. */
private const val DEBOUNCE_MS = 350L

class RemoteViewModel(app: Application) : AndroidViewModel(app), RemoteActions {
    private val prefs = AcTimer.prefs(app)
    private val sender = IrSender(app)
    private var pending: Job? = null

    var state by mutableStateOf(AcState.load(prefs))
        private set

    var timer by mutableStateOf(AcTimer.load(prefs))
        private set

    var status by mutableStateOf(if (sender.available) "" else "This phone has no IR emitter")
        private set

    /** Increments after every successful transmission, so the UI can give haptic feedback. */
    var sentCount by mutableIntStateOf(0)
        private set

    val hasEmitter: Boolean get() = sender.available

    // The timer fires in TimerReceiver, possibly while the app is open: pick up its changes.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
        state = AcState.load(p)
        timer = AcTimer.load(p)
        if (key == AcTimer.KEY_LAST_EVENT) status = p.getString(key, null).orEmpty()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onCleared() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun setMode(mode: Mode) = change {
        // Fan mode has no Auto speed.
        it.copy(mode = mode, fan = if (mode == Mode.Fan && it.fan == Fan.Auto) Fan.Low else it.fan)
    }

    override fun setFan(fan: Fan) = change { it.copy(fan = fan) }

    override fun stepTemp(delta: Int) = change { it.copy(temp = (it.temp + delta).coerceIn(MIN_TEMP, MAX_TEMP)) }

    override fun setSwing(on: Boolean) = change { it.copy(swing = on) }

    override fun setTurbo(on: Boolean) = change { it.copy(turbo = on) }

    override fun togglePower() {
        update(state.copy(power = !state.power))
        send(TadiranProtocol.encode(state), state.describe(), debounce = false)
    }

    /** Re-sends the current state, for when the AC was changed by its own remote. */
    override fun resend() = send(TadiranProtocol.encode(state), state.describe(), debounce = false)

    override fun setTimer(minutes: Int, turnOn: Boolean) {
        val timer = AcTimer(System.currentTimeMillis() + minutes * 60_000L, turnOn)
        AcTimer.schedule(getApplication(), timer)
        status = "Timer set: ${timer.describe()}"
    }

    override fun cancelTimer() {
        AcTimer.cancel(getApplication())
        status = "Timer cancelled"
    }

    /** Sends a hand-edited frame from the lab screen; the checksum is recomputed. */
    fun sendProbe(frame: ByteArray) {
        val fixed = TadiranProtocol.withChecksum(frame)
        send(fixed, TadiranProtocol.hex(fixed), debounce = false)
    }

    private fun change(transform: (AcState) -> AcState) {
        val next = transform(state)
        if (next == state) return
        update(next)
        if (state.power) send(TadiranProtocol.encode(state), state.describe(), debounce = true)
    }

    private fun update(next: AcState) {
        state = next
        state.save(prefs)
    }

    private fun send(frame: ByteArray, label: String, debounce: Boolean) {
        pending?.cancel()
        pending = viewModelScope.launch {
            if (debounce) delay(DEBOUNCE_MS)
            status = if (!sender.available) {
                "This phone has no IR emitter"
            } else try {
                withContext(Dispatchers.IO) { sender.send(frame) }
                sentCount++
                "Sent: $label"
            } catch (e: Exception) {
                "Send failed: ${e.message}"
            }
        }
    }
}
