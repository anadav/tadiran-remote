package zone.amit.tadiranremote

import android.content.Context
import android.hardware.ConsumerIrManager

class IrSender(context: Context) {
    private val ir = context.getSystemService(ConsumerIrManager::class.java)

    val available: Boolean
        get() = ir?.hasIrEmitter() == true

    /** Sends one Tadiran frame. Blocks until done; call off the main thread. */
    fun send(frame: ByteArray) {
        checkNotNull(ir) { "No IR emitter" }
            .transmit(TadiranProtocol.FREQUENCY, TadiranProtocol.timings(frame))
    }

    fun send(state: AcState) = send(TadiranProtocol.encode(state))
}
