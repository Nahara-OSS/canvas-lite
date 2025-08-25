package io.github.naharaoss.canvaslite.compose.page

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.naharaoss.canvaslite.R
import io.github.naharaoss.canvaslite.compose.dialog.CustomCanvasDialog
import io.github.naharaoss.canvaslite.compose.dialog.NewFolderDialog
import io.github.naharaoss.canvaslite.compose.TimeDisplayText
import io.github.naharaoss.canvaslite.engine.project.Canvas
import io.github.naharaoss.canvaslite.engine.project.Library
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryPageScaffold(
    modifier: Modifier = Modifier,
    route: LibraryRoute,
    onNavigateUp: () -> Unit,
    onPreferences: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onCreateCanvas: (Canvas.CanvasPreset) -> Unit,
    content: @Composable ((PaddingValues) -> Unit)
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val screenSizePreset = Canvas.CanvasPreset.ScreenSize
    var fabExpanded by remember { mutableStateOf(false) }
    var showCustomPresetDialog by rememberSaveable { mutableStateOf(false) }
    var showFolderCreateDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler(fabExpanded) { fabExpanded = false }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text("Library") },
                subtitle = {
                    AnimatedContent(
                        targetState = route.folderName,
                        transitionSpec = { (fadeIn() togetherWith fadeOut()).using(SizeTransform(clip = false)) },
                        content = { name -> Text(name) }
                    )
                },
                navigationIcon = {
                    AnimatedContent(
                        targetState = route,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                    ) { route ->
                        when (route.folderId) {
                            null -> {}
                            else -> IconButton(onNavigateUp) { Icon(painterResource(R.drawable.arrow_back_24px), "Go back") }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onPreferences,
                        content = { Icon(painterResource(R.drawable.settings_24px), "Preferences") }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButtonMenu(
                expanded = fabExpanded,
                button = {
                    ToggleFloatingActionButton(
                        checked = fabExpanded,
                        onCheckedChange = { fabExpanded = it }
                    ) {
                        Icon(
                            modifier = Modifier.rotate(checkedProgress * 45f).animateIcon({ checkedProgress }),
                            painter = painterResource(R.drawable.add_24px),
                            contentDescription = "New"
                        )
                    }
                }
            ) {
                FloatingActionButtonMenuItem(
                    onClick = {
                        showFolderCreateDialog = true
                        fabExpanded = false
                    },
                    text = { Text("Folder") },
                    icon = { Icon(painterResource(R.drawable.folder_24px), "Folder") }
                )
                Spacer(Modifier.size(8.dp))
                FloatingActionButtonMenuItem(
                    onClick = {
                        showCustomPresetDialog = true
                        fabExpanded = false
                    },
                    text = { Text("Custom canvas") },
                    icon = { Icon(painterResource(R.drawable.tune_24px), "Custom canvas") }
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        onCreateCanvas(Canvas.CanvasPreset.InfiniteCanvas)
                        fabExpanded = false
                    },
                    text = { Text("Infinite canvas") },
                    icon = { Icon(painterResource(R.drawable.all_inclusive_24px), "Infinite canvas") }
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        onCreateCanvas(screenSizePreset)
                        fabExpanded = false
                    },
                    text = { Text("Screen size") },
                    icon = { Icon(painterResource(R.drawable.wall_art_24px), "Screen size") }
                )
                // TODO: Canvas presets
            }
        }
    ) { innerPadding ->
        content(innerPadding)

        if (showCustomPresetDialog) CustomCanvasDialog(
            initialPreset = screenSizePreset,
            onDismissRequest = { showCustomPresetDialog = false },
            onCreate = {
                onCreateCanvas(it)
                showCustomPresetDialog = false
            }
        )

        if (showFolderCreateDialog) NewFolderDialog(
            onDismissRequest = { showFolderCreateDialog = false },
            onCreate = {
                onCreateFolder(it)
                showFolderCreateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryPageContent(
    modifier: Modifier = Modifier,
    empty: Boolean,
    loading: Boolean,
    innerPadding: PaddingValues,
    content: LazyStaggeredGridScope.() -> Unit
) {
    Box(modifier.padding(innerPadding)) {
        LazyVerticalStaggeredGrid(
            modifier = Modifier.fillMaxSize(),
            columns = StaggeredGridCells.Adaptive(180.dp),
            contentPadding = PaddingValues(16.dp, 16.dp),
            verticalItemSpacing = 16.dp,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (empty) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        Column(
                            modifier = Modifier.padding(0.dp, 96.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                modifier = Modifier.size(96.dp),
                                painter = painterResource(R.drawable.folder_24px),
                                contentDescription = null
                            )
                            Text("Umm... nothing here!")
                        }
                    }
                }
            } else {
                content()
            }
        }

        AnimatedContent(
            modifier = Modifier.align(Alignment.Center),
            targetState = loading,
            content = { loading -> if (loading) LoadingIndicator(Modifier.align(Alignment.Center)) }
        )
    }
}

@Serializable
data class LibraryRoute(val folderId: String?, val folderName: String)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    name: @Composable (() -> Unit),
    supportingContent: @Composable (ColumnScope.() -> Unit),
    content: @Composable (BoxScope.() -> Unit)
) {
    ElevatedCard(modifier = modifier, onClick = onClick) {
        Column {
            Box(modifier = Modifier.fillMaxWidth(), content = content)
            Column(Modifier.padding(16.dp)) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.headlineSmall,
                    content = name
                )
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodySmall,
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                    content = { Column(content = supportingContent) }
                )
            }
        }
    }
}