package com.abu00salman.novadrive

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abu00salman.novadrive.core.CarPaints
import com.abu00salman.novadrive.core.ThemeMode
import com.abu00salman.novadrive.core.carBrand
import com.abu00salman.novadrive.core.carModel
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.data.Link
import com.abu00salman.novadrive.data.MediaInfo
import com.abu00salman.novadrive.data.NavInfo
import com.abu00salman.novadrive.data.NetState
import com.abu00salman.novadrive.data.QueueEntry
import com.abu00salman.novadrive.data.VehicleData
import com.abu00salman.novadrive.data.Weather
import com.abu00salman.novadrive.ui.apps.AppsScreen
import com.abu00salman.novadrive.ui.home.DashActions
import com.abu00salman.novadrive.ui.home.DashUi
import com.abu00salman.novadrive.ui.home.Dashboard
import com.abu00salman.novadrive.ui.home.DockSlot
import com.abu00salman.novadrive.ui.music.MusicScreen
import com.abu00salman.novadrive.ui.nav.NavigationScreen
import com.abu00salman.novadrive.ui.novaBackdrop
import com.abu00salman.novadrive.ui.shell.GpsState
import com.abu00salman.novadrive.ui.shell.Rail
import com.abu00salman.novadrive.ui.shell.Screen
import com.abu00salman.novadrive.ui.shell.ShellLayout
import com.abu00salman.novadrive.ui.shell.TopBar
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaTheme
import com.abu00salman.novadrive.ui.vehicle.VehicleScreen
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real Compose screens at the head unit's 1280×720 to PNG (Robolectric native
 * graphics). Values below are illustrative fixtures for the screenshots only — the app itself
 * shows live device data or an explicit "unavailable" state.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1280dp-h720dp-land-mdpi", application = Application::class)
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val ctx get() = ApplicationProvider.getApplicationContext<Application>()

    private fun icon(color: Int, letter: String): Bitmap {
        val b = Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        c.drawRoundRect(RectF(4f, 4f, 108f, 108f), 26f, 26f, p)
        p.color = 0xFFFFFFFF.toInt(); p.textSize = 56f; p.typeface = Typeface.DEFAULT_BOLD; p.textAlign = Paint.Align.CENTER
        c.drawText(letter, 56f, 76f, p)
        return b
    }

    private fun app(pkg: String, label: String, color: Long, letter: String) =
        AppEntry("$pkg/.Main", pkg, ".Main", label, icon(color.toInt(), letter).asImageBitmap())

    private val maps get() = app("com.google.android.apps.maps", "Google Maps", 0xFF1A73E8, "M")
    private val spotify get() = app("com.spotify.music", "Spotify", 0xFF1DB954, "S")
    private val zlink get() = app("com.zjinnova.zlink", "ZLink", 0xFF2A7BFF, "Z")
    private val youtube get() = app("com.google.android.youtube", "YouTube", 0xFFFF0033, "▶")
    private val waze get() = app("com.waze", "Waze", 0xFF33CCFF, "W")
    private val anghami get() = app("com.anghami", "Anghami", 0xFF8A2BE2, "A")

    private fun art(): Bitmap {
        val src = BitmapFactory.decodeResource(ctx.resources, R.drawable.scenic_road)
        val side = minOf(src.width, src.height)
        return Bitmap.createScaledBitmap(Bitmap.createBitmap(src, 0, src.height / 6, side, side), 320, 320, true)
    }

    private fun media(ar: Boolean) = MediaInfo(
        title = if (ar) "طريق الساحل" else "Coastal Road", artist = if (ar) "جلسات نوفا" else "Nova Sessions",
        art = art().asImageBitmap(), playing = true, packageName = "com.spotify.music",
        durationMs = 275_000, positionMs = 84_000, positionAt = Long.MAX_VALUE / 2, speed = 0f,
        queue = listOf(
            QueueEntry(1, if (ar) "طريق الساحل" else "Coastal Road", if (ar) "جلسات نوفا" else "Nova Sessions"),
            QueueEntry(2, if (ar) "ليل الصحراء" else "Desert Night", if (ar) "جلسات نوفا" else "Nova Sessions"),
            QueueEntry(3, if (ar) "أفق" else "Horizon", if (ar) "جلسات نوفا" else "Nova Sessions"),
        ),
        queueTitle = if (ar) "قائمة القيادة" else "Driving mix", activeQueueId = 1,
    )

    private val live = VehicleData(VehicleData.State.LIVE, rpm = 1850, speedKmh = 72, coolantC = 88, voltage = 14.1f, fuelPct = 62, loadPct = 31, mil = false, dtcCount = 0)

    private fun nav(ar: Boolean) = NavInfo(
        "com.google.android.apps.maps",
        if (ar) "300 م" else "300 m",
        if (ar) "باتجاه طريق الملك عبدالله" else "Toward King Abdullah Rd",
        if (ar) "3 دقائق · 1.2 كم" else "3 min · 1.2 km",
        null, null,
    )

    private fun dock(installed: Boolean) = listOf(
        DockSlot("maps", "Maps", if (installed) maps else null, Icons.Rounded.Map, "com.google.android.apps.maps"),
        DockSlot("music", "Music", if (installed) spotify else null, Icons.Rounded.MusicNote, "com.spotify.music"),
        DockSlot("phone", "Phone", null, Icons.Rounded.Phone, builtIn = true),
        DockSlot("carplay", "Apple CarPlay", if (installed) zlink else null, Icons.Rounded.PhoneIphone),
        DockSlot("auto", "Android Auto", if (installed) zlink else null, Icons.Rounded.DirectionsCar, "com.google.android.projection.gearhead"),
        DockSlot("youtube", "YouTube", if (installed) youtube else null, Icons.Rounded.OndemandVideo, "com.google.android.youtube"),
        DockSlot("netflix", "Netflix", null, Icons.Rounded.Movie, "com.netflix.mediaclient"),
        DockSlot("apps", "All apps", null, Icons.Rounded.Apps, builtIn = true),
    )

    private fun ui(ar: Boolean, full: Boolean) = DashUi(
        speedKmh = if (full) 72f else null, hasLocation = full,
        vehicle = if (full) live else VehicleData(), obdEnabled = full,
        car = carModel("hyundai", "sonata19"), paint = CarPaints[0].color,
        weather = Weather(35.0, 0, true, 0, 38.0, 26.0), city = if (ar) "جازان" else "Jazan", showWeather = true,
        media = if (full) media(ar) else null, notificationAccess = full,
        nav = if (full) nav(ar) else null, navAppLabel = "Google Maps",
        homeSet = true, workSet = true, dock = dock(full), use24h = false,
    )

    private fun shot(name: String, screen: Screen, dark: Boolean = true, ar: Boolean = true, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            NovaTheme(dark = dark, accentIndex = 0, arabic = ar) {
                Box(Modifier.fillMaxSize().novaBackdrop(LocalNova.current)) {
                    ShellLayout(
                        rail = { Rail(screen, {}, {}) },
                        topBar = {
                            TopBar(Link.CONNECTED, NetState(Link.CONNECTED, true), GpsState.FIX, dark, ThemeMode.AUTO, false,
                                {}, {}, {}, {}, {}, {}, {})
                        },
                        content = content,
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_500)
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    @Test fun home_ar_dark() = shot("01_home_ar_dark", Screen.HOME) { Dashboard(ui(true, true), DashActions()) }

    @Test fun home_ar_dark_noData() = shot("02_home_ar_dark_no_data", Screen.HOME) { Dashboard(ui(true, false), DashActions()) }

    @Test fun home_en_light() = shot("03_home_en_day", Screen.HOME, dark = false, ar = false) { Dashboard(ui(false, true), DashActions()) }

    @Test fun navigation() = shot("04_navigation_ar", Screen.NAV) {
        NavigationScreen(nav(true), true, maps, listOf(maps, waze), "حي الروضة، جازان", "", false,
            {}, {}, {}, {}, {}, {})
    }

    @Test fun music() = shot("05_music_ar", Screen.MUSIC) {
        MusicScreen(media(true), true, spotify, listOf(spotify, anghami, youtube), {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun apps() = shot("06_apps_ar", Screen.APPS) {
        val all = listOf(maps, spotify, zlink, youtube, waze, anghami) + (1..12).map { app("pkg.$it", "App $it", 0xFF3B4A6B, "$it") }
        AppsScreen(all, listOf(maps.key, spotify.key, zlink.key), false, {}, {}, { _, _ -> }, {})
    }

    @Test fun vehicle() = shot("07_vehicle_ar", Screen.VEHICLE) {
        VehicleScreen(carBrand("hyundai"), carModel("hyundai", "sonata19"), CarPaints[0], true, live, {})
    }

    @Test @Config(qualifiers = "w1024dp-h600dp-land-mdpi")
    fun home_1024x600() = shot("08_home_ar_1024x600", Screen.HOME) { Dashboard(ui(true, true), DashActions()) }
}
