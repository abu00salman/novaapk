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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abu00salman.novadrive.core.AppTab
import com.abu00salman.novadrive.core.CarPaints
import com.abu00salman.novadrive.core.HomeMode
import com.abu00salman.novadrive.core.ThemeMode
import com.abu00salman.novadrive.core.carBrand
import com.abu00salman.novadrive.core.carModel
import com.abu00salman.novadrive.data.AppCategory
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.data.Link
import com.abu00salman.novadrive.data.MediaInfo
import com.abu00salman.novadrive.data.NavInfo
import com.abu00salman.novadrive.data.NetState
import com.abu00salman.novadrive.data.QueueEntry
import com.abu00salman.novadrive.data.VehicleData
import com.abu00salman.novadrive.data.Weather
import com.abu00salman.novadrive.ui.apps.AppsScreen
import com.abu00salman.novadrive.ui.home.DockSlot
import com.abu00salman.novadrive.ui.home.HomeActions
import com.abu00salman.novadrive.ui.home.HomeLayout
import com.abu00salman.novadrive.ui.home.HomeUi
import com.abu00salman.novadrive.ui.home.dockTitle
import com.abu00salman.novadrive.ui.novaBackdrop
import com.abu00salman.novadrive.ui.shell.GpsState
import com.abu00salman.novadrive.ui.shell.Rail
import com.abu00salman.novadrive.ui.shell.Screen
import com.abu00salman.novadrive.ui.shell.ShellLayout
import com.abu00salman.novadrive.ui.shell.TopBar
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaTheme
import com.abu00salman.novadrive.ui.vehicle.VehicleScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneIphone
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real Compose screens at the head unit's 1280×720 (and the 1024×600 fallback) to
 * PNG with Robolectric native graphics. Values are illustrative fixtures for screenshots only —
 * the app itself shows live device data or an explicit unavailable state.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1280dp-h720dp-land-mdpi", application = Application::class)
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val ctx get() = ApplicationProvider.getApplicationContext<Application>()

    private fun icon(color: Int, letter: String): Bitmap {
        val b = Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        c.drawRoundRect(RectF(6f, 6f, 138f, 138f), 34f, 34f, p)
        p.color = 0xFFFFFFFF.toInt(); p.textSize = 70f; p.typeface = Typeface.DEFAULT_BOLD; p.textAlign = Paint.Align.CENTER
        c.drawText(letter, 72f, 96f, p)
        return b
    }

    private fun app(pkg: String, label: String, color: Long, letter: String, cat: AppCategory = AppCategory.OTHER, system: Boolean = false) =
        AppEntry("$pkg/.Main", pkg, ".Main", label, icon(color.toInt(), letter).asImageBitmap(), cat, system)

    private val maps get() = app("com.google.android.apps.maps", "Google Maps", 0xFF1A73E8, "M", AppCategory.NAVIGATION)
    private val waze get() = app("com.waze", "Waze", 0xFF33CCFF, "W", AppCategory.NAVIGATION)
    private val spotify get() = app("com.spotify.music", "Spotify", 0xFF1DB954, "S", AppCategory.MEDIA)
    private val anghami get() = app("com.anghami", "Anghami", 0xFF8A2BE2, "A", AppCategory.MEDIA)
    private val youtube get() = app("com.google.android.youtube", "YouTube", 0xFFFF0033, "▶", AppCategory.MEDIA)
    private val zlink get() = app("com.zjinnova.zlink", "ZLink", 0xFF2A7BFF, "Z", AppCategory.CAR)
    private val btPhone get() = app("com.car.btphone", "هاتف البلوتوث", 0xFF22A06B, "☎", AppCategory.CAR)
    private val radio get() = app("com.car.radio", "Radio", 0xFFF2680F, "R", AppCategory.MEDIA)
    private val settingsApp get() = app("com.android.settings", "الإعدادات", 0xFF5A6580, "⚙", AppCategory.TOOLS, system = true)
    private val files get() = app("com.car.files", "مدير الملفات", 0xFF3B82F6, "F", AppCategory.TOOLS)
    private val chrome get() = app("com.android.chrome", "Chrome", 0xFFEA4335, "C", AppCategory.TOOLS)

    private fun art(): Bitmap {
        val src = BitmapFactory.decodeResource(ctx.resources, R.drawable.scenic_road)
        val side = minOf(src.width, src.height)
        return Bitmap.createScaledBitmap(Bitmap.createBitmap(src, 0, (src.height - side) / 2, side, side), 320, 320, true)
    }

    private fun media(ar: Boolean) = MediaInfo(
        title = if (ar) "طريق الساحل" else "Coastal Road", artist = if (ar) "جلسات نوفا" else "Nova Sessions",
        art = art().asImageBitmap(), playing = true, packageName = "com.spotify.music",
        durationMs = 332_000, positionMs = 134_000, positionAt = Long.MAX_VALUE / 2, speed = 0f,
        queue = listOf(QueueEntry(1, "طريق الساحل", "جلسات نوفا")), activeQueueId = 1, canSeek = true,
    )

    private val live = VehicleData(VehicleData.State.LIVE, rpm = 1850, speedKmh = 63, coolantC = 88, voltage = 14.1f, fuelPct = 62, loadPct = 31, mil = false, dtcCount = 0)

    private fun nav(ar: Boolean) = NavInfo(
        "com.google.android.apps.maps", if (ar) "300 م" else "300 m",
        if (ar) "استمر مباشرة باتجاه طريق الملك فهد" else "Continue onto King Fahd Rd",
        if (ar) "12 دقيقة · 8.4 كم · الوصول 1:04 م" else "12 min · 8.4 km · 1:04 PM", null, null,
    )

    private fun dock(ar: Boolean) = listOf(
        DockSlot("maps", dockTitle("maps", ar), maps, Icons.Rounded.Map, "com.google.android.apps.maps"),
        DockSlot("carplay", dockTitle("carplay", ar), zlink, Icons.Rounded.PhoneIphone),
        DockSlot("music", dockTitle("music", ar), spotify, Icons.Rounded.MusicNote, "com.spotify.music"),
        DockSlot("phone", dockTitle("phone", ar), btPhone, Icons.Rounded.Phone, builtIn = true),
        DockSlot("auto", dockTitle("auto", ar), null, Icons.Rounded.DirectionsCar, "com.google.android.projection.gearhead"),
        DockSlot("apps", dockTitle("apps", ar), null, Icons.Rounded.Apps, builtIn = true),
    )

    private fun ui(ar: Boolean, gps: Boolean = true, obd: Boolean = true, playing: Boolean = true, guiding: Boolean = true) = HomeUi(
        speedKmh = if (gps) 63f else null, hasLocation = gps,
        vehicle = if (obd) live else VehicleData(), obdEnabled = obd,
        brand = carBrand("hyundai"), car = carModel("hyundai", "sonata19"), paint = CarPaints[0],
        weather = Weather(35.0, 0, true, 0, 38.0, 29.0), city = if (ar) "جازان" else "Jazan", showWeather = true,
        media = if (playing) media(ar) else null, notificationAccess = true,
        nav = if (guiding) nav(ar) else null, navApp = maps,
        homeSet = true, workSet = true, dock = dock(ar), use24h = false,
    )

    private fun shot(name: String, screen: Screen, mode: HomeMode = HomeMode.CAR, dark: Boolean = true, ar: Boolean = true,
                     railRight: Boolean = false, textScale: Float = 1f, iconScale: Float = 1f, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            NovaTheme(dark = dark, accentIndex = 0, arabic = ar) {
                Box(Modifier.fillMaxSize().novaBackdrop(LocalNova.current)) {
                    ShellLayout(
                        railRight = railRight, textScale = textScale, iconScale = iconScale,
                        rail = { Rail(screen, mode, {}, {}, {}) },
                        topBar = {
                            TopBar(Link.CONNECTED, NetState(Link.CONNECTED, false), GpsState.FIX, dark, ThemeMode.AUTO, false,
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

    private fun home(name: String, mode: HomeMode, dark: Boolean, ar: Boolean = true, ui: HomeUi = ui(ar), auto: Boolean = false) =
        shot(name, Screen.HOME, mode, dark, ar) { HomeLayout(ui, mode, auto, HomeActions()) }

    @Test fun car_night() = home("01_car_night_ar", HomeMode.CAR, dark = true, ui = ui(true, guiding = false))
    @Test fun car_day() = home("02_car_day_ar", HomeMode.CAR, dark = false, ui = ui(true, guiding = false))
    @Test fun navigation_night() = home("03_navigation_night_ar", HomeMode.NAVIGATION, dark = true, auto = true)
    @Test fun navigation_day_noRoute() = home("04_navigation_day_ar_no_route", HomeMode.NAVIGATION, dark = false, ui = ui(true, guiding = false))
    @Test fun music_night() = home("05_music_night_ar", HomeMode.MUSIC, dark = true)
    @Test fun music_day() = home("06_music_day_ar", HomeMode.MUSIC, dark = false)
    @Test fun split_night() = home("07_split_night_ar", HomeMode.SPLIT, dark = true)
    @Test fun split_day_en() = home("08_split_day_en", HomeMode.SPLIT, dark = false, ar = false, ui = ui(false))
    @Test fun music_empty() = home("09_music_empty_night_ar", HomeMode.MUSIC, dark = true, ui = ui(true, playing = false, guiding = false))
    @Test fun car_noGpsNoObd() = home("10_car_no_gps_no_obd_night_ar", HomeMode.CAR, dark = true, ui = ui(true, gps = false, obd = false, playing = false, guiding = false))

    private val allApps get() = listOf(maps, waze, spotify, anghami, youtube, radio, zlink, btPhone, settingsApp, files, chrome) +
        (1..9).map { app("pkg.app$it", "تطبيق $it", 0xFF3B4A6B, "$it") }

    @Test fun apps_all() = shot("11_apps_all_night_ar", Screen.APPS) {
        AppsScreen(allApps, listOf(maps.key, zlink.key, spotify.key), false, false, {}, {}, { _, _ -> }, {}, {}, initialTab = AppTab.ALL)
    }

    @Test fun apps_car_day() = shot("12_apps_car_day_ar", Screen.APPS, dark = false) {
        AppsScreen(allApps, listOf(maps.key), false, false, {}, {}, { _, _ -> }, {}, {}, initialTab = AppTab.CAR)
    }

    @Test @Config(qualifiers = "w1024dp-h600dp-land-mdpi")
    fun car_1024() = home("13_car_1024x600_night_ar", HomeMode.CAR, dark = true, ui = ui(true, guiding = false))

    @Test @Config(qualifiers = "w1024dp-h600dp-land-mdpi")
    fun split_1024() = home("14_split_1024x600_day_ar", HomeMode.SPLIT, dark = false)

    @Test fun railRight_largeText() = shot("16_car_rail_right_large_text_night_ar", Screen.HOME, railRight = true, textScale = 1.3f, iconScale = 1.3f) {
        HomeLayout(ui(true, obd = false, guiding = false), HomeMode.CAR, false, HomeActions())
    }

    @Test fun vehicle() = shot("15_vehicle_night_ar", Screen.VEHICLE) {
        VehicleScreen(carBrand("hyundai"), carModel("hyundai", "sonata19"), CarPaints[0], true, live, {})
    }
}
