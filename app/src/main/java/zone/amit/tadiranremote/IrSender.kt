package zone.amit.tadiranremote

import android.content.Context
import android.hardware.ConsumerIrManager

class IrSender(context: Context) {
    private val ir = context.getSystemService(ConsumerIrManager::class.java)

    val available: Boolean
        get() = ir?.hasIrEmitter() == true

    /** Blocks until the pattern has been sent; call off the main thread. */
    fun send(signal: IrSignal) {
        checkNotNull(ir) { "No IR emitter" }.transmit(signal.frequency, signal.pattern)
    }
}
