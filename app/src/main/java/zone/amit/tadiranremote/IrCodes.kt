package zone.amit.tadiranremote

/** A raw IR signal from a Flipper .ir file: carrier frequency and mark/space durations in µs. */
class IrSignal(val frequency: Int, val pattern: IntArray)

/**
 * Parses the raw signals of a Flipper "IR signals file" into a name -> signal map.
 * Only `type: raw` signals are supported; that is all tadiran-irdb contains.
 */
fun parseFlipperIr(text: String): Map<String, IrSignal> {
    val signals = LinkedHashMap<String, IrSignal>()
    var name: String? = null
    var frequency = 38000
    for (line in text.lineSequence()) {
        val key = line.substringBefore(':').trim()
        val value = line.substringAfter(':', "").trim()
        when (key) {
            "name" -> {
                name = value
                frequency = 38000
            }
            "frequency" -> frequency = value.toInt()
            "data" -> {
                val pattern = value.split(' ').filter { it.isNotEmpty() }.map { it.toInt() }
                signals[requireNotNull(name) { "data line before any name" }] =
                    IrSignal(frequency, pattern.toIntArray())
                name = null
            }
        }
    }
    return signals
}
