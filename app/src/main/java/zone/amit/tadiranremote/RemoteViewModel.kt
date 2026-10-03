package zone.amit.tadiranremote

import android.app.Application
import android.content.Context
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

const val CODES_ASSET = "Tadiran_1345_full.ir"

/** Rapid taps (e.g. + + +) only send the state they end on. */
private const val DEBOUNCE_MS = 350L

class RemoteViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("ac_state", Context.MODE_PRIVATE)
    private val sender = IrSender(app)
    private val codes = app.assets.open(CODES_ASSET).bufferedReader().use { parseFlipperIr(it.readText()) }
    private var pending: Job? = null

    var state by mutableStateOf(AcState.load(prefs))
        private set

    var status by mutableStateOf(if (sender.available) "" else "This phone has no IR emitter")
        private set

    /** Increments after every successful transmission, so the UI can give haptic feedback. */
    var sentCount by mutableIntStateOf(0)
        private set

    val hasEmitter: Boolean get() = sender.available

    fun setMode(mode: Mode) = change { it.copy(mode = mode) }

    fun setFan(fan: Fan) = change { it.copy(fan = fan) }

    fun stepTemp(delta: Int) = change { it.copy(temp = (it.temp + delta).coerceIn(MIN_TEMP, MAX_TEMP)) }

    fun togglePower() {
        state = state.copy(power = !state.power)
        state.save(prefs)
        send(debounce = false)
    }

    /** Re-sends the current state, for when the AC was changed by its own remote. */
    fun resend() = send(debounce = false)

    private fun change(transform: (AcState) -> AcState) {
        val next = transform(state)
        if (next == state) return
        state = next
        state.save(prefs)
        if (state.power) send(debounce = true)
    }

    private fun send(debounce: Boolean) {
        pending?.cancel()
        val target = state
        pending = viewModelScope.launch {
            if (debounce) delay(DEBOUNCE_MS)
            val signal = codes[target.signalName]
            status = when {
                !sender.available -> "This phone has no IR emitter"
                signal == null -> "No code for ${target.signalName}"
                else -> try {
                    withContext(Dispatchers.IO) { sender.send(signal) }
                    sentCount++
                    "Sent: ${describe(target)}"
                } catch (e: Exception) {
                    "Send failed: ${e.message}"
                }
            }
        }
    }

    private fun describe(s: AcState): String =
        if (!s.power) "Off"
        else listOfNotNull(s.mode.name, s.fan.name.takeIf { s.fanApplies }, "${s.temp}°").joinToString(" · ")
}
