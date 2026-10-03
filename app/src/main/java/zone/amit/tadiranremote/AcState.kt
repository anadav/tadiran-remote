package zone.amit.tadiranremote

import android.content.SharedPreferences

enum class Mode { Cool, Heat, Dry, Fan, Auto }

enum class Fan { Low, Mid, High, Auto }

const val MIN_TEMP = 16
const val MAX_TEMP = 30

data class AcState(
    val power: Boolean = false,
    val mode: Mode = Mode.Cool,
    val fan: Fan = Fan.Auto,
    val temp: Int = 24,
    val swing: Boolean = false,
    val turbo: Boolean = false,
) {
    /** The remote ignores the fan speed in Dry mode. */
    val fanApplies: Boolean
        get() = mode != Mode.Dry

    /** Fan mode has no Auto speed; the remote sends Low instead. */
    val autoFanApplies: Boolean
        get() = mode != Mode.Fan

    /** Turbo ("Max" in IRremoteESP8266) only exists for Cool and Heat. */
    val turboApplies: Boolean
        get() = mode == Mode.Cool || mode == Mode.Heat

    fun describe(): String =
        if (!power) "Off"
        else listOfNotNull(
            mode.name,
            fan.name.takeIf { fanApplies },
            "${temp}°",
            "swing".takeIf { swing },
            "turbo".takeIf { turbo && turboApplies },
        ).joinToString(" · ")

    fun save(prefs: SharedPreferences) {
        prefs.edit()
            .putBoolean("power", power)
            .putString("mode", mode.name)
            .putString("fan", fan.name)
            .putInt("temp", temp)
            .putBoolean("swing", swing)
            .putBoolean("turbo", turbo)
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
                swing = prefs.getBoolean("swing", default.swing),
                turbo = prefs.getBoolean("turbo", default.turbo),
            )
        }

        private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
            enumValues<T>().firstOrNull { it.name == name }
    }
}
