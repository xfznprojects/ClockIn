package com.clockin.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clockin.app.ui.theme.ScanFailRed
import com.clockin.app.ui.theme.ScanSuccessGreen
import com.clockin.app.ui.theme.ScanWaitingGray

enum class ScanState {
    WAITING, SUCCESS, ERROR
}

@Composable
fun ScanIndicator(
    state: ScanState,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (state) {
            ScanState.WAITING -> ScanWaitingGray
            ScanState.SUCCESS -> ScanSuccessGreen
            ScanState.ERROR -> ScanFailRed
        },
        animationSpec = tween(durationMillis = 300),
        label = "scanColor"
    )

    // Pulse animation when waiting
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val currentScale = when (state) {
        ScanState.WAITING -> pulseScale
        ScanState.SUCCESS -> 1.1f
        ScanState.ERROR -> 0.95f
    }

    val icon = when (state) {
        ScanState.WAITING -> Icons.Default.Nfc
        ScanState.SUCCESS -> Icons.Default.Check
        ScanState.ERROR -> Icons.Default.Close
    }

    // Outer glow ring
    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow ring (only when waiting)
        if (state == ScanState.WAITING) {
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.1f,
                targetValue = 0.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowAlpha"
            )
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha))
            )
        }

        // Main circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(currentScale)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = when (state) {
                    ScanState.WAITING -> "Waiting for badge"
                    ScanState.SUCCESS -> "Scan successful"
                    ScanState.ERROR -> "Scan failed"
                },
                tint = Color.White,
                modifier = Modifier.size(72.dp)
            )
        }
    }
}
