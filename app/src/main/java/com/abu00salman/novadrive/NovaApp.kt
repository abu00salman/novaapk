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
import kotlinx.coroutines.flow.stateIn

class NovaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashGuard.install(this)
        container = AppContainer(this)
    }
}

/** Lightweight manual DI: everything heavy is lazy so cold start stays fast. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppContainer(private val app: Application) {
    val appContext: Context get() = app
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val safeMode: Boolean = CrashGuard.isSafeMode(app)

    val settings = SettingsRepository(app)
    val apps by lazy { AppsRepository(app, scope) }
    val location by lazy { LocationRepository(app) }
    val weather by lazy { WeatherRepository(app, scope) }
    val media by lazy { MediaRepository(app) { settings.current.arabic } }
    val carPlay by lazy { CarPlayDetector(app) }
    val controls by lazy { SystemControls(app) }
    val obd by lazy { ObdClient(app, scope) }
    val status by lazy { StatusRepository(app) }

    /** Screen/mode changes requested by «أمرني» voice commands, handled by the UI. */
    val voiceActions = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val speaker by lazy { com.abu00salman.novadrive.data.Speaker(app) }

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
        append(android.provider.Settings.System.canWrite(app))
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

    val iPhoneConnected: StateFlow<Boolean> by lazy {
        carPlay.connected().stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)
    }
}

val Context.nova: AppContainer get() = (applicationContext as NovaApp).container
