package com.hayhak.currencyconverter.ui.settings

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.getFlagEmoji
import kotlin.math.roundToInt

@Composable
fun FavoriteReorderList(
    codes: List<String>,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (codes.isEmpty()) return
    Column(modifier) {
        Text(
            stringResource(R.string.favorites_drag),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        codes.forEachIndexed { index, code ->
            var offsetY by remember(code) { mutableFloatStateOf(0f) }
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .zIndex(if (offsetY != 0f) 1f else 0f)
                    .offset { IntOffset(0, offsetY.roundToInt()) }
                    .pointerInput(code, codes.size) {
                        detectDragGesturesAfterLongPress(
                            onDragEnd = {
                                val steps = (offsetY / size.height.coerceAtLeast(1).toFloat()).roundToInt()
                                if (steps != 0) onMove(index, (index + steps).coerceIn(0, codes.lastIndex))
                                offsetY = 0f
                            },
                            onDragCancel = { offsetY = 0f },
                            onDrag = { change, drag ->
                                change.consume()
                                offsetY += drag.y
                            }
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DragHandle, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(getFlagEmoji(code), modifier = Modifier.padding(end = 8.dp))
                    Text(code, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}
