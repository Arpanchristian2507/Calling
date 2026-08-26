package dev.arpan.calling.presentation

import android.os.Handler
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.arpan.calling.FakeCallRequest
import dev.arpan.calling.R
import dev.arpan.calling.WearLink
import dev.arpan.calling.presentation.theme.CallingTheme
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CallingTheme {
                TriggerScreen()
            }
        }
    }
}

private enum class SendState {
    Idle,
    Sending,
    Calling,
    NoPhoneConnected,
    AppMissing,
    Failed,
}

private data class CallerPreset(
    val label: String,
    val callerName: String,
)

@Composable
private fun TriggerScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val resetHandler = remember { Handler(Looper.getMainLooper()) }
    val callerPresets =
        remember {
            listOf(
                CallerPreset("Boss", "Boss"),
                CallerPreset("Mom", "Mom"),
                CallerPreset("Bro", "Brother"),
                CallerPreset("Alex", "Alex"),
                CallerPreset("Client", "Client"),
            )
        }
    var selectedCaller by remember { mutableStateOf(callerPresets.first()) }
    var sendState by remember { mutableStateOf(SendState.Idle) }

    DisposableEffect(Unit) {
        onDispose {
            resetHandler.removeCallbacksAndMessages(null)
        }
    }

    val gradient =
        remember {
            Brush.verticalGradient(
                colors =
                    listOf(
                        Color(0xFF101808),
                        Color(0xFF060805),
                    ),
            )
        }

    AppScaffold(
        modifier = Modifier.fillMaxSize(),
        timeText = {},
        containerColor = Color.Transparent,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(gradient)
                    .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.watch_brand_line),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 3.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.watch_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.watch_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                CallerPicker(
                    presets = callerPresets,
                    selectedCaller = selectedCaller,
                    onSelect = { selectedCaller = it },
                )
                Spacer(modifier = Modifier.height(22.dp))
                PulseRingTrigger(
                    enabled = sendState != SendState.Sending,
                    onTrigger = {
                        if (sendState == SendState.Sending) return@PulseRingTrigger
                        resetHandler.removeCallbacksAndMessages(null)
                        sendState = SendState.Sending
                        triggerPhoneCall(
                            context = context,
                            callerName = selectedCaller.callerName,
                            onSuccess = {
                                pulseSuccessHaptics(context, view)
                                sendState = SendState.Calling
                                scheduleReset(resetHandler) { sendState = SendState.Idle }
                            },
                            onFailure = { failedState ->
                                sendState = failedState
                                scheduleReset(resetHandler) { sendState = SendState.Idle }
                            },
                        )
                    },
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = statusLabel(sendState),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CallerPicker(
    presets: List<CallerPreset>,
    selectedCaller: CallerPreset,
    onSelect: (CallerPreset) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.watch_caller_label, selectedCaller.callerName),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            presets.forEach { preset ->
                val isSelected = preset == selectedCaller
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            )
                            .border(
                                width = 1.dp,
                                color =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                shape = RoundedCornerShape(50),
                            )
                            .clickable { onSelect(preset) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun statusLabel(state: SendState): String {
    return when (state) {
        SendState.Idle -> stringResource(R.string.watch_status_ready)
        SendState.Sending -> stringResource(R.string.watch_status_sending)
        SendState.Calling -> stringResource(R.string.watch_status_sent)
        SendState.NoPhoneConnected -> stringResource(R.string.watch_status_no_phone)
        SendState.AppMissing -> stringResource(R.string.watch_status_app_missing)
        SendState.Failed -> stringResource(R.string.watch_status_failed)
    }
}

@Composable
private fun PulseRingTrigger(enabled: Boolean, onTrigger: () -> Unit) {
    val ringColor = MaterialTheme.colorScheme.primary
    val core = MaterialTheme.colorScheme.surfaceContainerHigh
    val halo = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)

    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulseScale",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val size = (148f * pulse).dp

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(168.dp)) {
        Box(
            modifier =
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(halo),
        )
        Box(
            modifier =
                Modifier
                    .size(148.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush =
                            Brush.sweepGradient(
                                colors =
                                    listOf(
                                        ringColor,
                                        MaterialTheme.colorScheme.secondary,
                                        ringColor,
                                    ),
                            ),
                        shape = CircleShape,
                    )
                    .background(core)
                    .clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onTrigger,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.watch_trigger_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun pulseSuccessHaptics(context: android.content.Context, view: android.view.View) {
    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    if (vibrator?.hasVibrator() != true) return
    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
}

private sealed interface PhoneTargetResult {
    data object NoPhoneConnected : PhoneTargetResult
    data object AppMissing : PhoneTargetResult
    data class Target(val node: Node) : PhoneTargetResult
}

private fun triggerPhoneCall(
    context: android.content.Context,
    callerName: String,
    onSuccess: () -> Unit,
    onFailure: (SendState) -> Unit,
) {
    val payload =
        FakeCallRequest(
            callerName = callerName,
            delaySeconds = 0,
        ).encode()

    resolvePhoneTarget(
        context = context,
        onResult = { targetResult ->
            when (targetResult) {
                PhoneTargetResult.NoPhoneConnected -> onFailure(SendState.NoPhoneConnected)
                PhoneTargetResult.AppMissing -> onFailure(SendState.AppMissing)
                is PhoneTargetResult.Target -> {
                    Wearable.getMessageClient(context)
                        .sendMessage(
                            targetResult.node.id,
                            WearLink.MESSAGE_PATH_TRIGGER,
                            payload,
                        )
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(SendState.Failed) }
                }
            }
        },
    )
}

private fun resolvePhoneTarget(
    context: android.content.Context,
    onResult: (PhoneTargetResult) -> Unit,
) {
    val nodeClient = Wearable.getNodeClient(context)
    val capabilityClient = Wearable.getCapabilityClient(context)

    nodeClient.connectedNodes
        .addOnSuccessListener { connectedNodes ->
            if (connectedNodes.isEmpty()) {
                onResult(PhoneTargetResult.NoPhoneConnected)
                return@addOnSuccessListener
            }

            capabilityClient
                .getCapability(WearLink.CAPABILITY_PHONE, CapabilityClient.FILTER_ALL)
                .addOnSuccessListener { capabilityInfo ->
                    val capabilityIds = capabilityInfo.nodes.map { it.id }.toSet()
                    val connectedTarget =
                        connectedNodes
                            .sortedByDescending { it.isNearby }
                            .firstOrNull { it.id in capabilityIds }

                    if (connectedTarget != null) {
                        onResult(PhoneTargetResult.Target(connectedTarget))
                    } else {
                        onResult(PhoneTargetResult.AppMissing)
                    }
                }
                .addOnFailureListener {
                    onResult(PhoneTargetResult.AppMissing)
                }
        }
        .addOnFailureListener {
            onResult(PhoneTargetResult.NoPhoneConnected)
        }
}

private fun scheduleReset(
    handler: Handler,
    reset: () -> Unit,
) {
    handler.postDelayed(reset, 1800L)
}
