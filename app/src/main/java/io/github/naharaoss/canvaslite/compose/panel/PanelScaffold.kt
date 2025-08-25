package io.github.naharaoss.canvaslite.compose.panel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PanelScaffold(
    modifier: Modifier = Modifier,
    headlineContent: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    divider: Boolean = true,
    content: @Composable (ColumnScope.() -> Unit)
) {
    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(64.dp)) {
            if (headlineContent != null) Box(Modifier.padding(24.dp, 0.dp).align(Alignment.CenterStart)) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                    LocalTextStyle provides MaterialTheme.typography.labelMedium,
                    content = headlineContent
                )
            }
            if (actions != null) Row(
                modifier = Modifier.padding(10.dp, 0.dp).align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
        if (divider) HorizontalDivider()
        content()
    }
}