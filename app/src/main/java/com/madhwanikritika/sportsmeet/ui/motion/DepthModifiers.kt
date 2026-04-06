package com.madhwanikritika.sportsmeet.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

/**
 * Subtle press scale + tilt — no elevation shadow (flat UI).
 */
fun Modifier.pressTilt3D(
    interactionSource: MutableInteractionSource,
    maxTiltDeg: Float = 6f,
    pressedScale: Float = 0.985f
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val tilt by animateFloatAsState(
        targetValue = if (pressed) maxTiltDeg else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tilt"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    val density = LocalDensity.current
    val cam = 14f * density.density
    graphicsLayer {
        scaleX = scale
        scaleY = scale
        rotationX = -tilt * 0.45f
        rotationY = tilt * 0.55f
        cameraDistance = cam
    }
}
