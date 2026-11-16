package com.abu00salman.novadrive

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import androidx.compose.ui.graphics.asImageBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abu00salman.novadrive.core.AppTab
import com.abu00salman.novadrive.core.Lang
import com.abu00salman.novadrive.core.NovaBackup
import com.abu00salman.novadrive.core.NovaSettings
import com.abu00salman.novadrive.core.Scene
import com.abu00salman.novadrive.core.SceneRepository
import com.abu00salman.novadrive.core.Scenes
import com.abu00salman.novadrive.core.SettingsRepository
import com.abu00salman.novadrive.core.ThemeMode
import com.abu00salman.novadrive.core.WidgetGrid
import com.abu00salman.novadrive.core.WidgetKind
import com.abu00salman.novadrive.core.WidgetRepository
import com.abu00salman.novadrive.core.filterApps
import com.abu00salman.novadrive.core.smartDockApp
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.data.AppUsage
import com.abu00salman.novadrive.data.ReleaseInfo
import com.abu00salman.novadrive.data.UpdateCenter
import com.abu00salman.novadrive.data.Versions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

/** Backup, Scenes, usage ranking / Smart Dock, Update Center, QuickSwitch, profiles and upgrades. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class NextFeaturesTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private fun prefs(name: String) = app.getSharedPreferences(name, Context.MODE_PRIVATE)

    @Before fun clean() {
        (NovaBackup.FILES.keys + "nova_crash").forEach { prefs(it).edit().clear().commit() }
    }

    private fun entry(pkg: String, label: String) =
        AppEntry("$pkg/.Main", pkg, ".Main", label, Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap())

    // ---- NOVA Backup

    @Test fun backupRoundTripRestoresEverythingButNeverKeys() {
        val settings = SettingsRepository(app)
        settings.update { it.copy(uiStyle = "bmw", language = Lang.EN, dock = listOf("maps", "smart", "apps"), textScale = 1.2f, homeAddress = "طريق الملك فهد") }
        settings.addProfile("Sara")
        settings.update { it.copy(uiStyle = "tesla") }
        prefs("widgets").edit().putString("board", "SPEED:LARGE,CLOCK:SMALL").commit()
        prefs("nova_ai").edit().putString("openai_key", "ENCRYPTED-KEY").putBoolean("nova_enabled", true).putInt("listen_timeout", 20).commit()
        prefs("dimmer").edit().putFloat("level", 0.6f).commit()

        val json = NovaBackup.export(app, "2.5.0-beta1")
        assertFalse("keys never leave the unit", json.contains("ENCRYPTED-KEY") || json.contains("openai_key"))

        // Another unit / a reset unit with its own key.
        (NovaBackup.FILES.keys).forEach { prefs(it).edit().clear().commit() }
        prefs("nova_ai").edit().putString("claude_key", "LOCAL-KEY").commit()
        val r = NovaBackup.import(app, json)
        assertTrue(r is NovaBackup.Result.Ok)
        assertEquals("2.5.0-beta1", (r as NovaBackup.Result.Ok).fromVersion)

        val restored = SettingsRepository(app)
        assertEquals(listOf("Driver", "Sara"), restored.current.profileNames)
        assertEquals("tesla", restored.current.uiStyle)
        restored.switchProfile(0)
        assertEquals("bmw", restored.current.uiStyle)
        assertEquals(Lang.EN, restored.current.language)
        assertEquals(listOf("maps", "smart", "apps"), restored.current.dock)
        assertEquals(1.2f, restored.current.textScale, 0.001f)
        assertEquals("طريق الملك فهد", restored.current.homeAddress)
        assertEquals("SPEED:LARGE,CLOCK:SMALL", prefs("widgets").getString("board", null))
        assertEquals(0.6f, prefs("dimmer").getFloat("level", 1f), 0.001f)
        assertTrue(prefs("nova_ai").getBoolean("nova_enabled", false))
        assertEquals(20, prefs("nova_ai").getInt("listen_timeout", 0))
        assertEquals("the unit's own key survives an import", "LOCAL-KEY", prefs("nova_ai").getString("claude_key", null))
        assertNull(prefs("nova_ai").getString("openai_key", null))
    }

    @Test fun invalidBackupsChangeNothing() {
        SettingsRepository(app).update { it.copy(uiStyle = "classic") }
        fun rejected(json: String, reason: NovaBackup.Reason) {
            assertEquals(NovaBackup.Result.Invalid(reason), NovaBackup.import(app, json))
            assertEquals("classic", SettingsRepository(app).current.uiStyle)
        }
        rejected("not json at all", NovaBackup.Reason.NOT_JSON)
        rejected("{\"app\":\"Other\",\"format\":1,\"prefs\":{}}", NovaBackup.Reason.NOT_NOVADRIVE)
        rejected("{\"app\":\"NovaDrive\",\"format\":99,\"prefs\":{}}", NovaBackup.Reason.NEWER_FORMAT)
        rejected("{\"app\":\"NovaDrive\",\"format\":1,\"prefs\":{\"nova_settings\":{\"p0.uiStyle\":{\"t\":\"i\",\"v\":\"oops\"}}}}", NovaBackup.Reason.BAD_VALUE)
        rejected("{\"app\":\"NovaDrive\",\"format\":1,\"prefs\":{\"nova_settings\":{\"p0.uiStyle\":\"bmw\"}}}", NovaBackup.Reason.BAD_VALUE)
        rejected("x".repeat(NovaBackup.MAX_BYTES + 1), NovaBackup.Reason.TOO_BIG)
        // Unknown files and secret keys inside a backup are ignored, never written.
        val odd = "{\"app\":\"NovaDrive\",\"format\":1,\"prefs\":{\"evil_file\":{\"k\":{\"t\":\"s\",\"v\":\"x\"}},\"nova_ai\":{\"openai_key\":{\"t\":\"s\",\"v\":\"INJECTED\"}}}}"
        assertTrue(NovaBackup.import(app, odd) is NovaBackup.Result.Ok)
        assertTrue(prefs("evil_file").all.isEmpty())
        assertNull(prefs("nova_ai").getString("openai_key", null))
    }

    // ---- Upgrades and profiles

    @Test fun upgradeFromOlderInstallKeepsSettingsAndGetsNewDefaults() {
        // What a 2.2.0 install has on disk: no smart slot, no scenes, no sound or widget files.
        prefs("nova_settings").edit().putString("profiles", "Driver").putInt("profile", 0)
            .putString("p0.uiStyle", "tesla").putString("p0.dock", "maps|carplay|music|phone|apps").putString("p0.lang", "AR")
            .putString("p0.carplay", "com.zjinnova.zlink").putBoolean("p0.driving", true).commit()
        val s = SettingsRepository(app).current
        assertEquals("tesla", s.uiStyle)
        assertEquals(listOf("maps", "carplay", "music", "phone", "apps"), s.dock)
        assertEquals("com.zjinnova.zlink", s.carPlayPackage)
        assertTrue(s.drivingMode)
        assertEquals("new options start at their defaults", NovaSettings().voiceReply, s.voiceReply)
        assertEquals(WidgetGrid.DEFAULT.map { it.kind }, WidgetRepository(app).items.value.map { it.kind })
        assertTrue(SceneRepository(app, SettingsRepository(app)) { WidgetRepository(app) }.list().isEmpty())
    }

    @Test fun profilesKeepIndependentSettingsAcrossRestart() {
        val a = SettingsRepository(app)
        a.update { it.copy(uiStyle = "classic", themeMode = ThemeMode.DARK) }
        a.addProfile("Guest")
        a.update { it.copy(uiStyle = "widgets", themeMode = ThemeMode.LIGHT, language = Lang.EN) }
        val b = SettingsRepository(app) // restart
        assertEquals(1, b.current.profileId)
        assertEquals("widgets", b.current.uiStyle)
        b.switchProfile(0)
        assertEquals("classic", b.current.uiStyle)
        assertEquals(ThemeMode.DARK, b.current.themeMode)
        assertEquals(Lang.AR, b.current.language)
    }

    // ---- NOVA Scenes

    @Test fun scenesSaveApplyAndStayWithTheirProfile() {
        val settings = SettingsRepository(app)
        val widgets = WidgetRepository(app)
        val scenes = SceneRepository(app, settings) { widgets }
        settings.update { it.copy(uiStyle = "widgets", homeMode = "MUSIC", themeMode = ThemeMode.DARK, dock = listOf("music", "apps")) }
        widgets.set(WidgetGrid.decode("MAP:LARGE,NAV:WIDE"))
        assertFalse("a name is required", scenes.saveCurrent("   "))
        assertTrue(scenes.saveCurrent("  Road   trip "))
        assertEquals("Road trip", scenes.list().single().name)

        settings.update { it.copy(uiStyle = "tesla", homeMode = "CAR", themeMode = ThemeMode.LIGHT, dock = NovaSettings.DEFAULT_DOCK) }
        widgets.reset()
        scenes.apply(scenes.find("road TRIP")!!)
        assertEquals("widgets", settings.current.uiStyle)
        assertEquals("MUSIC", settings.current.homeMode)
        assertEquals(ThemeMode.DARK, settings.current.themeMode)
        assertEquals(listOf("music", "apps"), settings.current.dock)
        assertEquals(listOf(WidgetKind.MAP, WidgetKind.NAV), widgets.items.value.map { it.kind })

        // Survives a restart; another profile has its own (empty) list.
        val again = SceneRepository(app, SettingsRepository(app)) { WidgetRepository(app) }
        assertEquals(1, again.list().size)
        settings.addProfile("Guest")
        assertTrue(scenes.list().isEmpty())
        settings.switchProfile(0)
        scenes.delete("Road trip")
        assertTrue(scenes.list().isEmpty())
    }

    @Test fun sceneRulesAndOldScenes() {
        fun sc(n: String, style: String = "smart") = Scene(n, style, "CAR", ThemeMode.AUTO, listOf("maps"), "")
        var list = emptyList<Scene>()
        (1..10).forEach { list = Scenes.put(list, sc("S$it")) }
        assertEquals(Scenes.MAX, list.size)
        assertEquals("the oldest make room", "S3", list.first().name)
        list = Scenes.put(list, sc("s10", "bmw"))
        assertEquals("same name replaces", Scenes.MAX, list.size)
        assertEquals("bmw", list.last().uiStyle)
        assertEquals(list, Scenes.decode(Scenes.encode(list)))
        assertTrue(Scenes.decode("garbage").isEmpty())
        // A scene saved by a version with an interface/mode this one doesn't know falls back safely.
        val applied = Scenes.applyTo(NovaSettings(), Scene("x", "hologram", "ORBIT", ThemeMode.DARK, emptyList(), ""))
        assertEquals("smart", applied.uiStyle); assertEquals("CAR", applied.homeMode); assertEquals(NovaSettings.DEFAULT_DOCK, applied.dock)
    }

    // ---- Usage, Frequent tab and Smart Dock

    @Test fun usageRanksRecentUseAndPersists() {
        var now = 1_000_000_000_000L
        val u = AppUsage(app) { now }
        repeat(5) { u.record("old/.Main") }
        now += 60L * 24 * 3600_000 // two months later
        repeat(2) { u.record("new/.Main") }
        u.record("once/.Main")
        assertEquals(listOf("new/.Main", "once/.Main", "old/.Main"), u.ranked.value)
        assertEquals("kept across restarts", u.ranked.value, AppUsage(app) { now }.ranked.value)
        u.clear()
        assertTrue(AppUsage(app) { now }.ranked.value.isEmpty())
    }

    @Test fun smartDockAndFrequentTab() {
        val maps = entry("g.maps", "Maps"); val yt = entry("com.google.android.youtube", "YouTube"); val pod = entry("pod", "Podcasts")
        val apps = listOf(maps, yt, pod)
        val frequent = listOf(maps.key, "gone/.Main", yt.key, pod.key)
        assertEquals("skips what the dock already shows", yt, smartDockApp(frequent, apps, setOf("g.maps"), driving = false))
        assertEquals("no video while driving", pod, smartDockApp(frequent, apps, setOf("g.maps"), driving = true))
        assertNull("nothing yet on a fresh install", smartDockApp(emptyList(), apps, emptySet(), false))
        assertEquals(listOf("Maps", "YouTube", "Podcasts"), filterApps(apps, AppTab.FREQUENT, emptyList(), "", false, frequent).map { it.label })
        assertEquals(listOf("Podcasts"), filterApps(apps, AppTab.FREQUENT, emptyList(), "pod", false, frequent).map { it.label })
    }

    // ---- NOVA Update Center

    @Test fun versionsCompare() {
        assertTrue(Versions.compare("2.5.0", "2.4.9") > 0)
        assertTrue(Versions.compare("v2.5.0", "2.5.0-beta3") > 0)
        assertTrue(Versions.compare("2.5.0-beta2", "2.5.0-beta1") > 0)
        assertTrue(Versions.compare("2.5.0-rc1", "2.5.0-beta9") > 0)
        assertTrue(Versions.compare("2.10.0", "2.9.9") > 0)
        assertEquals(0, Versions.compare("v2.5.0-beta1", "2.5.0-beta1"))
        assertTrue(Versions.compare("2.4.0-beta2", "2.5.0-beta1") < 0)
    }

    @Test fun onlyOfficialReleasesAreOffered() {
        fun rel(tag: String, pre: Boolean, url: String = "https://github.com/abu00salman/novaapk/releases/tag/$tag") =
            "{\"tag_name\":\"$tag\",\"name\":\"NovaDrive $tag\",\"body\":\"notes\",\"html_url\":\"$url\",\"prerelease\":$pre,\"draft\":false,\"assets\":[{\"name\":\"NovaDrive-$tag.apk\"}]}"
        val json = "[" + listOf(rel("v2.2.0", false), rel("v2.6.0-beta1", true), rel("v9.9.9", false, "https://evil.example/releases/tag/v9.9.9"),
            "{\"tag_name\":\"v3.0.0\",\"html_url\":\"https://github.com/abu00salman/novaapk/releases/tag/v3.0.0\",\"draft\":true}").joinToString(",") + "]"
        val list = UpdateCenter.parseReleases(json)
        assertEquals("drafts and other hosts are dropped", listOf("v2.2.0", "v2.6.0-beta1"), list.map { it.tag })
        assertEquals("NovaDrive-v2.2.0.apk", list[0].apk)
        assertEquals(UpdateCenter.Check.UpToDate("2.5.0-beta1"), UpdateCenter.pick(list, "2.5.0-beta1", includeBeta = false))
        val beta = UpdateCenter.pick(list, "2.5.0-beta1", includeBeta = true)
        assertEquals("v2.6.0-beta1", (beta as UpdateCenter.Check.Available).release.tag)
        assertEquals(UpdateCenter.Check.NoReleases, UpdateCenter.pick(emptyList<ReleaseInfo>(), "2.5.0", true))
        assertTrue(UpdateCenter.parseReleases("{\"message\":\"Not Found\"}").isEmpty())
    }

    @Test fun whatsNewComesFromTheBundledChangelog() {
        val (title, body) = UpdateCenter.latestSection("# Changelog\n\n## 2.5.0-beta1\n\n- **Backup** and `scenes`\n- More\n\n## 2.4.0\n- Old")!!
        assertEquals("2.5.0-beta1", title)
        assertEquals("- Backup and scenes\n- More", body)
        assertNull(UpdateCenter.latestSection("no sections"))
        val real = java.io.File("../CHANGELOG.md").readText()
        assertNotNull("the repository changelog has a section to show", UpdateCenter.latestSection(real))
        assertTrue("the changelog describes this version", UpdateCenter.latestSection(real)!!.first.startsWith(BuildConfig.VERSION_NAME))
    }

    // ---- NOVA QuickSwitch overlay

    @Test fun quickSwitchOnlyWhenEnabledAllowedAndInBackground() {
        val c = AppContainer(app)
        val qs = c.quickSwitch
        fun idle() = shadowOf(Looper.getMainLooper()).idle()
        assertFalse("off by default", qs.enabled.value)
        ShadowSettings.setCanDrawOverlays(true)
        qs.onBackground(); idle()
        assertFalse("not shown while disabled", qs.isShowing)

        qs.setEnabled(true); idle()
        assertTrue("shown over other apps", qs.isShowing)
        assertFalse("starts folded", qs.isExpanded)
        qs.setExpanded(true)
        assertTrue(qs.isExpanded)
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(8))
        assertFalse("folds back by itself", qs.isExpanded)

        qs.onForeground(); idle()
        assertFalse("never over NovaDrive itself", qs.isShowing)

        ShadowSettings.setCanDrawOverlays(false)
        qs.onBackground(); idle()
        assertFalse("needs the overlay permission", qs.isShowing)
        assertTrue("the choice is remembered", AppContainer(app).quickSwitch.enabled.value)
    }

    // ---- Diagnostics and crash recovery

    @Test fun diagnosticsReportRealStateWithoutCrashing() {
        val c = AppContainer(app)
        val items = c.health.snapshot()
        assertTrue(items.size > 15)
        assertEquals("start-up isn't invented before it is measured", "--", items.first { it.en.startsWith("Start-up") }.value)
        assertEquals("no", items.first { it.en == "Location permission" }.value)
        assertFalse("no keys or coordinates in the report", c.health.report().contains("key", ignoreCase = true))
    }

    @Test fun twoQuickCrashesEnterSafeModeAndItCanBeLeft() {
        val now = System.currentTimeMillis()
        prefs("nova_crash").edit().putString("times", "${now - 5_000},${now - 1_000}").commit()
        assertTrue(com.abu00salman.novadrive.core.CrashGuard.isSafeMode(app))
        assertTrue("optional subsystems stay off", AppContainer(app).safeMode)
        com.abu00salman.novadrive.core.CrashGuard.exitSafeMode(app)
        assertFalse(com.abu00salman.novadrive.core.CrashGuard.isSafeMode(app))
        prefs("nova_crash").edit().putString("times", "${now - 600_000},${now - 1_000}").commit()
        assertFalse("crashes far apart don't trigger it", com.abu00salman.novadrive.core.CrashGuard.isSafeMode(app))
    }
}
