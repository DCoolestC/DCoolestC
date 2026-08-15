package com.isokovibe.musicplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isokovibe.musicplayer.data.Song
import com.isokovibe.musicplayer.ui.components.AlbumArt
import kotlin.math.roundToInt

/** Every queue row is exactly this tall, which is what makes the
 *  drag-to-reorder index maths below exact instead of a measurement guess. */
private val QUEUE_ROW_HEIGHT = 56.dp

/**
 * The "Up next" list: tap to jump, long-press and drag to reorder, × to
 * remove, and a shortcut to keep the whole queue as a playlist.
 *
 * Reordering is done by hand rather than with a drag-and-drop library so
 * the app doesn't take another dependency for one screen. The trick that
 * keeps it honest is the fixed [QUEUE_ROW_HEIGHT]: with uniform rows, the
 * target index is just the drag distance divided by the row height, so
 * there's no need to measure or track individual item bounds.
 */
@Composable
fun QueueSheet(
    queue: List<Song>,
    currentIndex: Int,
    onItemClick: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Int) -> Unit,
    onSaveAsPlaylist: () -> Unit
) {
    val listState = rememberLazyListState()
    val rowHeightPx = with(LocalDensity.current) { QUEUE_ROW_HEIGHT.toPx() }

    // Index being dragged, and how far it's moved. Kept local to the sheet:
    // the reorder is only committed to the player on drag end, so a dragged
    // finger never causes a burst of queue mutations.
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    fun targetIndexFor(index: Int): Int =
        (index + (dragOffsetPx / rowHeightPx).roundToInt()).coerceIn(0, queue.lastIndex.coerceAtLeast(0))

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Up next", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onSaveAsPlaylist, enabled = queue.isNotEmpty()) {
                Icon(Icons.Filled.PlaylistAdd, contentDescription = null)
                Text(" Save as playlist")
            }
        }

        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            items(queue.size) { index ->
                val song = queue[index]
                val isDragging = index == draggingIndex
                val isCurrent = index == currentIndex

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(QUEUE_ROW_HEIGHT)
                        // The dragged row floats above its neighbours and
                        // follows the finger; everything else stays put until
                        // the move is committed on release.
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffsetPx else 0f
                            shadowElevation = if (isDragging) 8f else 0f
                        }
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { onItemClick(index) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArt(uri = song.albumArtUri, modifier = Modifier.size(36.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                            // Tracks already behind the playhead are dimmed so
                            // "up next" actually reads as what's coming.
                            .alpha(if (index < currentIndex) 0.5f else 1f)
                    ) {
                        Text(
                            song.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { onRemove(index) }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove from queue",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .pointerInput(index, queue.size) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingIndex = index
                                        dragOffsetPx = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetPx += dragAmount.y
                                    },
                                    onDragEnd = {
                                        val target = targetIndexFor(index)
                                        if (target != index) onMove(index, target)
                                        draggingIndex = -1
                                        dragOffsetPx = 0f
                                    },
                                    onDragCancel = {
                                        draggingIndex = -1
                                        dragOffsetPx = 0f
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
