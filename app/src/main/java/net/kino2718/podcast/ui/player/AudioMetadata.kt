package net.kino2718.podcast.ui.player

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.Util.shouldShowPlayButton
import coil.compose.AsyncImage
import net.kino2718.podcast.R
import net.kino2718.podcast.ui.utils.InteractiveLinearProgressIndicator
import net.kino2718.podcast.ui.utils.toHMS

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun AudioMetadata(
    player: Player,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metadataState = rememberAudioMetadataState(player)
    val positionState = rememberPlaybackPositionState(player)

    BoxWithConstraints(
        modifier = modifier,
    ) {
        val width = with(LocalDensity.current) { maxWidth.toPx() }
        var offset by remember { mutableFloatStateOf(0f) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            seekBySwipe(player, offset / width)
                            offset = 0f
                        },
                        onDragCancel = {
                            seekBySwipe(player, offset / width)
                            offset = 0f
                        }
                    ) { _, dragAmount ->
                        offset += dragAmount
                    }
                },
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // httpだと表示されないため
                val imageUrl = metadataState.metadata.artworkUri?.toString()
                    ?.replaceFirst("http://", "https://")
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.item_image_size))
                )

                Column(modifier = Modifier.weight(1f)) {
                    val title = metadataState.metadata.title?.toString() ?: ""
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall
                    )

                    val subTitle = metadataState.metadata.subtitle?.toString() ?: ""
                    Text(
                        text = subTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                IconButton(
                    onClick = { dismiss() },
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_button_small)),
                    enabled = shouldShowPlayButton(player) // 停止時のみdismissできる
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircle,
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_small))
                    )
                }
            }

            val duration = positionState.playbackPosition.duration
            val playbackPosition = positionState.playbackPosition.position
            val progress = if (duration != 0L)
                (playbackPosition.toFloat() / duration.toFloat())
                    .coerceAtLeast(0f).coerceAtMost(1f)
            else 0f
            InteractiveLinearProgressIndicator(
                progress = progress,
                onTappedFraction = {
                    // tap位置にseekする。
                    val newPos = (duration * it).toLong()
                        .coerceAtLeast(0L).coerceAtMost(duration - 100L)
                    player.seekTo(newPos)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 再生item番号
                val current = player.currentMediaItemIndex
                val total = player.mediaItemCount

                val itemNo = if (total <= 1) "" else "${current + 1}/$total"
                Text(
                    text = itemNo,
                    style = MaterialTheme.typography.labelSmall
                )

                Spacer(modifier = Modifier.weight(1f))

                // 再生位置/再生時間の表示
                val curPos: String
                if (positionState.playbackPosition.duration != C.TIME_UNSET) {
                    val position = (positionState.playbackPosition.position).toHMS()
                    val duration = (positionState.playbackPosition.duration).toHMS()
                    curPos = "$position/$duration"
                } else {
                    curPos = ""
                }
                Text(
                    text = curPos,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun seekBySwipe(player: Player, amount: Float) {
    // 端から端までswipeで2分とする
    val seekAmount = (amount * 2 * 60 * 1000).toLong()
    val newPosition = (player.contentPosition + seekAmount)
        .coerceAtMost(player.duration - 100L).coerceAtLeast(0L)
    player.seekTo(newPosition)
}