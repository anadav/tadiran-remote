package zone.amit.tadiranremote

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import java.text.DateFormat
import java.util.Date

const val PREFS = "ac_state"

/** A pending "turn the AC on/off at [atMillis]" sent by the phone itself. */
data class AcTimer(val atMillis: Long, val turnOn: Boolean) {
    fun describe(): String {
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(atMillis))
        return "${if (turnOn) "On" else "Off"} at $time"
    }

    companion object {
        private const val KEY_AT = "timer_at"
        private const val KEY_ON = "timer_on"
        const val KEY_LAST_EVENT = "timer_last_event"

        fun load(prefs: SharedPreferences): AcTimer? {
            val at = prefs.getLong(KEY_AT, 0)
            return if (at > 0) AcTimer(at, prefs.getBoolean(KEY_ON, false)) else null
        }

        fun schedule(context: Context, timer: AcTimer) {
            prefs(context).edit().putLong(KEY_AT, timer.atMillis).putBoolean(KEY_ON, timer.turnOn).apply()
            arm(context, timer.atMillis)
        }

        fun cancel(context: Context) {
            prefs(context).edit().remove(KEY_AT).remove(KEY_ON).apply()
            context.getSystemService(AlarmManager::class.java).cancel(firePending(context))
        }

        /**
         * setAlarmClock is the alarm type least affected by Doze and vendor battery savers;
         * it also shows the alarm icon in the status bar while the timer is set.
         */
        internal fun arm(context: Context, atMillis: Long) {
            val alarms = context.getSystemService(AlarmManager::class.java)
            val show = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
            )
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
                alarms.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, show), firePending(context))
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, firePending(context))
            }
        }

        private fun firePending(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context, 0, Intent(context, TimerReceiver::class.java), PendingIntent.FLAG_IMMUTABLE,
        )

        internal fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
}

/** Fires the timer: sends the AC state with power switched, and records it. */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = AcTimer.prefs(context)
        val timer = AcTimer.load(prefs) ?: return
        AcTimer.cancel(context)
        val state = AcState.load(prefs).copy(power = timer.turnOn)
        state.save(prefs)
        val result = try {
            IrSender(context).send(state)
            "Timer: sent ${state.describe()}"
        } catch (e: Exception) {
            "Timer failed: ${e.message}"
        }
        prefs.edit().putString(AcTimer.KEY_LAST_EVENT, result).apply()
    }
}

/** Alarms are lost on reboot; re-arm a timer that is still in the future. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val timer = AcTimer.load(AcTimer.prefs(context)) ?: return
        if (timer.atMillis > System.currentTimeMillis()) AcTimer.arm(context, timer.atMillis)
        else AcTimer.cancel(context)
    }
}
