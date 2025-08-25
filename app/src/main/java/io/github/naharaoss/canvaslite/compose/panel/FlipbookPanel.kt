package io.github.naharaoss.canvaslite.compose.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.naharaoss.canvaslite.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlipbookPanel(
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()
    val frame by remember { derivedStateOf { state.firstVisibleItemIndex } }

    PanelScaffold(
        modifier = modifier,
        headlineContent = { Text("Flipbook") },
        actions = {
            TextButton(
                onClick = {},
                content = { Text("24 FPS") }
            )
            IconButton(
                enabled = frame != 0,
                onClick = {},
                content = { Icon(painterResource(R.drawable.stop_24px), "Stop") }
            )
            IconButton(
                onClick = {},
                content = { Icon(painterResource(R.drawable.play_24px), "Play") }
            )
            IconButton(
                onClick = {},
                content = { Icon(painterResource(R.drawable.expand_content_24px), "Expand") }
            )
        }
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                contentPadding = PaddingValues(start = 16.dp, end = maxWidth - 128.dp - 16.dp, top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                flingBehavior = rememberSnapFlingBehavior(state, SnapPosition.Start)
            ) {
                items(15) {
                    ElevatedCard(
                        modifier = Modifier.width(128.dp).aspectRatio(16 / 9f),
                        shape = MaterialTheme.shapes.large,
                        onClick = {} // TODO: Scroll to frame
                    ) {
                        Box(Modifier.fillMaxSize().background(Color.White))
                    }
                }
                item {
                    OutlinedCard(
                        modifier = Modifier.width(128.dp).aspectRatio(16 / 9f),
                        shape = MaterialTheme.shapes.large,
                        onClick = {} // TODO: New frame
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Icon(
                                modifier = Modifier.align(Alignment.Center),
                                painter = painterResource(R.drawable.add_24px),
                                contentDescription = "New frame"
                            )
                        }
                    }
                }
            }
        }
    }
}