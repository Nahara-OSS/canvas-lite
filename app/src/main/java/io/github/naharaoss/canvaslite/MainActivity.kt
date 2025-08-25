package io.github.naharaoss.canvaslite

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.viewModels
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.plus
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.naharaoss.canvaslite.compose.page.LibraryPageContent
import io.github.naharaoss.canvaslite.compose.page.LibraryPageScaffold
import io.github.naharaoss.canvaslite.compose.page.LibraryRoute
import io.github.naharaoss.canvaslite.compose.Overlay
import io.github.naharaoss.canvaslite.compose.OverlayState
import io.github.naharaoss.canvaslite.compose.TimeDisplayText
import io.github.naharaoss.canvaslite.compose.page.LibraryItem
import io.github.naharaoss.canvaslite.compose.page.PreferencesPage
import io.github.naharaoss.canvaslite.compose.page.PreferencesRoute
import io.github.naharaoss.canvaslite.compose.panel.LayerBackgroundItem
import io.github.naharaoss.canvaslite.compose.panel.LayerItem
import io.github.naharaoss.canvaslite.engine.project.Canvas
import io.github.naharaoss.canvaslite.engine.project.Layer
import io.github.naharaoss.canvaslite.engine.project.Library
import io.github.naharaoss.canvaslite.model.CanvasViewModel
import io.github.naharaoss.canvaslite.model.FolderViewModel
import io.github.naharaoss.canvaslite.model.LibraryViewModel
import io.github.naharaoss.canvaslite.model.PreferencesViewModel
import io.github.naharaoss.canvaslite.ui.theme.NaharasCanvasLiteTheme
import io.github.naharaoss.canvaslite.view.DrawingCanvasView
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val preferencesViewModel: PreferencesViewModel by viewModels()
        val savePrefs = registerForActivityResult(CreateDocument("application/json")) { preferencesViewModel.saveTo(it) }
        val loadPrefs = registerForActivityResult(OpenDocument()) { preferencesViewModel.loadFrom(it) }

        setContent {
            val preferences by preferencesViewModel.preferences.collectAsState()
            val navController = rememberNavController()

            var color by remember { mutableStateOf(Color.Black) }

            val effectSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
            val spatialSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
            val spatialOffsetSpec = MaterialTheme.motionScheme.slowSpatialSpec<IntOffset>()

            NaharasCanvasLiteTheme {
                Surface(Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = LibraryPage,
                        enterTransition = { fadeIn(effectSpec) + scaleIn(spatialSpec, 0.8f) },
                        exitTransition = { fadeOut(effectSpec) + scaleOut(spatialSpec, 1.2f) },
                        popEnterTransition = { fadeIn(effectSpec) + scaleIn(spatialSpec, 1.2f) },
                        popExitTransition = { fadeOut(effectSpec) + scaleOut(spatialSpec, 0.8f) }
                    ) {
                        composable<LibraryPage> {
                            val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
                            val libraryNavController = rememberNavController()
                            val navEntry by libraryNavController.currentBackStackEntryAsState()

                            LibraryPageScaffold(
                                route = navEntry?.toRoute() ?: LibraryRoute(null, "Home"),
                                onNavigateUp = { libraryNavController.navigateUp() },
                                onPreferences = { navController.navigate(PreferencesRoute(PreferencesRoute.Submenu.Main)) },
                                onCreateFolder = { name ->
                                    val parentId = libraryNavController.getBackStackEntry<LibraryRoute>().toRoute<LibraryRoute>().folderId
                                    libraryViewModel.createFolder(parentId, name) { libraryNavController.navigate(LibraryRoute(it.libraryId, name)) }
                                },
                                onCreateCanvas = { preset ->
                                    val parentId = libraryNavController.getBackStackEntry<LibraryRoute>().toRoute<LibraryRoute>().folderId
                                    libraryViewModel.createCanvas(parentId, preset) { navController.navigate(CanvasPage(it.libraryId)) }
                                },
                            ) { innerPadding ->
                                NavHost(
                                    navController = libraryNavController,
                                    startDestination = LibraryRoute(null, "Home"),
                                    enterTransition = { fadeIn(effectSpec) + slideInHorizontally(spatialOffsetSpec) { x -> x } },
                                    exitTransition = { fadeOut(effectSpec) + slideOutHorizontally(spatialOffsetSpec) { x -> -x } },
                                    popEnterTransition = { fadeIn(effectSpec) + slideInHorizontally(spatialOffsetSpec) { x -> -x } },
                                    popExitTransition = { fadeOut(effectSpec) + slideOutHorizontally(spatialOffsetSpec) { x -> x } }
                                ) {
                                    composable<LibraryRoute> { entry ->
                                        val route = entry.toRoute<LibraryRoute>()
                                        val folderViewModel = viewModel { FolderViewModel(libraryViewModel.library, route.folderId) }
                                        val items by folderViewModel.items.collectAsState()

                                        LaunchedEffect(Unit) { folderViewModel.refresh() }

                                        LibraryPageContent(
                                            empty = items?.isEmpty() ?: false,
                                            loading = items == null,
                                            innerPadding = innerPadding
                                        ) {
                                            val items = items
                                            if (items != null) items(items, { it.libraryId }) { item ->
                                                LibraryItem(
                                                    onClick = {
                                                        when (item.type) {
                                                            Library.ItemType.Canvas -> navController.navigate(CanvasPage(item.libraryId))
                                                            Library.ItemType.Folder -> libraryNavController.navigate(LibraryRoute(item.libraryId, item.metadata.name))
                                                        }
                                                    },
                                                    name = { Text(item.metadata.name) },
                                                    supportingContent = {
                                                        when {
                                                            item.type == Library.ItemType.Folder -> Text("Folder")
                                                            item.type == Library.ItemType.Canvas && item.metadata.canvasSize == null -> Text("Infinite canvas")
                                                            item.type == Library.ItemType.Canvas && item.metadata.canvasSize != null -> Text(item.metadata.canvasSize.toString())
                                                            else -> item.type.toString()
                                                        }

                                                        TimeDisplayText(item.metadata.lastModified)
                                                    }
                                                ) {
                                                    if (item.type == Library.ItemType.Canvas) {
                                                        var thumbnail: ImageBitmap? by remember { mutableStateOf(null) }

                                                        LaunchedEffect(item.libraryId) {
                                                            thumbnail = folderViewModel.library.loadThumbnail(item.libraryId)
                                                            Log.d("MainActivity", "Thumbnail is $thumbnail")
                                                        }

                                                        Box(Modifier
                                                            .aspectRatio(item.metadata.canvasSize?.let { it.width / it.height.toFloat() } ?: 1f)
                                                            .background(Color.White, MaterialTheme.shapes.medium)) {
                                                            val thumbnail = thumbnail

                                                            if (thumbnail != null) {
                                                                Log.d("MainActivity", "Recompose with $thumbnail")
                                                                Image(
                                                                    modifier = Modifier.fillMaxSize(),
                                                                    bitmap = thumbnail,
                                                                    contentDescription = null
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        composable<CanvasPage> { entry ->
                            val route = entry.toRoute<CanvasPage>()
                            val context = LocalContext.current
                            val canvasViewModel: CanvasViewModel = viewModel(
                                factory = CanvasViewModel.Factory,
                                extras = defaultViewModelCreationExtras + MutableCreationExtras().also {
                                    it[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] = application
                                    it[CanvasViewModel.LIBRARY_ID_KEY] = route.libraryId
                                }
                            )
                            val canvas by canvasViewModel.canvas.collectAsState()
                            val selectedLayer by canvasViewModel.currentLayer.collectAsState()
                            val allLayers by canvasViewModel.allLayers.collectAsState()
                            var overlayState by rememberSaveable { mutableStateOf(OverlayState()) }

                            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                Box {
                                    DrawingCanvas(
                                        modifier = Modifier.fillMaxSize(),
                                        touchDrawing = preferences.general.touchDrawing,
                                        lowLatency = preferences.graphics.lowLatency,
                                        canvas = canvas,
                                        selectedLayer = selectedLayer,
                                        brushColor = color,
                                        eyedropper = false,
                                        onCanvasTouch = { if (!preferences.layout.pin) overlayState = overlayState.asAllClosed },
                                        onEyedropper = { color = it }
                                    )
                                    Overlay(
                                        modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxSize(),
                                        state = overlayState,
                                        pickedColor = color,
                                        lastColor = Color.White,
                                        layers = {
                                            allLayers.fastForEachReversed { layer ->
                                                item {
                                                    LayerItem(
                                                        headlineContent = { Text(layer.name) },
                                                        visible = true,
                                                        selected = selectedLayer == layer,
                                                        expanded = false,
                                                        opacity = layer.opacity,
                                                        blendingMode = "Normal",
                                                        onVisiblityChange = {},
                                                        onSelect = { canvasViewModel.selectLayer(layer) },
                                                        onExpand = {}
                                                    )
                                                }
                                            }
                                            item {
                                                LayerBackgroundItem(
                                                    headlineContent = { Text("Background") },
                                                    expanded = false,
                                                    color = Color.White,
                                                    onExpand = {}
                                                )
                                            }
                                        },
                                        onStateChange = { overlayState = it },
                                        onNavigateUp = { navController.navigateUp() },
                                        onPickColor = { color = it },
                                        onAddLayer = {
                                            val layer = canvasViewModel.addLayer()
                                            if (layer != null) canvasViewModel.selectLayer(layer)
                                        },
                                        onPreferences = { navController.navigate(PreferencesRoute(PreferencesRoute.Submenu.Main)) },
                                        onExport = {
                                            canvasViewModel.testExport { file ->
                                                val uri = FileProvider.getUriForFile(context, "io.github.naharaoss.canvaslite", file)
                                                val intent = Intent(Intent.ACTION_EDIT, uri).apply {
                                                    component = ComponentName.createRelative("org.krita", ".android.MainActivity")
                                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                                }
                                                startActivity(intent)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        composable<PreferencesRoute> { entry ->
                            val route = entry.toRoute<PreferencesRoute>()
                            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                            PreferencesPage(
                                route = route,
                                scrollBehavior = scrollBehavior,
                                preferences = preferences,
                                onPreferenceChange = { preferencesViewModel.updatePreference(it) },
                                onSave = { savePrefs.launch("nahara-canvas-lite-prefs.json") },
                                onLoad = { loadPrefs.launch(arrayOf("application/json")) },
                                onNavigate = { navController.navigate(it) },
                                onBack = { navController.navigateUp() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Serializable private object LibraryPage
@Serializable private data class CanvasPage(val libraryId: String)

@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    touchDrawing: Boolean,
    lowLatency: Boolean,
    canvas: Canvas?,
    selectedLayer: Layer?,
    brushColor: Color,
    eyedropper: Boolean,
    onCanvasTouch: () -> Unit,
    onEyedropper: (Color) -> Unit
) {
    val lowLatency = lowLatency && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val background = MaterialTheme.colorScheme.surfaceDim

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                object : DrawingCanvasView(it) {
                    @SuppressLint("ClickableViewAccessibility")
                    override fun onTouchEvent(event: MotionEvent?): Boolean {
                        if (event != null && event.action == MotionEvent.ACTION_DOWN) onCanvasTouch()
                        return super.onTouchEvent(event)
                    }
                }
            }
        ) {
            it.background = background
            it.touchDrawing = touchDrawing
            it.canvas = canvas
            it.selectedLayer = selectedLayer
            it.brushColor = brushColor
        }
    }
}