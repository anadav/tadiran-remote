package zone.amit.tadiranremote

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Bytes and bits nobody has decoded yet, which the lab lets you flip: byte index to bits. */
private val PROBE_BITS = listOf(
    3 to (7 downTo 0).toList(),
    4 to (7 downTo 0).toList(),
    5 to (3 downTo 0).toList(),
    6 to (5 downTo 2).toList(),
)

/**
 * Sends the current state with extra bits flipped in the undecoded bytes, to find the
 * timer (or sleep, etc.) by watching how the AC reacts.
 */
@Composable
fun LabScreen(vm: RemoteViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val base = TadiranProtocol.encode(vm.state)
    var masks by remember { mutableStateOf(IntArray(TadiranProtocol.FRAME_BYTES)) }
    val sent = remember { mutableStateListOf<String>() }
    val probe = TadiranProtocol.withChecksum(
        ByteArray(TadiranProtocol.FRAME_BYTES) { (base[it].toInt() xor masks[it]).toByte() }
    )

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text("Lab: timer probe", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                "Sends the remote's current state (${vm.state.describe()}) with extra bits set in " +
                    "the bytes nobody has decoded. Turn the AC on in the main screen first, then flip one bit, send, " +
                    "and watch the AC: a timer light, hours on the display, a different beep. " +
                    "\"Send normal\" undoes it. Note which bits did what.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            PROBE_BITS.forEach { (byte, bits) ->
                Text(
                    "Byte $byte",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (7 downTo 0).forEach { bit ->
                        if (bit in bits) {
                            BitToggle(
                                bit = bit,
                                value = (base[byte].toInt() xor masks[byte]) shr bit and 1,
                                flipped = masks[byte] shr bit and 1 == 1,
                                onClick = { masks = masks.copyOf().also { it[byte] = it[byte] xor (1 shl bit) } },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            FrameLine("Normal", base)
            FrameLine("Probe ", probe)

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    vm.sendProbe(probe)
                    sent.add(0, TadiranProtocol.hex(probe))
                }) { Text("Send probe") }
                OutlinedButton(onClick = vm::resend) { Text("Send normal") }
                TextButton(onClick = { masks = IntArray(TadiranProtocol.FRAME_BYTES) }) { Text("Clear") }
            }
            Text(
                vm.status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (sent.isNotEmpty()) {
                Text(
                    "Sent probes",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                sent.take(12).forEach {
                    Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** One bit of the frame: its number and current value; highlighted when flipped. */
@Composable
private fun BitToggle(bit: Int, value: Int, flipped: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (flipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (flipped) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = modifier.height(48.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("b$bit", style = MaterialTheme.typography.labelSmall)
            Text("$value", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FrameLine(label: String, frame: ByteArray) {
    Row {
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.width(64.dp))
        Text(TadiranProtocol.hex(frame), fontFamily = FontFamily.Monospace)
    }
}
