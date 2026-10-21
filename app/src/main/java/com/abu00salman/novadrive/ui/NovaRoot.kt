package com.abu00salman.novadrive.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abu00salman.novadrive.core.SunCalc
import com.abu00salman.novadrive.core.ThemeMode
import com.abu00salman.novadrive.nova
import com.abu00salman.novadrive.ui.apps.AppDrawer
import com.abu00salman.novadrive.ui.components.Pill
import com.abu00salman.novadrive.ui.home.HomeScreen
import com.abu00salman.novadrive.ui.home.SplitSheet
import com.abu00salman.novadrive.ui.quick.QuickPanel
import com.abu00salman.novadrive.ui.settings.SettingsScreen
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaTheme
import com.abu00salman.novadrive.ui.theme.tr
import kotlinx.coroutines.delay

enum class Screen { HOME, APPS, SETTINGS }

private const val DEFAULT_LAT = 16.89   // used only until the first GPS fix
private const val DEFAULT_LON = 42.55

@Composable
fun NovaRoot(onVoice: () -> Unit) {
    val ctx = LocalContext.current
    val c = remember { ctx.nova }
    val settings by c.settings.state.collectAsStateWithLifecycle()
    val coarse by c.coarseLocation.collectAsStateWithLifecycle()
    val fixState = c.fix.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()

    // Driving: derived so speed updates don't recompose the whole tree.
    val drivingRaw by remember { derivedStateOf { (fixState.value?.speedKmh ?: 0f) > 8f } }
    val driving = drivingRaw && settings.drivingMode

    val minute by produceState(System.currentTimeMillis()) {
        while (true) { delay(60_000); value = System.currentTimeMillis() }
    }
    val place = coarse ?: c.settings.lastLocation() ?: Pair(DEFAULT_LAT, DEFAULT_LON)
    val dark = when (settings.themeMode) {
        ThemeMode.AUTO -> SunCalc.isNight(place.first, place.second, minute)
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    // Persist location + refresh weather when the coarse position changes.
    LaunchedEffect(coarse, settings.showWeather) {
        val loc = coarse ?: c.settings.lastLocation()
        coarse?.let { c.settings.saveLastLocation(it.first, it.second) }
        if (loc != null && settings.showWeather && !c.safeMode) c.weather.maybeRefresh(loc.first, loc.second)
    }
    // Resume the OBD link if the profile has it enabled.
    LaunchedEffect(settings.obdEnabled, settings.obdAddress) {
        val addr = settings.obdAddress
        if (settings.obdEnabled && addr != null && !c.safeMode) c.obd.start(addr) else c.obd.stop()
    }

    val locationPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        c.refreshPermissions()
    }
    LaunchedEffect(Unit) {
        if (!c.location.hasPermission()) {
            locationPerm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    NovaTheme(dark = dark, accentIndex = settings.accent, arabic = settings.arabic) {
        val p = LocalNova.current
        var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
        var quick by remember { mutableStateOf(false) }
        var split by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            c.homePressed.collect { screen = Screen.HOME; quick = false; split = false }
        }
        // A launcher never "exits" on back.
        BackHandler {
            when {
                quick -> quick = false
                split -> split = false
                screen != Screen.HOME -> screen = Screen.HOME
            }
        }

        Box(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(p.bgTop, p.bgBottom)))
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            HomeScreen(
                c = c, settings = settings, driving = driving,
                onApps = { screen = Screen.APPS },
                onSettings = { screen = Screen.SETTINGS },
                onQuick = { quick = true },
                onSplit = { split = true },
                onVoice = onVoice,
                onRequestLocation = {
                    locationPerm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                },
            )
            AnimatedVisibility(
                screen == Screen.APPS,
                enter = fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.96f),
                exit = fadeOut(tween(180)) + scaleOut(tween(200), targetScale = 0.98f),
            ) { AppDrawer(c, settings, driving) { screen = Screen.HOME } }
            AnimatedVisibility(
                screen == Screen.SETTINGS,
                enter = fadeIn(tween(220)) + slideInVertically(tween(280)) { it / 12 },
                exit = fadeOut(tween(180)),
            ) { SettingsScreen(c, settings, driving) { screen = Screen.HOME } }
            AnimatedVisibility(
                quick,
                enter = fadeIn(tween(180)) + slideInVertically(tween(260)) { -it / 3 },
                exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { -it / 3 },
            ) { QuickPanel(c, settings, onClose = { quick = false }, onSettings = { quick = false; screen = Screen.SETTINGS }) }
            AnimatedVisibility(split, enter = fadeIn(tween(200)), exit = fadeOut(tween(160))) {
                SplitSheet(c, settings) { split = false }
            }
            if (c.safeMode && screen == Screen.HOME) {
                Pill(
                    tr("Safe mode — tap for options", "الوضع الآمن — اضغط للخيارات"), p.danger,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 112.dp)
                ) { screen = Screen.SETTINGS }
            }
        }
    }
}
