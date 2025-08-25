package io.github.naharaoss.canvaslite.compose.panel

import android.icu.text.DecimalFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.naharaoss.canvaslite.R

@Composable
fun LayersPanel(
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    PanelScaffold(
        modifier = modifier,
        headlineContent = { Text("Layers") },
        actions = {
            IconButton(
                onClick = onAdd,
                content = { Icon(painterResource(R.drawable.add_24px), "Add layer") }
            )
        },
        content = { LazyColumn(content = content) }
    )
}

private val numberFormatter = DecimalFormat("##0%")

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LayerItem(
    modifier: Modifier = Modifier,
    headlineContent: @Composable (() -> Unit),
    selected: Boolean,
    expanded: Boolean,
    visible: Boolean,
    opacity: Float,
    blendingMode: String,
    onVisiblityChange: (Boolean) -> Unit,
    onSelect: () -> Unit,
    onExpand: () -> Unit
) {
    val spatialPosSpec = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()
    val spatialSizeSpec = MaterialTheme.motionScheme.fastSpatialSpec<IntSize>()
    val effectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

    Column(modifier) {
        ListItem(
            modifier = Modifier.combinedClickable(
                onClick = { if (selected) onExpand() else onSelect() },
                onLongClick = onExpand
            ),
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedContent(
                        targetState = selected,
                        transitionSpec = {
                            (slideInVertically(spatialPosSpec) { y -> y } + fadeIn(effectsSpec))
                                .togetherWith(slideOutVertically(spatialPosSpec) { y -> y } + fadeOut(effectsSpec))
                                .using(SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> spatialSizeSpec }))
                        }
                    ) { selected ->
                        if (selected) Row {
                            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                    headlineContent()
                }
            },
            supportingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.opacity_24px),
                        contentDescription = "Opacity"
                    )
                    Text(numberFormatter.format(opacity))
                    Text("\u2022")
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.layers_24px),
                        contentDescription = "Blending mode"
                    )
                    Text(blendingMode)
                }
            },
            leadingContent = { Icon(painterResource(R.drawable.layers_24px), null) },
            trailingContent = { Checkbox(checked = visible, onCheckedChange = onVisiblityChange) },
            tonalElevation = if (selected || expanded) 6.dp else 0.dp
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LayerBackgroundItem(
    modifier: Modifier = Modifier,
    headlineContent: @Composable (() -> Unit),
    expanded: Boolean,
    color: Color,
    onExpand: () -> Unit
) {
    val hexColor = listOf(color.red, color.green, color.blue).joinToString(
        separator = "",
        transform = { (it * 255).toInt().toString(16).uppercase().padStart(2, '0') }
    )

    Column(modifier) {
        ListItem(
            modifier = Modifier.combinedClickable(onClick = onExpand, onLongClick = onExpand),
            headlineContent = headlineContent,
            supportingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.opacity_24px),
                        contentDescription = "Opacity"
                    )
                    Text(numberFormatter.format(color.alpha))
                    Text("\u2022")
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.colors_24px),
                        contentDescription = "Color"
                    )
                    Text("#$hexColor")
                }
            },
            leadingContent = { Box(Modifier.size(24.dp).background(color, CircleShape)) },
            trailingContent = { Checkbox(checked = true, onCheckedChange = {}) },
            tonalElevation = if (expanded) 6.dp else 0.dp
        )
    }
}