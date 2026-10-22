package com.abu00salman.novadrive.ui

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.core.CarPaints
import com.abu00salman.novadrive.core.NovaSettings
import com.abu00salman.novadrive.core.SunCalc
import com.abu00salman.novadrive.core.ThemeMode
import com.abu00salman.novadrive.core.carBrand
import com.abu00salman.novadrive.core.carModel
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.data.AppsRepository
import com.abu00salman.novadrive.data.MediaRepository
import com.abu00salman.novadrive.data.NavInfo
import com.abu00salman.novadrive.data.NovaNotificationListener
import com.abu00salman.novadrive.nova
import com.abu00salman.novadrive.ui.apps.AppsScreen
import com.abu00salman.novadrive.ui.components.Pill
import com.abu00salman.novadrive.ui.home.HomeActions
import com.abu00salman.novadrive.ui.home.HomeModeViewModel
import com.abu00salman.novadrive.core.HomeMode
import com.abu00salman.novadrive.core.VIDEO_SLOTS
import com.abu00salman.novadrive.core.isVideoApp
import com.abu00salman.novadrive.core.replaceDockEntry
import com.abu00salman.novadrive.ui.components.AppPickerDialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.map
import com.abu00salman.novadrive.ui.home.DockSlot
import com.abu00salman.novadrive.ui.home.HomeScreen
import com.abu00salman.novadrive.ui.home.Shortcuts
import com.abu00salman.novadrive.ui.home.SplitSheet
import com.abu00salman.novadrive.ui.home.rememberShortcuts
import com.abu00salman.novadrive.ui.music.MusicScreen
import com.abu00salman.novadrive.ui.nav.NavigationScreen
import com.abu00salman.novadrive.ui.quick.QuickPanel
import com.abu00salman.novadrive.ui.settings.SettingsScreen
import com.abu00salman.novadrive.ui.shell.GpsState
import com.abu00salman.novadrive.ui.shell.Rail
import com.abu00salman.novadrive.ui.shell.Screen
import com.abu00salman.novadrive.ui.shell.ShellLayout
import com.abu00salman.novadrive.ui.shell.TopBar
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaPalette
import com.abu00salman.novadrive.ui.theme.NovaTheme
import com.abu00salman.novadrive.ui.theme.tr
import com.abu00salman.novadrive.ui.vehicle.VehicleScreen
import kotlinx.coroutines.delay

private const val DEFAULT_LAT = 16.89   // used only until the first GPS fix
private const val DEFAULT_LON = 42.55

/** Deep-navy backdrop with two soft, static light pools (no animation, no blur). */
fun Modifier.novaBackdrop(p: NovaPalette): Modifier = this
    .background(Brush.verticalGradient(listOf(p.bgTop, p.bgBottom)))
    .background(Brush.radialGradient(listOf(p.accent.copy(alpha = if (p.dark) 0.16f else 0.10f), Color.Transparent), center = Offset(0f, 0f), radius = 1400f))
    .background(Brush.radialGradient(listOf(p.accent2.copy(alpha = if (p.dark) 0.12f else 0.08f), Color.Transparent), center = Offset(2400f, 1400f), radius = 1600f))

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

    // Persist location, refresh weather and the city name when the coarse position changes.
    LaunchedEffect(coarse, settings.showWeather, settings.arabic) {
        val loc = coarse ?: c.settings.lastLocation()
        coarse?.let { c.settings.saveLastLocation(it.first, it.second) }
        if (loc != null && settings.showWeather && !c.safeMode) {
            c.weather.maybeRefresh(loc.first, loc.second)
            c.weather.maybeResolveCity(loc.first, loc.second, settings.arabic)
        }
    }
    // Resume the OBD link if the profile has it enabled.
    LaunchedEffect(settings.obdEnabled, settings.obdAddress) {
        val addr = settings.obdAddress
        if (settings.obdEnabled && addr != null && !c.safeMode) c.obd.start(addr) else c.obd.stop()
    }

    val locationPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        c.refreshPermissions()
    }
    val requestLocation = {
        locationPerm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(Unit) { if (!c.location.hasPermission()) requestLocation() }

    val apps by c.apps.apps.collectAsStateWithLifecycle()
    val sc = rememberShortcuts(c, settings, apps)
    CarPlayAutoLaunch(c, settings, sc)

    NovaTheme(dark = dark, accentIndex = settings.accent, arabic = settings.arabic) {
        val p = LocalNova.current
        var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
        var settingsStart by remember { mutableStateOf<String?>(null) }
        var quick by remember { mutableStateOf(false) }
        var split by remember { mutableStateOf(false) }
        val go: (Screen) -> Unit = { screen = it; settingsStart = null }
        val openSettings: (String?) -> Unit = { settingsStart = it; screen = Screen.SETTINGS }

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

        val bt by c.bluetooth.collectAsStateWithLifecycle()
        val net by c.network.collectAsStateWithLifecycle()
        val perms by c.permissions.collectAsStateWithLifecycle()
        val fix = fixState.value
        val gps = remember(fix?.fromGps, perms) {
            when {
                !c.location.hasPermission() || !c.status.gpsEnabled() -> GpsState.OFF
                fix?.fromGps == true -> GpsState.FIX
                else -> GpsState.SEARCHING
            }
        }
        // Home mode lives in a ViewModel: it survives recomposition/config changes and reacts to
        // verified route start/end from the navigation app's guidance notification.
        val modeVm: HomeModeViewModel = viewModel(
            factory = remember { HomeModeViewModel.Factory(c.settings, NovaNotificationListener.navigation.map { it != null }) }
        )
        val modeState by modeVm.state.collectAsStateWithLifecycle()
        val onMode: (HomeMode) -> Unit = { modeVm.select(it); screen = Screen.HOME; settingsStart = null }
        val actions = Actions(c, settings, sc, driving, onApps = { go(Screen.APPS) })

        Box(Modifier.fillMaxSize().novaBackdrop(p)) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                ShellLayout(
                    rail = { Rail(screen, modeState.mode, go, onMode, onPhone = actions::phone) },
                    topBar = {
                        TopBar(
                            bt = bt, net = net, gps = gps, dark = dark, themeMode = settings.themeMode, use24h = settings.use24h,
                            onBluetooth = { c.controls.openBluetooth() },
                            onWifi = { c.controls.openWifi() },
                            onGps = { if (!c.location.hasPermission()) requestLocation() else c.controls.openLocationSettings() },
                            onToggleTheme = { c.settings.update { it.copy(themeMode = if (dark) ThemeMode.LIGHT else ThemeMode.DARK) } },
                            onAutoTheme = { c.settings.update { it.copy(themeMode = ThemeMode.AUTO) } },
                            onQuick = { quick = true },
                            onVoice = onVoice,
                        )
                    },
                    overlay = {
                        AnimatedVisibility(
                            quick,
                            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 4 },
                            exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { -it / 4 },
                        ) {
                            QuickPanel(c, settings, onClose = { quick = false }, onSettings = { quick = false; openSettings(null) },
                                onSplit = { quick = false; split = true }, onVoice = { quick = false; onVoice() })
                        }
                        AnimatedVisibility(split, enter = fadeIn(tween(180)), exit = fadeOut(tween(150))) {
                            SplitSheet(c, settings) { split = false }
                        }
                        if (c.safeMode && screen == Screen.HOME) Box(Modifier.fillMaxSize()) {
                            Pill(
                                tr("Safe mode — tap for options", "الوضع الآمن — اضغط للخيارات"), p.danger,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp)
                            ) { openSettings(null) }
                        }
                    },
                ) {
                    Crossfade(screen, animationSpec = tween(200), label = "screen") { s ->
                        Routes(s, c, settings, sc, apps, driving, actions, go, openSettings, settingsStart, requestLocation, modeState.mode, modeState.autoPromoted, onMode)
                    }
                }
            }
        }
    }
}

@Composable
private fun Routes(
    s: Screen,
    c: AppContainer,
    settings: NovaSettings,
    sc: Shortcuts,
    apps: List<AppEntry>,
    driving: Boolean,
    a: Actions,
    go: (Screen) -> Unit,
    openSettings: (String?) -> Unit,
    settingsStart: String?,
    requestLocation: () -> Unit,
    mode: HomeMode,
    autoPromoted: Boolean,
    onMode: (HomeMode) -> Unit,
) {
    val ctx = LocalContext.current
    when (s) {
        Screen.HOME -> {
          var dockPick by remember { mutableStateOf<String?>(null) }
          HomeScreen(c, settings, driving, mode, autoPromoted) { ui ->
            HomeActions(
                onDockLong = { slot ->
                    if (slot.id == "apps") Unit
                    else if (driving) toast(ctx, if (settings.arabic) "تغيير الشريط متاح عند التوقف" else "Dock editing is available when parked")
                    else dockPick = slot.id
                },
                onMode = onMode,
                onNavDetails = { go(Screen.NAV) },
                onMusicDetails = { go(Screen.MUSIC) },
                onSeek = c.media::seekTo,
                onEnableLocation = requestLocation,
                onVehicle = { go(Screen.VEHICLE) },
                onOpenNav = a::openNav,
                onNavCard = { a.openGuidance(ui.nav) },
                onHome = { a.navigate(settings.homeAddress) },
                onWork = { a.navigate(settings.workAddress) },
                onGrantAccess = { c.controls.openNotificationAccess() },
                onOpenMedia = { a.openMedia(ui.media?.packageName) },
                onPrev = c.media::previous, onPlayPause = c.media::playPause, onNext = c.media::next,
                onDock = a::dock,
            )
          }
          dockPick?.let { id ->
              AppPickerDialog(
                  if (settings.arabic) "اختر تطبيقاً لهذا الزر" else "Choose an app for this button", apps, allowNone = false,
                  onPick = { app ->
                      if (app != null) c.settings.update { st ->
                          val i = st.dock.indexOf(id)
                          st.copy(dock = replaceDockEntry(st.dock, if (i < 0) st.dock.size else i, app.packageName))
                      }
                      dockPick = null
                  },
                  onDismiss = { dockPick = null },
              )
          }
        }
        Screen.NAV -> {
            val nav by NovaNotificationListener.navigation.collectAsStateWithLifecycle()
            val perms by c.permissions.collectAsStateWithLifecycle()
            val navApps = remember(apps) {
                apps.filter { e -> e.packageName in AppsRepository.NAV_PKGS || AppsRepository.NAV_KEYS.any { e.label.contains(it, true) } }
            }
            NavigationScreen(
                nav = nav, hasAccess = remember(perms) { MediaRepository.hasAccess(ctx) },
                navApp = sc.nav, navApps = navApps,
                homeAddress = settings.homeAddress, workAddress = settings.workAddress, driving = driving,
                onCard = { a.openGuidance(nav) }, onOpenNav = a::openNav,
                onGrantAccess = { c.controls.openNotificationAccess() },
                onNavigate = a::navigate, onLaunchApp = { c.apps.launch(it) },
                onSetPlaces = { openSettings("shortcuts") },
            )
        }
        Screen.MUSIC -> {
            val media by c.mediaInfo.collectAsStateWithLifecycle()
            val perms by c.permissions.collectAsStateWithLifecycle()
            val musicApps = remember(apps) {
                apps.filter { e -> e.packageName in AppsRepository.MUSIC_PKGS || AppsRepository.MUSIC_KEYS.any { e.label.contains(it, true) || e.packageName.contains(it, true) } }
                    .take(12)
            }
            val source = media?.packageName?.let { pkg -> apps.firstOrNull { it.packageName == pkg } } ?: sc.music
            MusicScreen(
                info = media, hasAccess = remember(perms) { MediaRepository.hasAccess(ctx) },
                sourceApp = source, musicApps = musicApps,
                onOpenSource = { a.openMedia(media?.packageName) },
                onGrantAccess = { c.controls.openNotificationAccess() },
                onPrev = c.media::previous, onPlayPause = c.media::playPause, onNext = c.media::next,
                onQueueItem = c.media::playQueueItem, onLaunchApp = { c.apps.launch(it) },
            )
        }
        Screen.VEHICLE -> {
            val data by c.obd.data.collectAsStateWithLifecycle()
            VehicleScreen(
                brand = carBrand(settings.carBrand), model = carModel(settings.carBrand, settings.carModel),
                paint = CarPaints.getOrElse(settings.carPaint) { CarPaints[0] },
                obdEnabled = settings.obdEnabled, data = data, onSetup = { openSettings("vehicle") },
                carImageVersion = settings.carImageVersion,
            )
        }
        Screen.APPS -> AppsScreen(
            apps = apps, favorites = settings.favorites, showSystem = settings.showSystemApps, driving = driving,
            onLaunch = a::launch,
            onToggleFavorite = c.settings::toggleFavorite,
            onMoveFavorite = c.settings::moveFavorite,
            onShowSystem = { v -> c.settings.update { it.copy(showSystemApps = v) } },
            onParkedOnly = { toast(ctx, if (settings.arabic) "متاح عند التوقف" else "Available when parked") },
        )
        Screen.SETTINGS -> SettingsScreen(c, settings, driving, settingsStart) { go(Screen.HOME) }
    }
}

/** Every outward action, each falling back gracefully when an app or service is missing. */
private class Actions(val c: AppContainer, val s: NovaSettings, val sc: Shortcuts, val driving: Boolean, val onApps: () -> Unit) {
    private val ar get() = s.arabic
    private fun say(en: String, a: String) = toast(c.appContext, if (ar) a else en)

    private fun open(entry: AppEntry?, en: String, a: String): Boolean {
        if (entry != null && c.apps.launch(entry)) return true
        say("Choose a $en app in Settings › Shortcuts", "اختر تطبيق $a من الإعدادات › الاختصارات")
        return false
    }

    fun openNav() { open(sc.nav, "navigation", "الملاحة") }

    fun navigate(address: String) {
        if (address.isBlank()) return
        if (!c.controls.navigateTo(address, sc.nav?.packageName)) say("No navigation app found", "لا يوجد تطبيق ملاحة")
    }

    /** Re-opens the navigation app exactly where its guidance notification points. */
    fun openGuidance(nav: NavInfo?) {
        val pi = nav?.open
        if (pi != null) {
            val sent = runCatching {
                val opts = ActivityOptions.makeBasic()
                if (Build.VERSION.SDK_INT >= 34) opts.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                pi.send(c.appContext, 0, null, null, null, null, opts.toBundle())
            }.isSuccess
            if (sent) return
        }
        if (nav != null && c.apps.launchPackage(nav.packageName)) return
        openNav()
    }

    fun openMedia(pkg: String?) {
        if (pkg != null && c.apps.launchPackage(pkg)) return
        open(sc.music, "music", "الموسيقى")
    }

    /** The chosen phone app, else the unit's own dialer (built-in Bluetooth phone). */
    fun phone() {
        if (s.phonePackage != null && sc.phone != null && c.apps.launch(sc.phone)) return
        if (c.controls.dial()) return
        if (sc.phone != null && c.apps.launch(sc.phone)) return
        say("No phone app found — choose one in Settings › Shortcuts", "لا يوجد تطبيق هاتف — اختره من الإعدادات › الاختصارات")
    }

    /** Opens an installed app; video apps wait until the car is parked. */
    fun launch(app: AppEntry) {
        if (driving && isVideoApp(app.packageName, app.label)) { say("Video is available when parked", "الفيديو متاح عند التوقف"); return }
        if (!c.apps.launch(app)) say("Couldn't open ${app.label}", "تعذّر فتح ${app.label}")
    }

    fun dock(slot: DockSlot) {
        val app = slot.app
        if (driving && slot.id in VIDEO_SLOTS) { say("Video is available when parked", "الفيديو متاح عند التوقف"); return }
        when {
            slot.id == "apps" -> onApps()
            slot.id == "phone" -> phone()
            app != null -> launch(app)
            slot.storePackage != null -> {
                val pkg = slot.storePackage
                val ok = c.controls.start(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))) ||
                    c.controls.start(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
                if (!ok) say("${slot.title} isn't installed", "${slot.title} غير مثبت")
            }
            slot.id == "carplay" -> say("No CarPlay app found (ZLink / DiPlay). Choose it in Settings › CarPlay.",
                "لم يتم العثور على تطبيق كاربلاي (ZLink / DiPlay). اختره من الإعدادات › كاربلاي.")
            else -> say("${slot.title} isn't installed", "${slot.title} غير مثبت")
        }
    }
}

/** Opens CarPlay when an iPhone is plugged in, only while NOVA is on screen and if enabled. */
@Composable
private fun CarPlayAutoLaunch(c: AppContainer, settings: NovaSettings, sc: Shortcuts) {
    val latest by rememberUpdatedState(Pair(settings, sc))
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var prev = c.iPhoneConnected.value
            c.iPhoneConnected.collect { now ->
                val (s, shortcuts) = latest
                if (now && !prev && s.autoLaunchCarPlay && !c.safeMode) shortcuts.carPlay?.let { c.apps.launch(it) }
                prev = now
            }
        }
    }
}
