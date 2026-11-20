package com.abu00salman.novadrive

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.abu00salman.novadrive.core.CrashGuard
import com.abu00salman.novadrive.core.SettingsRepository
import com.abu00salman.novadrive.data.AppsRepository
import com.abu00salman.novadrive.data.CarPlayDetector
import com.abu00salman.novadrive.data.GeoFix
import com.abu00salman.novadrive.data.LocationRepository
import com.abu00salman.novadrive.data.MediaInfo
import com.abu00salman.novadrive.data.MediaRepository
import com.abu00salman.novadrive.data.ObdClient
import com.abu00salman.novadrive.data.Link
import com.abu00salman.novadrive.data.NetState
import com.abu00salman.novadrive.data.StatusRepository
import com.abu00salman.novadrive.data.SystemControls
import com.abu00salman.novadrive.data.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn

class NovaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashGuard.install(this)
        container = AppContainer(this)
        // Restore the screen dimmer (brightness) chosen before the last restart.
        container.controls.dimmer.apply()
    }
}

/** Lightweight manual DI: everything heavy is lazy so cold start stays fast. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppContainer(private val app: Application) {
    val appContext: Context get() = app
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val safeMode: Boolean = CrashGuard.isSafeMode(app)

    val settings = SettingsRepository(app)
    /** Colours, wallpaper, font and light/dark of each interface, saved independently. */
    val styleOptions by lazy { com.abu00salman.novadrive.core.StyleOptionsRepository(app) }
    /** Which apps are opened from NovaDrive (kept on the unit): the Frequent tab and the smart dock slot. */
    val usage by lazy { com.abu00salman.novadrive.data.AppUsage(app) }
    val apps by lazy { AppsRepository(app, scope) { usage.record(it.key) } }
    /** NOVA Scenes (saved dashboards, per profile), Diagnostics and the QuickSwitch overlay. */
    val scenes by lazy { com.abu00salman.novadrive.core.SceneRepository(app, settings) { widgets } }
    val health by lazy { com.abu00salman.novadrive.data.SystemHealth(this) }
    val quickSwitch by lazy { com.abu00salman.novadrive.data.QuickSwitchOverlay(this) }

    /** Restores a scene and shows it. */
    fun applyScene(scene: com.abu00salman.novadrive.core.Scene) {
        scenes.apply(scene)
        voiceActions.tryEmit("mode:" + settings.current.homeMode)
    }
    val location by lazy { LocationRepository(app) }
    val weather by lazy { WeatherRepository(app, scope) }
    val media by lazy { MediaRepository(app) { settings.current.arabic } }
    val carPlay by lazy { CarPlayDetector(app) }
    val controls by lazy { SystemControls(app) }
    val obd by lazy { ObdClient(app, scope) }
    val status by lazy { StatusRepository(app) }

    /** Screen/mode changes requested by «أمرني» voice commands, handled by the UI. */
    val voiceActions = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Widgets interface: the saved board and the GPS trip computer. */
    val widgets by lazy { com.abu00salman.novadrive.core.WidgetRepository(app) }
    val trip by lazy { com.abu00salman.novadrive.data.TripMeter(scope, fix) }
    /** Interface sound effects: settings and the player. */
    val sounds by lazy { com.abu00salman.novadrive.core.SoundRepository(app) }
    val soundFx by lazy { com.abu00salman.novadrive.data.SoundEngine(this) }
    val speaker by lazy { com.abu00salman.novadrive.data.Speaker(app) { ai.config.value.voice } }
    /** Nova AI settings (provider, models, encrypted keys) and the assistant itself. */
    val ai by lazy { com.abu00salman.novadrive.data.ai.AiConfigRepository(app) }
    val assistant by lazy { com.abu00salman.novadrive.data.ai.NovaAssistant(this) }
    /** In-app turn-by-turn navigation; survives interface changes and other apps in front. */
    val navEngine by lazy {
        com.abu00salman.novadrive.data.nav.NavEngine(app, scope, fix, { settings.current.arabic }) { t -> speaker.speak(t, settings.current.arabic) }
    }

    /** Emitted when the HOME button is pressed while we are already in front. */
    val homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Changes whenever a runtime/special permission changes, so flows re-subscribe. */
    private val permSignature = MutableStateFlow(computePermSignature())
    val permissions: StateFlow<String> = permSignature

    fun refreshPermissions() { permSignature.value = computePermSignature() }

    private fun granted(p: String) =
        ContextCompat.checkSelfPermission(app, p) == PackageManager.PERMISSION_GRANTED

    private fun computePermSignature(): String = buildString {
        append(granted(Manifest.permission.ACCESS_FINE_LOCATION))
        append(granted(Manifest.permission.ACCESS_COARSE_LOCATION))
        if (Build.VERSION.SDK_INT >= 31) append(granted(Manifest.permission.BLUETOOTH_CONNECT))
        append(MediaRepository.hasAccess(app))
        append(android.provider.Settings.canDrawOverlays(app))
    }

    val fix: StateFlow<GeoFix?> by lazy {
        permSignature.flatMapLatest { location.fixes() }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)
    }

    /** ~5 km resolution; changes rarely, safe to observe at the root of the UI. */
    val coarseLocation: StateFlow<Pair<Double, Double>?> by lazy {
        fix.map { f -> f?.let { Pair(Math.round(it.lat * 20) / 20.0, Math.round(it.lon * 20) / 20.0) } }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)
    }

    val mediaInfo: StateFlow<MediaInfo?> by lazy {
        permSignature.flatMapLatest { media.sessions() }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)
    }

    val bluetooth: StateFlow<Link> by lazy {
        permSignature.flatMapLatest { status.bluetooth() }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), Link.OFF)
    }

    val network: StateFlow<NetState> by lazy {
        status.network().stateIn(scope, SharingStarted.WhileSubscribed(5_000), NetState())
    }

    val speedCameras by lazy { com.abu00salman.novadrive.data.SpeedCameraRepository(app).also { it.load() } }
    private var camAnnounced: com.abu00salman.novadrive.data.nav.LatLon? = null
    private var camRefreshAt = 0L
    private var camBeepAt = 0L
    private val camTone by lazy { android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 90) }

    /** The speed camera (ساهر) ahead, with a spoken warning once per camera and a beep above its limit. */
    val cameraAlert: StateFlow<com.abu00salman.novadrive.data.CameraAlert?> by lazy {
        fix.map { f ->
            val s = settings.current
            if (f == null || !s.cameraAlerts) return@map null
            val here = com.abu00salman.novadrive.data.nav.LatLon(f.lat, f.lon)
            val now = System.currentTimeMillis()
            if (now - camRefreshAt > 60_000) {
                camRefreshAt = now
                scope.launch { speedCameras.refreshIfNeeded(here, assistant.online()) }
            }
            val heading = f.bearing?.takeIf { f.speedKmh > 10f }
            val a = com.abu00salman.novadrive.data.SpeedCameraRepository.ahead(here, heading, speedCameras.cameras)
            if (a != null && a.distanceM < 550 && a.camera.at != camAnnounced) {
                camAnnounced = a.camera.at
                val m = (a.distanceM / 50).toInt() * 50
                val lim = a.camera.maxKmh
                speaker.speak(
                    if (s.arabic) "كاميرا ساهر بعد $m متر" + (lim?.let { "، السرعة $it" } ?: "")
                    else "Speed camera in $m metres" + (lim?.let { ", limit $it" } ?: ""), s.arabic,
                )
            }
            val lim = a?.camera?.maxKmh
            if (lim != null && f.speedKmh > lim + 2 && now - camBeepAt > 4_000) {
                camBeepAt = now
                runCatching { camTone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 400) }
            }
            a
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)
    }

    val iPhoneConnected: StateFlow<Boolean> by lazy {
        carPlay.connected().stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)
    }
}

val Context.nova: AppContainer get() = (applicationContext as NovaApp).container
