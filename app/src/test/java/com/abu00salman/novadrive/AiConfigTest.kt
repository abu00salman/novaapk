package com.abu00salman.novadrive

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abu00salman.novadrive.data.VoiceChannel
import com.abu00salman.novadrive.data.VoiceLang
import com.abu00salman.novadrive.data.ai.AiConfigRepository
import com.abu00salman.novadrive.data.ai.AiProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The chosen Nova AI provider and voice settings survive an app restart. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class AiConfigTest {
    @Test fun providerAndVoiceRestoredAfterRestart() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        AiConfigRepository(ctx).update {
            it.copy(provider = AiProvider.ANTHROPIC, claudeModel = "claude-opus-5-5", followUp = true, inAppSpeech = false,
                voice = it.voice.copy(lang = VoiceLang.ARABIC, rate = 1.2f, channel = VoiceChannel.NAVIGATION))
        }
        // A new repository = the app started again.
        val cfg = AiConfigRepository(ctx).config.value
        assertEquals(AiProvider.ANTHROPIC, cfg.provider)
        assertEquals("claude-opus-5-5", cfg.claudeModel)
        assertTrue(cfg.followUp)
        assertFalse(cfg.inAppSpeech)
        assertEquals(VoiceLang.ARABIC, cfg.voice.lang)
        assertEquals(1.2f, cfg.voice.rate, 0.001f)
        assertEquals(VoiceChannel.NAVIGATION, cfg.voice.channel)
        assertFalse("no key is ever bundled or preset", cfg.hasClaudeKey || cfg.hasOpenAiKey)
    }

    @Test fun switchingProviderKeepsOnlyOne() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val repo = AiConfigRepository(ctx)
        repo.update { it.copy(provider = AiProvider.OPENAI) }
        repo.update { it.copy(provider = AiProvider.ANTHROPIC) }
        assertEquals(AiProvider.ANTHROPIC, AiConfigRepository(ctx).config.value.provider)
    }
}
