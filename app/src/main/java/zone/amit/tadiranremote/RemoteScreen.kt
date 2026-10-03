package zone.amit.tadiranremote

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/** What the remote screen can ask for; implemented by [RemoteViewModel]. */
interface RemoteActions {
    fun setMode(mode: Mode)
    fun setFan(fan: Fan)
    fun stepTemp(delta: Int)
    fun setSwing(on: Boolean)
    fun setTurbo(on: Boolean)
    fun togglePower()
    fun resend()
    fun setTimer(minutes: Int, turnOn: Boolean)
    fun cancelTimer()
}

@Composable
fun RemoteScreen(vm: RemoteViewModel, onOpenLab: () -> Unit) {
    val view = LocalView.current
    LaunchedEffect(vm.sentCount) {
        if (vm.sentCount > 0) {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                else HapticFeedbackConstants.KEYBOARD_TAP
            )
        }
    }
    RemoteContent(vm.state, vm.timer, vm.status, statusIsError = !vm.hasEmitter, vm, onOpenLab)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteContent(
    state: AcState,
    timer: AcTimer?,
    status: String,
    statusIsError: Boolean,
    actions: RemoteActions,
    onOpenLab: () -> Unit,
) {
    val idle = MaterialTheme.colorScheme.outline
    val accent by animateColorAsState(if (state.power) state.mode.color() else idle, label = "accent")
    var timerSheet by remember { mutableStateOf(false) }

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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Tadiran AC",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (state.power) "On" else "Off",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenLab) {
                        Icon(
                            Icons.Outlined.Science,
                            contentDescription = "Lab",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))
                TemperatureDial(state, accent, onStep = actions::stepTemp)
                Spacer(Modifier.weight(1f))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mode.entries.forEach { mode ->
                        ModeButton(
                            mode = mode,
                            selected = mode == state.mode,
                            accent = if (state.power) mode.color() else idle,
                            onClick = { actions.setMode(mode) },
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
                            onClick = { actions.setFan(fan) },
                            enabled = state.fanApplies && (fan != Fan.Auto || state.autoFanApplies),
                            shape = SegmentedButtonDefaults.itemShape(i, Fan.entries.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = accent.copy(alpha = 0.25f),
                            ),
                            label = { Text(fan.name) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FeatureChip("Swing", Icons.Filled.SwapVert, state.swing, enabled = true, accent) {
                        actions.setSwing(!state.swing)
                    }
                    FeatureChip("Turbo", Icons.Filled.Bolt, state.turbo && state.turboApplies, state.turboApplies, accent) {
                        actions.setTurbo(!state.turbo)
                    }
                    Text(
                        "experimental",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.weight(1f))
                if (timer != null) {
                    InputChip(
                        selected = true,
                        onClick = { timerSheet = true },
                        label = { Text(timer.describe()) },
                        leadingIcon = { Icon(Icons.Filled.Timer, null, Modifier.size(18.dp)) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Cancel timer",
                                modifier = Modifier.size(18.dp).clickable { actions.cancelTimer() },
                            )
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundIconButton(Icons.Filled.Timer, "Timer", accent) { timerSheet = true }
                    PowerButton(state.power, accent, onClick = actions::togglePower, onLongClick = actions::resend)
                    RoundIconButton(Icons.Filled.Refresh, "Resend", accent, onClick = actions::resend)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    status.ifEmpty { "Point the phone at the AC" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusIsError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (timerSheet) {
        TimerSheet(
            timer = timer,
            powerIsOn = state.power,
            onSet = { minutes, turnOn -> actions.setTimer(minutes, turnOn); timerSheet = false },
            onCancel = { actions.cancelTimer(); timerSheet = false },
            onDismiss = { timerSheet = false },
        )
    }
}

private val TIMER_PRESETS = listOf(15, 30, 60, 90, 120, 180, 240, 360, 480)

private fun formatMinutes(m: Int): String = when {
    m < 60 -> "$m min"
    m % 60 == 0 -> "${m / 60} h"
    else -> "${m / 60}.${m % 60 * 10 / 60} h"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TimerSheet(
    timer: AcTimer?,
    powerIsOn: Boolean,
    onSet: (minutes: Int, turnOn: Boolean) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    var turnOn by remember { mutableStateOf(!powerIsOn) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Timer", style = MaterialTheme.typography.titleLarge)
            Text(
                "The phone sends the code at that time, so leave it facing the AC.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(false to "Turn off", true to "Turn on").forEachIndexed { i, (on, label) ->
                    SegmentedButton(
                        selected = turnOn == on,
                        onClick = { turnOn = on },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        label = { Text(label) },
                    )
                }
            }
            Text(
                "in",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TIMER_PRESETS.forEach { m ->
                    FilledTonalButton(onClick = { onSet(m, turnOn) }) { Text(formatMinutes(m)) }
                }
            }
            if (timer != null) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onCancel) { Text("Cancel timer (${timer.describe()})") }
            }
        }
    }
}

@Composable
private fun FeatureChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent.copy(alpha = 0.25f)),
    )
}

@Composable
private fun RoundIconButton(icon: ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = accent.copy(alpha = 0.16f),
            contentColor = accent,
        ),
        modifier = Modifier.size(56.dp),
    ) {
        Icon(icon, contentDescription = label)
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
