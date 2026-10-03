package zone.amit.tadiranremote

/**
 * Tadiran TAC 297 IR protocol: the "Amcor" format in IRremoteESP8266.
 *
 * An 8-byte frame, sent LSB-first, each bit a fixed-length mark/space pair (long mark = 1):
 *
 *   0  0x01
 *   1  fan << 4 | mode          mode: 1 Cool, 2 Heat, 3 Fan, 4 Dry, 5 Auto
 *                               fan:  1 Low, 2 Mid, 3 High, 4 Auto
 *   2  temperature << 1         °C
 *   3  0x00                     unknown (timer?)
 *   4  0x00                     unknown (timer?)
 *   5  power << 4 | x           power: 0x3 on, 0xC off; x: see [encode]
 *   6  swing (bits 6-7) | turbo (bits 0-1)
 *   7  sum of the nibbles of bytes 0-6
 *
 * Observed on a TAC 297 unit (2026-10): swing and turbo frames are accepted (the AC
 * beeps) but change nothing; flipping single bits in bytes 3-5 changes nothing visible
 * (no timer); a frame with byte 6 bits 4 and 5 both set is ignored.
 *
 * Timings are the medians of the SmartIR captures of the real remote.
 */
object TadiranProtocol {
    const val FREQUENCY = 38000
    const val FRAME_BYTES = 8

    private const val HDR_MARK = 8500
    private const val HDR_SPACE = 4500
    private const val LONG = 1700
    private const val SHORT = 650
    private const val FOOTER_MARK = 2000
    private const val GAP = 25000
    private const val FRAMES = 2

    fun encode(state: AcState): ByteArray {
        val fixedSpeedCoolHeat =
            (state.mode == Mode.Cool || state.mode == Mode.Heat) && state.fan != Fan.Auto
        val turbo = state.turbo && state.turboApplies
        val bytes = intArrayOf(
            0x01,
            fanCode(state) shl 4 or modeCode(state.mode),
            state.temp shl 1,
            0x00,
            0x00,
            // The real remote sends low nibble 0 for Cool/Heat at a fixed fan speed, else 3.
            (if (state.power) 0x30 else 0xC0) or (if (fixedSpeedCoolHeat) 0x0 else 0x3),
            (if (state.swing) 0xC0 else 0) or (if (turbo) 0x03 else 0),
            0,
        )
        return withChecksum(ByteArray(FRAME_BYTES) { bytes[it].toByte() })
    }

    /** Returns a copy of [frame] with byte 7 set to the checksum of bytes 0-6. */
    fun withChecksum(frame: ByteArray): ByteArray =
        frame.copyOf().also { it[7] = checksum(it).toByte() }

    fun checksum(frame: ByteArray): Int =
        (0 until 7).sumOf { val b = frame[it].toInt() and 0xFF; (b shr 4) + (b and 0xF) } and 0xFF

    /** Mark/space durations in µs for [ConsumerIrManager.transmit]. */
    fun timings(frame: ByteArray): IntArray {
        val out = ArrayList<Int>(FRAMES * (2 + 16 * FRAME_BYTES + 2))
        repeat(FRAMES) {
            out += HDR_MARK
            out += HDR_SPACE
            for (byte in frame) {
                for (bit in 0 until 8) {
                    val one = (byte.toInt() shr bit) and 1 == 1
                    out += if (one) LONG else SHORT
                    out += if (one) SHORT else LONG
                }
            }
            out += FOOTER_MARK
            out += GAP
        }
        return out.toIntArray()
    }

    fun hex(frame: ByteArray): String = frame.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

    private fun modeCode(mode: Mode): Int = when (mode) {
        Mode.Cool -> 1
        Mode.Heat -> 2
        Mode.Fan -> 3
        Mode.Dry -> 4
        Mode.Auto -> 5
    }

    /** The real remote sends Low in Dry mode, and in Fan mode when Auto is selected. */
    private fun fanCode(state: AcState): Int = when {
        state.mode == Mode.Dry -> 1
        state.mode == Mode.Fan && state.fan == Fan.Auto -> 1
        else -> when (state.fan) {
            Fan.Low -> 1
            Fan.Mid -> 2
            Fan.High -> 3
            Fan.Auto -> 4
        }
    }
}
