package zone.amit.tadiranremote

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun Mode.color(): Color = when (this) {
    Mode.Cool -> Color(0xFF1E88E5)
    Mode.Heat -> Color(0xFFF4511E)
    Mode.Dry -> Color(0xFF00ACC1)
    Mode.Fan -> Color(0xFF43A047)
    Mode.Auto -> Color(0xFF8E24AA)
}

private fun Mode.icon(): ImageVector = when (this) {
    Mode.Cool -> Icons.Filled.AcUnit
    Mode.Heat -> Icons.Filled.WbSunny
    Mode.Dry -> Icons.Filled.WaterDrop
    Mode.Fan -> Icons.Filled.Air
    Mode.Auto -> Icons.Filled.Autorenew
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun RemoteScreen(vm: RemoteViewModel) {
    val view = LocalView.current
    LaunchedEffect(vm.sentCount) {
        if (vm.sentCount > 0) {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                else HapticFeedbackConstants.KEYBOARD_TAP
            )
        }
    }
    RemoteContent(
        state = vm.state,
        status = vm.status,
        statusIsError = !vm.hasEmitter,
        onMode = vm::setMode,
        onFan = vm::setFan,
        onStep = vm::stepTemp,
        onPower = vm::togglePower,
        onResend = vm::resend,
    )
}

@Composable
fun RemoteContent(
    state: AcState,
    status: String,
    statusIsError: Boolean,
    onMode: (Mode) -> Unit,
    onFan: (Fan) -> Unit,
    onStep: (Int) -> Unit,
    onPower: () -> Unit,
    onResend: () -> Unit,
) {
    val idle = MaterialTheme.colorScheme.outline
    val accent by animateColorAsState(if (state.power) state.mode.color() else idle, label = "accent")

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.18f), MaterialTheme.colorScheme.surface)
                    )
                )
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Tadiran AC",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (state.power) "On" else "Off",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.weight(1f))
                TemperatureDial(state, accent, onStep = onStep)
                Spacer(Modifier.weight(1f))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mode.entries.forEach { mode ->
                        ModeButton(
                            mode = mode,
                            selected = mode == state.mode,
                            accent = if (state.power) mode.color() else idle,
                            onClick = { onMode(mode) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    "Fan",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Fan.entries.forEachIndexed { i, fan ->
                        SegmentedButton(
                            selected = fan == state.fan && state.fanApplies,
                            onClick = { onFan(fan) },
                            enabled = state.fanApplies,
                            shape = SegmentedButtonDefaults.itemShape(i, Fan.entries.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = accent.copy(alpha = 0.25f),
                            ),
                            label = { Text(fan.name) },
                        )
                    }
                }

                Spacer(Modifier.weight(1f))
                PowerButton(state.power, accent, onClick = onPower, onLongClick = onResend)
                Spacer(Modifier.height(12.dp))
                Text(
                    status.ifEmpty { "Long-press power to resend" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusIsError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TemperatureDial(state: AcState, accent: Color, onStep: (Int) -> Unit) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fraction by animateFloatAsState(
        (state.temp - MIN_TEMP).toFloat() / (MAX_TEMP - MIN_TEMP),
        label = "dial",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(Icons.Filled.Remove, "Colder", accent, enabled = state.temp > MIN_TEMP) { onStep(-1) }
        Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                val inset = stroke.width / 2
                val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                val topLeft = Offset(inset, inset)
                drawArc(track, 135f, 270f, false, topLeft, arcSize, style = stroke)
                drawArc(accent, 135f, 270f * fraction, false, topLeft, arcSize, style = stroke)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${state.temp}°",
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    state.mode.name.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 3.sp,
                    color = accent,
                )
            }
        }
        StepButton(Icons.Filled.Add, "Warmer", accent, enabled = state.temp < MAX_TEMP) { onStep(+1) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, label: String, accent: Color, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = accent.copy(alpha = 0.16f),
            contentColor = accent,
        ),
        modifier = Modifier.size(56.dp),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun ModeButton(
    mode: Mode,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        if (selected) accent else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "mode",
    )
    val content = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = container,
        contentColor = content,
        modifier = modifier.height(72.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(mode.icon(), contentDescription = null)
            Spacer(Modifier.height(4.dp))
            Text(mode.name, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun PowerButton(on: Boolean, accent: Color, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (on) accent else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = if (on) 6.dp else 0.dp,
        modifier = Modifier.size(84.dp),
    ) {
        Box(
            Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PowerSettingsNew,
                contentDescription = if (on) "Turn off" else "Turn on",
                modifier = Modifier.size(40.dp),
            )
        }
    }
}
