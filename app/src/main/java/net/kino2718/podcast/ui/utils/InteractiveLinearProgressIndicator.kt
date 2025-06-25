package net.kino2718.podcast.ui.utils

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun InteractiveLinearProgressIndicator(
    progress: Float,
    onTappedFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    val tappedX = offset.x.coerceIn(0f, width)
                    val fraction = tappedX / width
                    onTappedFraction(fraction)
                }
            },
        gapSize = 0.dp,
        drawStopIndicator = {}
    )
}
