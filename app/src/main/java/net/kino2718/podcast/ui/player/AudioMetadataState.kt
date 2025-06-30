package net.kino2718.podcast.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen

@Composable
fun rememberAudioMetadataState(player: Player): AudioMetadataState {
    val audioMetaDataState = remember(player) { AudioMetadataState(player) }
    LaunchedEffect(player) { audioMetaDataState.observe() }
    return audioMetaDataState
}

class AudioMetadataState(private val player: Player) {
    var metadata by mutableStateOf(player.mediaMetadata)
        private set

    suspend fun observe(): Nothing =
        player.listen { events ->
            if (events.contains(Player.EVENT_MEDIA_METADATA_CHANGED)) {
                metadata = player.mediaMetadata
            }
        }
}