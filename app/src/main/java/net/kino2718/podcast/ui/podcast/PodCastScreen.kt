package net.kino2718.podcast.ui.podcast

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PodCastScreen(
    feedUrl: String,
    modifier: Modifier = Modifier,
) {
    Scaffold { innerPadding ->
        Text(
            text = feedUrl,
            modifier = modifier.padding(innerPadding)
        )
    }
}
