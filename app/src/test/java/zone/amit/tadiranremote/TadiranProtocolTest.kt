package zone.amit.tadiranremote

import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the encoder against SmartIR's captures of the real TAC 297 remote
 * (codes/climate/1345.json, converted to Flipper format by tadiran-irdb).
 */
class TadiranProtocolTest {
    private val captures = parseFlipperIr(File("src/test/resources/smartir_1345.ir").readText())

    /** Decodes the first frame of a raw capture: long mark = 1, LSB-first. */
    private fun decode(pattern: IntArray): ByteArray = ByteArray(8) { byte ->
        var v = 0
        for (bit in 0 until 8) if (pattern[2 + 2 * (8 * byte + bit)] > 1100) v = v or (1 shl bit)
        v.toByte()
    }

    private fun stateFor(name: String, frame: ByteArray): AcState {
        // SmartIR recorded some codes with swing on (byte 6 bits 6-7); that is not in the name.
        val swing = frame[6].toInt() and 0xC0 != 0
        if (name == "Off") return AcState(power = false, mode = Mode.Cool, fan = Fan.Mid, temp = 27, swing = swing)
        val (mode, fan, temp) = name.split('_')
        return AcState(true, Mode.valueOf(mode), Fan.valueOf(fan), temp.toInt(), swing)
    }

    @Test
    fun matchesEveryGoodCapture() {
        val good = captures.filterKeys { it !in BROKEN_CAPTURES }
        assertEquals(301 - BROKEN_CAPTURES.size, good.size)
        for ((name, signal) in good) {
            val captured = decode(signal.pattern)
            assertEquals(name, TadiranProtocol.hex(captured),
                TadiranProtocol.hex(TadiranProtocol.encode(stateFor(name, captured))))
        }
    }

    @Test
    fun brokenCapturesAreReallyBroken() {
        for (name in BROKEN_CAPTURES) {
            val captured = decode(captures.getValue(name).pattern)
            assertNotEquals(name, TadiranProtocol.hex(captured),
                TadiranProtocol.hex(TadiranProtocol.encode(stateFor(name, captured))))
        }
    }

    @Test
    fun timingsRoundTrip() {
        for (mode in Mode.entries) for (fan in Fan.entries) for (temp in MIN_TEMP..MAX_TEMP)
            for (power in listOf(true, false)) for (extra in listOf(false, true)) {
                val frame = TadiranProtocol.encode(AcState(power, mode, fan, temp, swing = extra, turbo = extra))
                val pattern = TadiranProtocol.timings(frame)
                assertArrayEquals(frame, decode(pattern))
                assertTrue(pattern.size % 2 == 0 && pattern.all { it > 0 })
                assertTrue("too long for ConsumerIrManager", pattern.sum() < 2_000_000)
            }
    }

    @Test
    fun checksumIsNibbleSum() {
        val off = TadiranProtocol.encode(AcState(power = false, mode = Mode.Cool, fan = Fan.Mid, temp = 27, swing = true))
        assertEquals("01 21 36 00 00 C0 C0 25", TadiranProtocol.hex(off))
    }

    @Test
    fun turboOnlyInCoolAndHeat() {
        val cool = TadiranProtocol.encode(AcState(power = true, mode = Mode.Cool, turbo = true))
        val fan = TadiranProtocol.encode(AcState(power = true, mode = Mode.Fan, fan = Fan.Low, turbo = true))
        assertEquals(0x03, cool[6].toInt() and 0x03)
        assertEquals(0x00, fan[6].toInt() and 0x03)
    }

    companion object {
        /** Bit-shifted captures (bad checksum), and ones that send the wrong temperature. */
        val BROKEN_CAPTURES = setOf(
            "Cool_Auto_17", "Auto_Auto_20", "Fan_Mid_23",
            "Cool_High_17", "Cool_High_18", "Cool_High_28",
        )
    }
}
