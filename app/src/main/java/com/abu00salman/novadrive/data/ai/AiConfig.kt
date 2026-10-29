package com.abu00salman.novadrive.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Which engine answers what the local command parser doesn't understand. */
enum class AiProvider { LOCAL, OPENAI, ANTHROPIC }

/** What «أمرني» does when Nova AI (experimental) is off. */
enum class VoiceButton {
    /** Google speech recognition → NovaDrive's local commands. */ COMMANDS,
    /** Opens Google Assistant. */ ASSISTANT,
    /** Opens the Gemini app. */ GEMINI,
}

@Immutable
data class AiConfig(
    /** Nova AI is experimental: off until the user switches it on in Settings › Experimental. */
    val novaEnabled: Boolean = false,
    val voiceButton: VoiceButton = VoiceButton.COMMANDS,
    /** «أمرني»: a sentence that isn't a car command is handed to Google (search/answers). */
    val unknownToGoogle: Boolean = true,
    val provider: AiProvider = AiProvider.LOCAL,
    val openAiModel: String = DEFAULT_OPENAI_MODEL,
    val claudeModel: String = DEFAULT_CLAUDE_MODEL,
    /** Optional backend proxy that holds the keys (base URL); empty = call the provider directly. */
    val proxyUrl: String = "",
    val hasOpenAiKey: Boolean = false,
    val hasClaudeKey: Boolean = false,
    /** The assistant may tell the AI service the current city/coordinates. Off by default. */
    val shareLocation: Boolean = false,
    /** Speech output: language, voice, rate, volume and audio channel. */
    val voice: com.abu00salman.novadrive.data.VoicePrefs = com.abu00salman.novadrive.data.VoicePrefs(),
    /** Recognise speech in the app (true) or in the system's Google window (false). */
    val inAppSpeech: Boolean = true,
    /** Ask the recognizer to work on the device when it can (always done when there's no internet). */
    val preferOfflineSpeech: Boolean = false,
    /** Keep the last questions/answers so follow-up questions make sense. */
    val keepContext: Boolean = true,
    /** Listen again automatically after a reply has been spoken. */
    val followUp: Boolean = false,
    val listenTimeoutSec: Int = 12,
    val replyTimeoutSec: Int = 30,
) {
    companion object {
        const val DEFAULT_OPENAI_MODEL = "gpt-4o-mini"
        const val DEFAULT_CLAUDE_MODEL = "claude-opus-5-5"
    }
}

/**
 * Nova AI settings. API keys are typed in by the user on the device and stored encrypted with a
 * hardware-backed Android Keystore key — nothing is bundled in the APK.
 */
class AiConfigRepository(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("nova_ai", Context.MODE_PRIVATE)
    private val _config = MutableStateFlow(read())
    val config: StateFlow<AiConfig> = _config

    private fun read() = AiConfig(
        novaEnabled = prefs.getBoolean("nova_enabled", false),
        voiceButton = runCatching { VoiceButton.valueOf(prefs.getString("voice_button", null) ?: "COMMANDS") }.getOrDefault(VoiceButton.COMMANDS),
        unknownToGoogle = prefs.getBoolean("unknown_to_google", true),
        provider = runCatching { AiProvider.valueOf(prefs.getString("provider", null) ?: "LOCAL") }.getOrDefault(AiProvider.LOCAL),
        openAiModel = prefs.getString("openai_model", null)?.takeIf { it.isNotBlank() } ?: AiConfig.DEFAULT_OPENAI_MODEL,
        claudeModel = prefs.getString("claude_model", null)?.takeIf { it.isNotBlank() } ?: AiConfig.DEFAULT_CLAUDE_MODEL,
        proxyUrl = prefs.getString("proxy", "") ?: "",
        hasOpenAiKey = prefs.contains("openai_key"),
        hasClaudeKey = prefs.contains("claude_key"),
        shareLocation = prefs.getBoolean("share_location", false),
        voice = com.abu00salman.novadrive.data.VoicePrefs(
            lang = runCatching { com.abu00salman.novadrive.data.VoiceLang.valueOf(prefs.getString("voice_lang", null) ?: "AUTO") }.getOrDefault(com.abu00salman.novadrive.data.VoiceLang.AUTO),
            voiceName = prefs.getString("voice_name", "") ?: "",
            rate = prefs.getFloat("voice_rate", 1f),
            volume = prefs.getFloat("voice_volume", 1f),
            channel = runCatching { com.abu00salman.novadrive.data.VoiceChannel.valueOf(prefs.getString("voice_channel", null) ?: "MEDIA") }.getOrDefault(com.abu00salman.novadrive.data.VoiceChannel.MEDIA),
        ),
        inAppSpeech = prefs.getBoolean("in_app_speech", true),
        preferOfflineSpeech = prefs.getBoolean("prefer_offline_speech", false),
        keepContext = prefs.getBoolean("keep_context", true),
        followUp = prefs.getBoolean("follow_up", false),
        listenTimeoutSec = prefs.getInt("listen_timeout", 12),
        replyTimeoutSec = prefs.getInt("reply_timeout", 30),
    )

    fun update(block: (AiConfig) -> AiConfig) {
        val n = block(_config.value)
        prefs.edit().putBoolean("nova_enabled", n.novaEnabled).putString("voice_button", n.voiceButton.name)
            .putBoolean("unknown_to_google", n.unknownToGoogle)
            .putString("provider", n.provider.name).putString("openai_model", n.openAiModel)
            .putString("claude_model", n.claudeModel).putString("proxy", n.proxyUrl.trim())
            .putBoolean("share_location", n.shareLocation)
            .putString("voice_lang", n.voice.lang.name).putString("voice_name", n.voice.voiceName)
            .putFloat("voice_rate", n.voice.rate).putFloat("voice_volume", n.voice.volume).putString("voice_channel", n.voice.channel.name)
            .putBoolean("in_app_speech", n.inAppSpeech).putBoolean("prefer_offline_speech", n.preferOfflineSpeech)
            .putBoolean("keep_context", n.keepContext).putBoolean("follow_up", n.followUp)
            .putInt("listen_timeout", n.listenTimeoutSec.coerceIn(5, 30)).putInt("reply_timeout", n.replyTimeoutSec.coerceIn(10, 90))
            .apply()
        _config.value = read()
    }

    fun setKey(provider: AiProvider, key: String?) {
        val name = if (provider == AiProvider.OPENAI) "openai_key" else "claude_key"
        if (key.isNullOrBlank()) prefs.edit().remove(name).apply()
        else prefs.edit().putString(name, Secrets.encrypt(key.trim())).apply()
        _config.value = read()
    }

    fun key(provider: AiProvider): String? {
        val name = if (provider == AiProvider.OPENAI) "openai_key" else "claude_key"
        return prefs.getString(name, null)?.let { Secrets.decrypt(it) }
    }
}

/** AES-GCM with a non-exportable Android Keystore key. */
private object Secrets {
    private const val ALIAS = "nova_ai_keys"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()
        )
        return gen.generateKey()
    }

    fun encrypt(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val out = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(c.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun decrypt(stored: String): String? = runCatching {
        val (iv, data) = stored.split(':', limit = 2)
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        }
        String(c.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
    }.getOrNull()
}
