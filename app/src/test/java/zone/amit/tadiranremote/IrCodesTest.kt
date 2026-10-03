package zone.amit.tadiranremote

import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IrCodesTest {
    private val codes = parseFlipperIr(File("src/main/assets/$CODES_ASSET").readText())

    @Test
    fun parsesAllSignals() {
        assertEquals(301, codes.size)
        codes.forEach { (name, signal) ->
            assertEquals(name, 38000, signal.frequency)
            assertTrue(name, signal.pattern.size % 2 == 0 && signal.pattern.all { it > 0 })
        }
    }

    @Test
    fun offStartsWithTadiranHeader() {
        val off = codes.getValue(OFF_SIGNAL).pattern
        intArrayOf(8500, 4560, 1675, 558).forEachIndexed { i, want ->
            assertTrue("Off[$i]=${off[i]}", abs(off[i] - want) <= want * 0.03)
        }
    }

    @Test
    fun everyReachableStateHasACode() {
        for (mode in Mode.entries) for (fan in Fan.entries) for (temp in MIN_TEMP..MAX_TEMP) {
            val name = AcState(power = true, mode = mode, fan = fan, temp = temp).signalName
            assertNotNull(name, codes[name])
        }
        assertNotNull(codes[AcState(power = false).signalName])
    }
}
