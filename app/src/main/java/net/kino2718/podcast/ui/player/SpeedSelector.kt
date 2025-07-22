package net.kino2718.podcast.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import net.kino2718.podcast.R

@Composable
fun SpeedSelector(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = remember { mutableStateOf(false) }
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    Box(
        modifier = modifier
            .background(Color.Gray.copy(alpha = 0.1f), CircleShape)
            .clickable { expanded.value = true }
            .padding(horizontal = dimensionResource(R.dimen.padding_small), vertical = dimensionResource(R.dimen.padding_extra_small)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "${speed}x", style = MaterialTheme.typography.labelLarge)
        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            speeds.forEach { s ->
                DropdownMenuItem(text = { Text("${s}x") }, onClick = {
                    expanded.value = false
                    onSpeedChange(s)
                })
            }
        }
    }
}
