package io.github.naharaoss.canvaslite.compose.page

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.naharaoss.canvaslite.R
import io.github.naharaoss.canvaslite.engine.Compatibility
import io.github.naharaoss.canvaslite.engine.Preferences
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreferencesPage(
    route: PreferencesRoute,
    scrollBehavior: TopAppBarScrollBehavior,
    preferences: Preferences,
    onPreferenceChange: (Preferences) -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onNavigate: (PreferencesRoute) -> Unit,
    onBack: () -> Unit
) {
    val compat = remember { Compatibility.lookup() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(route.submenu.title) },
                subtitle = when (route.submenu) {
                    PreferencesRoute.Submenu.Main -> null
                    else -> ({ Text("Preferences") })
                },
                navigationIcon = {
                    IconButton(onBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(Modifier
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .fillMaxHeight()
        ) {
            when (route.submenu) {
                PreferencesRoute.Submenu.Main -> {
                    Text(
                        modifier = Modifier.padding(16.dp, 8.dp),
                        text = "General",
                        style = MaterialTheme.typography.labelLarge
                    )

                    ListItemWithSwitch(
                        headlineContent = { Text("Touch drawing") },
                        supportingContent = { Text("Draw on canvas with finger") },
                        checked = preferences.general.touchDrawing,
                        onCheckedChange = { onPreferenceChange(preferences.copy(general = preferences.general.copy(touchDrawing = it))) }
                    )

                    Text(
                        modifier = Modifier.padding(16.dp, 8.dp),
                        text = "Layout",
                        style = MaterialTheme.typography.labelLarge
                    )

                    ListItemWithSwitch(
                        headlineContent = { Text("Pin pop-up panel") },
                        supportingContent = { Text("Keep pop-up panel open when tapping on canvas") },
                        checked = preferences.layout.pin,
                        onCheckedChange = { onPreferenceChange(preferences.copy(layout = preferences.layout.copy(pin = it))) }
                    )
                    ListItemWithSwitch(
                        headlineContent = { Text("Left hand mode") },
                        checked = preferences.layout.leftHand,
                        onCheckedChange = { onPreferenceChange(preferences.copy(layout = preferences.layout.copy(leftHand = it))) }
                    )

                    Text(
                        modifier = Modifier.padding(16.dp, 8.dp),
                        text = "Graphics",
                        style = MaterialTheme.typography.labelLarge
                    )

                    ListItemWithSwitch(
                        headlineContent = { Text("Low latency") },
                        supportingContent = {
                            when {
                                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> Text("Not supported on Android ${Build.VERSION.RELEASE}")
                                compat.lowLatency == Compatibility.Status.Bad -> Text("Your device is known to have graphical glitches when using low latency mode")
                                compat.lowLatency == Compatibility.Status.Good -> Text("Reduce lag between stylus and screen")
                                else -> Text("Reduce lag between stylus and screen (may have glitches)")
                            }
                        },
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                        checked = preferences.graphics.lowLatency,
                        onCheckedChange = { onPreferenceChange(preferences.copy(graphics = preferences.graphics.copy(lowLatency = it))) }
                    )

                    Text(
                        modifier = Modifier.padding(16.dp, 8.dp),
                        text = "Backup and restore",
                        style = MaterialTheme.typography.labelLarge
                    )

                    ListItem(
                        headlineContent = { Text("Save preferences") },
                        supportingContent = { Text("Save current preferences to a file") },
                        modifier = Modifier.clickable(true) { onSave() }
                    )
                    ListItem(
                        headlineContent = { Text("Load preferences") },
                        supportingContent = { Text("Load preferences from a given file") },
                        modifier = Modifier.clickable(true) { onLoad() }
                    )

                    Text(
                        modifier = Modifier.padding(16.dp, 8.dp),
                        text = "About",
                        style = MaterialTheme.typography.labelLarge
                    )

                    ListItem(
                        headlineContent = { Text("Nahara's Canvas Lite") },
                        supportingContent = { Text("v0.0.1-SNAPSHOT (git@deadbeef)") }
                    )
                    ListItem(
                        headlineContent = { Text("Compatibility report") },
                        supportingContent = { Text("Check your device's compatibility") },
                        modifier = Modifier.clickable(enabled = true) { onNavigate(PreferencesRoute(PreferencesRoute.Submenu.CompatibilityReport)) }
                    )
                }
                PreferencesRoute.Submenu.CompatibilityReport -> {
                    Text(
                        modifier = Modifier.padding(16.dp, 8.dp),
                        text = "About device",
                        style = MaterialTheme.typography.labelLarge
                    )

                    ListItem(
                        headlineContent = { Text("Android ${Build.VERSION.RELEASE}") },
                        supportingContent = { Text("SDK version ${Build.VERSION.SDK_INT}") }
                    )
                    ListItem(
                        headlineContent = { Text("Device model") },
                        supportingContent = { Text("${Build.BRAND}/${Build.MODEL}") }
                    )

                    Text(
                        modifier = Modifier.padding(16.dp, 8.dp),
                        text = "About compatibility",
                        style = MaterialTheme.typography.labelLarge
                    )

                    ListItem(
                        headlineContent = { Text("Stylus support") },
                        supportingContent = { Text(compat.stylus.name) }
                    )
                    ListItem(
                        headlineContent = { Text("Low latency graphics") },
                        supportingContent = { Text(compat.lowLatency.name) }
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ListItemWithSwitch(
    headlineContent: @Composable (() -> Unit),
    supportingContent: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = headlineContent,
        supportingContent = supportingContent,
        trailingContent = {
            Switch(
                enabled = enabled,
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier.clickable(enabled) { onCheckedChange(!checked) }
    )
}

@Serializable
data class PreferencesRoute(val submenu: Submenu) {
    enum class Submenu(val title: String) {
        Main("Preferences"),
        CompatibilityReport("Compatibility report"),
    }
}