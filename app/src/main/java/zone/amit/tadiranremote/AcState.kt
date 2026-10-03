package zone.amit.tadiranremote

import android.content.SharedPreferences

/** Enum names match the signal names in tadiran-irdb, e.g. `Cool_Auto_24`. */
enum class Mode { Cool, Heat, Dry, Fan, Auto }

enum class Fan { Low, Mid, High, Auto }

const val MIN_TEMP = 16
const val MAX_TEMP = 30
const val OFF_SIGNAL = "Off"

data class AcState(
    val power: Boolean = false,
    val mode: Mode = Mode.Cool,
    val fan: Fan = Fan.Auto,
    val temp: Int = 24,
) {
    /** Name of the signal that puts the AC into this state. Every Tadiran code also powers on. */
    val signalName: String
        get() = if (power) "${mode.name}_${fan.name}_$temp" else OFF_SIGNAL

    /** SmartIR's dry-mode codes are identical for every fan speed. */
    val fanApplies: Boolean
        get() = mode != Mode.Dry

    fun save(prefs: SharedPreferences) {
        prefs.edit()
            .putBoolean("power", power)
            .putString("mode", mode.name)
            .putString("fan", fan.name)
            .putInt("temp", temp)
            .apply()
    }

    companion object {
        fun load(prefs: SharedPreferences): AcState {
            val default = AcState()
            return AcState(
                power = prefs.getBoolean("power", default.power),
                mode = enumOrNull<Mode>(prefs.getString("mode", null)) ?: default.mode,
                fan = enumOrNull<Fan>(prefs.getString("fan", null)) ?: default.fan,
                temp = prefs.getInt("temp", default.temp).coerceIn(MIN_TEMP, MAX_TEMP),
            )
        }

        private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
            enumValues<T>().firstOrNull { it.name == name }
    }
}
