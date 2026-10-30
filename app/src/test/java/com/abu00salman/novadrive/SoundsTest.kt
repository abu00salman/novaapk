package com.abu00salman.novadrive

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abu00salman.novadrive.core.ChimeTriggers
import com.abu00salman.novadrive.core.GlobalSounds
import com.abu00salman.novadrive.core.PlayContext
import com.abu00salman.novadrive.core.SoundEvent
import com.abu00salman.novadrive.core.SoundPacks
import com.abu00salman.novadrive.core.SoundPolicy
import com.abu00salman.novadrive.core.SoundRepository
import com.abu00salman.novadrive.core.StyleSounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val calm = PlayContext(driving = false, inCall = false, speaking = false, listening = false)
private val bmw = StyleSounds(SoundPacks.LUXURY)
private val on = GlobalSounds(true, 0.45f)

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class SoundsTest {
    private fun repo() = SoundRepository(ApplicationProvider.getApplicationContext<Application>())
    private fun fakeAudio(n: Int = 4000) = ByteArray(n) { (it % 251).toByte() }

    @Test fun bmwDefaultsToLuxuryOthersSilent() {
        val r = repo(); r.resetAll()
        assertEquals(SoundPacks.LUXURY, r.style("bmw").pack)
        listOf("tesla", "mercedes", "ford", "apple", "jetour", "byd", "haval", "tank", "classic", "smart", "simple", "widgets").forEach {
            assertEquals("$it stays silent by default", SoundPacks.NONE, r.style(it).pack)
        }
        assertTrue("quiet by default", r.global.value.volume < 0.6f)
        assertTrue("the chime never plays by itself", r.style("bmw").chime.isEmpty())
    }

    @Test fun settingsSurviveRestartAndStayPerInterface() {
        val r = repo(); r.resetAll()
        r.setGlobal { it.copy(enabled = false, volume = 0.2f) }
        r.update("bmw") { it.copy(off = setOf(SoundEvent.TAP), chime = setOf(ChimeTriggers.ARRIVAL)) }
        r.update("tesla") { it.copy(pack = SoundPacks.LUXURY) }
        val again = repo() // a new instance = the unit restarted
        assertFalse(again.global.value.enabled)
        assertEquals(0.2f, again.global.value.volume, 0.001f)
        assertEquals(setOf(SoundEvent.TAP), again.style("bmw").off)
        assertEquals(setOf(ChimeTriggers.ARRIVAL), again.style("bmw").chime)
        assertEquals(SoundPacks.LUXURY, again.style("tesla").pack)
        assertEquals("other interfaces untouched", SoundPacks.NONE, again.style("ford").pack)
        assertTrue(again.style("ford").off.isEmpty())
        again.resetAll()
        assertTrue(again.global.value.enabled)
        assertEquals(SoundPacks.NONE, again.style("tesla").pack)
    }

    @Test fun importsAreSavedPerInterfaceAndValidated() {
        val r = repo(); r.resetAll()
        assertTrue(r.import("bmw", SoundEvent.NOTIFY, "mp3", ByteArrayInputStream(fakeAudio())))
        assertNotNull(r.fileFor("bmw", SoundEvent.NOTIFY))
        assertNull("not shared with other interfaces", r.fileFor("tesla", SoundEvent.NOTIFY))
        assertTrue("replacing keeps a single file", r.import("bmw", SoundEvent.NOTIFY, "wav", ByteArrayInputStream(fakeAudio())))
        assertEquals("wav", r.fileFor("bmw", SoundEvent.NOTIFY)!!.extension)
        assertFalse("only audio", r.import("bmw", SoundEvent.CHIME, "exe", ByteArrayInputStream(fakeAudio())))
        assertFalse("too big", r.import("bmw", SoundEvent.CHIME, "mp3", ByteArrayInputStream(fakeAudio(4 * 1024 * 1024))))
        assertFalse("empty", r.import("bmw", SoundEvent.CHIME, "ogg", ByteArrayInputStream(ByteArray(10))))
        assertNull(r.fileFor("bmw", SoundEvent.CHIME))
        r.removeImported("bmw", SoundEvent.NOTIFY)
        assertNull(r.fileFor("bmw", SoundEvent.NOTIFY))
    }

    @Test fun zipImportMapsByNumber() {
        val r = repo(); r.resetAll()
        val bytes = ByteArrayOutputStream().also { bo ->
            ZipOutputStream(bo).use { z ->
                listOf("07_BMW_Notification_User.mp3", "08_BMW_Chime_User.mp3", "README.txt", "99_unknown.mp3").forEach {
                    z.putNextEntry(ZipEntry(it)); z.write(fakeAudio()); z.closeEntry()
                }
            }
        }.toByteArray()
        val done = r.importZip("bmw", ByteArrayInputStream(bytes))
        assertEquals(setOf(SoundEvent.NOTIFY, SoundEvent.CHIME), done.toSet())
        assertEquals("mp3", r.fileFor("bmw", SoundEvent.CHIME)!!.extension)
    }

    // ---- policy (pure)

    @Test fun nothingPlaysWhenOffOrSilentOrBusy() {
        assertTrue(SoundPolicy.allowed(SoundEvent.TAP, on, bmw, calm))
        assertFalse("master switch", SoundPolicy.allowed(SoundEvent.TAP, GlobalSounds(false, 0.5f), bmw, calm))
        assertFalse("zero volume", SoundPolicy.allowed(SoundEvent.TAP, GlobalSounds(true, 0f), bmw, calm))
        assertFalse("interface without a pack", SoundPolicy.allowed(SoundEvent.TAP, on, StyleSounds(SoundPacks.NONE), calm))
        assertFalse("event switched off", SoundPolicy.allowed(SoundEvent.TAP, on, bmw.copy(off = setOf(SoundEvent.TAP)), calm))
        assertFalse("phone call", SoundPolicy.allowed(SoundEvent.NAV, on, bmw, calm.copy(inCall = true)))
        assertFalse("while guidance or Nova speaks", SoundPolicy.allowed(SoundEvent.NAV, on, bmw, calm.copy(speaking = true)))
        assertFalse("while Nova listens", SoundPolicy.allowed(SoundEvent.TAP, on, bmw, calm.copy(listening = true)))
    }

    @Test fun startupCuesStayQuietWhileDriving() {
        val moving = calm.copy(driving = true)
        assertFalse(SoundPolicy.allowed(SoundEvent.WELCOME, on, bmw, moving))
        assertFalse(SoundPolicy.allowed(SoundEvent.READY, on, bmw, moving))
        assertTrue("taps still click", SoundPolicy.allowed(SoundEvent.TAP, on, bmw, moving))
    }

    @Test fun welcomeOncePerSessionNeverBothCues() {
        assertEquals(SoundEvent.WELCOME, SoundPolicy.startupCue(bmw, alreadyPlayed = false))
        assertNull("not again in the same session", SoundPolicy.startupCue(bmw, alreadyPlayed = true))
        assertEquals("Welcome off → Start/Ready instead", SoundEvent.READY, SoundPolicy.startupCue(bmw.copy(off = setOf(SoundEvent.WELCOME)), false))
        assertNull(SoundPolicy.startupCue(bmw.copy(off = setOf(SoundEvent.WELCOME, SoundEvent.READY)), false))
    }

    @Test fun rateLimitsAndFileNames() {
        assertTrue(SoundPolicy.minGapMs(SoundEvent.TAP) in 100..300)
        assertTrue("the chime is never spammed", SoundPolicy.minGapMs(SoundEvent.CHIME) >= 5_000)
        assertEquals(SoundEvent.NOTIFY, SoundPolicy.eventForFileName("07_BMW_Notification_User.mp3"))
        assertEquals(SoundEvent.CHIME, SoundPolicy.eventForFileName("folder/08_BMW_Chime_User.mp3"))
        assertEquals(SoundEvent.WELCOME, SoundPolicy.eventForFileName("1-welcome.ogg"))
        assertNull(SoundPolicy.eventForFileName("README.txt"))
        assertNull(SoundPolicy.eventForFileName("09_extra.mp3"))
        assertTrue(SoundPolicy.isAudioName("a.MP3") && SoundPolicy.isAudioName("a.wav") && SoundPolicy.isAudioName("a.ogg"))
        assertFalse(SoundPolicy.isAudioName("a.m4a"))
    }

    @Test fun publicPackIncludesOnlyTheOriginalSounds() {
        val raw = java.io.File("src/main/res/raw").list().orEmpty().toSet()
        assertEquals(setOf("welcome", "ready", "nav", "tap", "confirm", "back").map { "nova_snd_$it.mp3" }.toSet(), raw)
    }
}
