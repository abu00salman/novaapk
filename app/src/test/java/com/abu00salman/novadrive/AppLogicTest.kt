package com.abu00salman.novadrive

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.abu00salman.novadrive.core.AppTab
import com.abu00salman.novadrive.core.filterApps
import com.abu00salman.novadrive.core.isVideoApp
import com.abu00salman.novadrive.core.replaceDockEntry
import com.abu00salman.novadrive.core.resolveDock
import com.abu00salman.novadrive.data.AppCategory
import com.abu00salman.novadrive.data.AppClassifier
import com.abu00salman.novadrive.data.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class AppLogicTest {
    private val icon by lazy { ImageBitmap(1, 1) }
    private fun app(pkg: String, label: String, cat: AppCategory, system: Boolean = false) =
        AppEntry("$pkg/.Main", pkg, ".Main", label, icon, cat, system)

    @Test fun classifierRecognisesCarBoxAndCategories() {
        assertEquals(AppCategory.CAR, AppClassifier.classify("com.zjinnova.zlink", "ZLink", -1))
        assertEquals(AppCategory.NAVIGATION, AppClassifier.classify("com.waze", "Waze", -1))
        assertEquals(AppCategory.MEDIA, AppClassifier.classify("com.spotify.music", "Spotify", -1))
        assertEquals(AppCategory.TOOLS, AppClassifier.classify("com.android.settings", "Settings", -1))
        assertFalse(AppClassifier.isHiddenSystem(true, AppCategory.CAR))
        assertTrue(AppClassifier.isHiddenSystem(true, AppCategory.TOOLS))
    }

    @Test fun dockDropsDuplicatesUnknownAndVideoWhileDriving() {
        val pkg = mapOf("maps" to "g.maps", "carplay" to "zlink", "auto" to "zlink", "youtube" to "yt", "music" to "spotify")
        val parked = resolveDock(listOf("maps", "carplay", "auto", "youtube", "bogus", "maps", "music"), driving = false, packageOf = { pkg[it] }) { it }
        assertEquals(listOf("maps", "carplay", "youtube", "music"), parked)
        val driving = resolveDock(listOf("maps", "youtube", "netflix", "apps"), driving = true, packageOf = { pkg[it] }) { it }
        assertEquals(listOf("maps", "apps"), driving)
    }

    @Test fun dockCustomAppsReplaceSlotsWithoutDuplicates() {
        val d = replaceDockEntry(listOf("maps", "carplay", "music"), 2, "com.anghami")
        assertEquals(listOf("maps", "carplay", "app:com.anghami"), d)
        // the same app chosen again for another slot moves rather than duplicates
        assertEquals(listOf("app:com.anghami", "carplay"), replaceDockEntry(d, 0, "com.anghami"))
        // appended when the index is past the end, but never beyond six entries
        assertEquals(6, replaceDockEntry(listOf("a", "b", "c", "d", "e", "f"), 9, "x").size)
        val resolved = resolveDock(listOf("maps", "app:com.anghami", "app:com.youtube.video"), driving = true, packageOf = { if (it.startsWith("app:")) it.removePrefix("app:") else "g.maps" }) { it }
        assertEquals(listOf("maps", "app:com.anghami"), resolved)
    }

    @Test fun dockIsCappedAtSix() {
        val all = listOf("maps", "carplay", "auto", "music", "phone", "radio", "youtube", "netflix", "apps")
        assertEquals(6, resolveDock(all, false, { null }) { it }.size)
    }

    @Test fun filterByTabFavoritesSearchAndSystem() {
        val apps = listOf(
            app("g.maps", "Google Maps", AppCategory.NAVIGATION),
            app("spotify", "Spotify", AppCategory.MEDIA),
            app("zlink", "ZLink", AppCategory.CAR),
            app("calc", "Calculator", AppCategory.TOOLS, system = true),
        )
        assertEquals(listOf("Spotify", "Google Maps"), filterApps(apps, AppTab.FAVORITES, listOf("spotify/.Main", "g.maps/.Main"), "", false).map { it.label })
        assertEquals(3, filterApps(apps, AppTab.ALL, emptyList(), "", showSystem = false).size)
        assertEquals(4, filterApps(apps, AppTab.ALL, emptyList(), "", showSystem = true).size)
        assertEquals(listOf("ZLink"), filterApps(apps, AppTab.CAR, emptyList(), "", false).map { it.label })
        assertEquals(listOf("Spotify"), filterApps(apps, AppTab.ALL, emptyList(), "spo", false).map { it.label })
    }

    @Test fun voiceNormalisationMatchesArabicVariants() {
        assertEquals("افتح اليوتيوب", com.abu00salman.novadrive.data.VoiceCommands.normalize("إفتح اليُوتيوب من فضلك"))
        assertEquals("قلل الاضاءه", com.abu00salman.novadrive.data.VoiceCommands.normalize("قلّل الإضاءة"))
        assertEquals("كم الساعه", com.abu00salman.novadrive.data.VoiceCommands.normalize("كم الساعة"))
    }

    @Test fun videoDetection() {
        assertTrue(isVideoApp("com.google.android.youtube", "YouTube"))
        assertTrue(isVideoApp("com.netflix.mediaclient", "Netflix"))
        assertFalse(isVideoApp("com.google.android.apps.youtube.music", "YouTube Music"))
        assertFalse(isVideoApp("com.spotify.music", "Spotify"))
    }
}
