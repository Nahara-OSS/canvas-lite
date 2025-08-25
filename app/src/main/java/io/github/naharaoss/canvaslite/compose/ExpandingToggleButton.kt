package io.github.naharaoss.canvaslite.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RowScope.ExpandingToggleButton(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: ToggleButtonShapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable (RowScope.() -> Unit)
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val weight by animateFloatAsState(
        targetValue = if (pressed) 1f + ButtonGroupDefaults.ExpandedRatio else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )

    ToggleButton(
        modifier = modifier.weight(weight),
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = shapes,
        interactionSource = interactionSource,
        content = content
    )
}