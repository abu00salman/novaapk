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

private fun mp3(n: Int = 4000) = "ID3".toByteArray() + ByteArray(n) { (it % 251).toByte() }
private fun wav(n: Int = 4000) = "RIFF".toByteArray() + ByteArray(4) + "WAVE".toByteArray() + ByteArray(n)
private fun ogg(n: Int = 4000) = "OggS".toByteArray() + ByteArray(n)
private fun zipOf(vararg files: Pair<String, ByteArray>) = ByteArrayOutputStream().also { bo ->
    ZipOutputStream(bo).use { z -> files.forEach { (n, b) -> z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry() } }
}.toByteArray()

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class SoundsTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private var duration: Long? = 1_000L
    private fun repo() = SoundRepository(app) { duration }

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
        r.update("tesla") { it.copy(pack = SoundPacks.CUSTOM) }
        assertTrue(r.import("tesla", SoundEvent.TAP, "my.ogg", ByteArrayInputStream(ogg())))
        val again = repo() // a new instance = the unit restarted
        assertFalse(again.global.value.enabled)
        assertEquals(0.2f, again.global.value.volume, 0.001f)
        assertEquals(setOf(SoundEvent.TAP), again.style("bmw").off)
        assertEquals(setOf(ChimeTriggers.ARRIVAL), again.style("bmw").chime)
        assertEquals(SoundPacks.CUSTOM, again.style("tesla").pack)
        assertNotNull("imported files and their mapping persist", again.fileFor("tesla", SoundEvent.TAP))
        assertEquals("other interfaces untouched", SoundPacks.NONE, again.style("ford").pack)
        again.resetAll()
        assertTrue(again.global.value.enabled)
        assertEquals(SoundPacks.NONE, again.style("tesla").pack)
        assertNull(again.fileFor("tesla", SoundEvent.TAP))
    }

    @Test fun importsAreSavedPerInterfaceReplaceAndFixLabels() {
        val r = repo(); r.resetAll()
        assertTrue(r.import("bmw", SoundEvent.NOTIFY, "a.mp3", ByteArrayInputStream(mp3())))
        assertNotNull(r.fileFor("bmw", SoundEvent.NOTIFY))
        assertNull("not shared with other interfaces", r.fileFor("tesla", SoundEvent.NOTIFY))
        val v = r.filesVersion.value
        assertTrue("replacing keeps a single file", r.import("bmw", SoundEvent.NOTIFY, "b.wav", ByteArrayInputStream(wav())))
        assertEquals("wav", r.fileFor("bmw", SoundEvent.NOTIFY)!!.extension)
        assertTrue("the player is told to reload right away", r.filesVersion.value > v)
        assertTrue("a WAV called .mp3 is stored for what it is", r.import("bmw", SoundEvent.TAP, "liar.mp3", ByteArrayInputStream(wav())))
        assertEquals("wav", r.fileFor("bmw", SoundEvent.TAP)!!.extension)
        r.removeImported("bmw", SoundEvent.NOTIFY)
        assertNull(r.fileFor("bmw", SoundEvent.NOTIFY))
    }

    @Test fun unsupportedOrCorruptFilesAreRejectedSafely() {
        val r = repo(); r.resetAll()
        assertTrue(r.import("bmw", SoundEvent.CHIME, "keep.mp3", ByteArrayInputStream(mp3())))
        val before = r.fileFor("bmw", SoundEvent.CHIME)!!.length()
        assertFalse("not audio (PNG header)", r.import("bmw", SoundEvent.CHIME, "x.mp3", ByteArrayInputStream(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()) + ByteArray(3000))))
        assertFalse("unsupported m4a", r.import("bmw", SoundEvent.CHIME, "x.m4a", ByteArrayInputStream(ByteArray(3000) { 7 })))
        assertFalse("too big", r.import("bmw", SoundEvent.CHIME, "x.mp3", ByteArrayInputStream(mp3(4 * 1024 * 1024))))
        assertFalse("empty / tiny", r.import("bmw", SoundEvent.CHIME, "x.ogg", ByteArrayInputStream(ByteArray(10))))
        duration = null
        assertFalse("decoder can't read it", r.import("bmw", SoundEvent.CHIME, "x.mp3", ByteArrayInputStream(mp3())))
        duration = 120_000L
        assertFalse("far too long for an effect", r.import("bmw", SoundEvent.CHIME, "x.mp3", ByteArrayInputStream(mp3())))
        assertEquals("the previous sound is untouched by failed imports", before, r.fileFor("bmw", SoundEvent.CHIME)!!.length())
    }

    @Test fun stageThenCommitPlaysTheChosenFile() {
        val r = repo(); r.resetAll()
        val staged = r.stage("picked.ogg", ByteArrayInputStream(ogg()))
        assertNotNull(staged)
        assertEquals("ogg", staged!!.ext); assertEquals(1_000L, staged.durationMs)
        assertNull("nothing changes until the user confirms", r.fileFor("bmw", SoundEvent.BACK))
        assertTrue(r.commit("bmw", SoundEvent.BACK, staged))
        assertEquals("ogg", r.fileFor("bmw", SoundEvent.BACK)!!.extension)
        val second = r.stage("again.wav", ByteArrayInputStream(wav()))!!
        r.discard(second)
        assertFalse(second.file.exists())
    }

    @Test fun zipImportIsCheckedFirst() {
        val r = repo(); r.resetAll()
        val done = r.importZip("bmw", ByteArrayInputStream(zipOf(
            "07_notify.mp3" to mp3(), "08_chime.ogg" to ogg(), "README.txt" to ByteArray(50), "99_unknown.mp3" to mp3(),
            "../04_evil.mp3" to mp3(), "03_corrupt.mp3" to ByteArray(3000) { 9 },
        )))
        assertEquals(setOf(SoundEvent.NOTIFY, SoundEvent.CHIME), done.toSet())
        assertNull("path tricks are ignored", r.fileFor("bmw", SoundEvent.TAP))
        assertNull("corrupt entries are skipped", r.fileFor("bmw", SoundEvent.NAV))
        assertEquals("ogg", r.fileFor("bmw", SoundEvent.CHIME)!!.extension)
        assertTrue("garbage instead of a ZIP is harmless", r.importZip("bmw", ByteArrayInputStream(ByteArray(500) { 3 })).isEmpty())
    }

    @Test fun zipWithTooManyEntriesIsCapped() {
        val r = repo(); r.resetAll()
        val files = (1..200).map { "noise_$it.txt" to ByteArray(10) }.toTypedArray() + ("01_welcome.mp3" to mp3())
        assertTrue("the real sound sits after the entry limit", r.importZip("bmw", ByteArrayInputStream(zipOf(*files))).isEmpty())
    }

    @Test fun exportMakesAPackThatImportsBack() {
        val r = repo(); r.resetAll()
        assertTrue(r.import("bmw", SoundEvent.TAP, "mine.wav", ByteArrayInputStream(wav())))
        val out = ByteArrayOutputStream()
        val n = r.exportZip("bmw", out) { e -> if (e == SoundEvent.NAV) ByteArrayInputStream(mp3()) else null }
        assertEquals("the custom sound and the built-in one", 2, n)
        val names = java.util.zip.ZipInputStream(ByteArrayInputStream(out.toByteArray())).let { z -> generateSequence { z.nextEntry }.map { it.name }.toList() }
        assertTrue("04_tap.wav" in names && "03_nav.mp3" in names && "pack.json" in names)
        val tesla = repo()
        val back = tesla.importZip("tesla", ByteArrayInputStream(out.toByteArray()))
        assertEquals(setOf(SoundEvent.TAP, SoundEvent.NAV), back.toSet())
        assertEquals("wav", tesla.fileFor("tesla", SoundEvent.TAP)!!.extension)
    }

    @Test fun resetOneSoundOneInterfaceOrEverything() {
        val r = repo(); r.resetAll()
        r.import("bmw", SoundEvent.TAP, "a.mp3", ByteArrayInputStream(mp3())); r.import("bmw", SoundEvent.NAV, "b.mp3", ByteArrayInputStream(mp3()))
        r.import("tesla", SoundEvent.TAP, "c.mp3", ByteArrayInputStream(mp3()))
        r.update("bmw") { it.copy(pack = SoundPacks.CUSTOM, off = setOf(SoundEvent.BACK)) }
        r.removeImported("bmw", SoundEvent.TAP)
        assertNull(r.fileFor("bmw", SoundEvent.TAP)); assertNotNull(r.fileFor("bmw", SoundEvent.NAV))
        r.resetStyle("bmw")
        assertNull(r.fileFor("bmw", SoundEvent.NAV))
        assertEquals(SoundPacks.LUXURY, r.style("bmw").pack); assertTrue(r.style("bmw").off.isEmpty())
        assertNotNull("other interfaces keep theirs", r.fileFor("tesla", SoundEvent.TAP))
    }

    @Test fun oldPackImportsAreDroppedOnceWhenV4Arrives() {
        app.getSharedPreferences("sounds", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        val old = java.io.File(app.filesDir, "sounds/bmw").apply { mkdirs() }
        java.io.File(old, "notify.mp3").writeBytes(mp3())
        val r = repo()
        assertNull("the earlier 07/08 files no longer override v4", r.fileFor("bmw", SoundEvent.NOTIFY))
        assertTrue(r.import("bmw", SoundEvent.NOTIFY, "new.mp3", ByteArrayInputStream(mp3())))
        assertNotNull("a later import is kept across restarts", repo().fileFor("bmw", SoundEvent.NOTIFY))
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

    @Test fun theAppBundlesAllEightV4Sounds() {
        val raw = java.io.File("src/main/res/raw").list().orEmpty().toSet()
        assertEquals(SoundEvent.entries.map { "nova_snd_${it.key}.mp3" }.toSet(), raw)
    }
}
