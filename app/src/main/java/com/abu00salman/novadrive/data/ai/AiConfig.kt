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

@Immutable
data class AiConfig(
    val provider: AiProvider = AiProvider.LOCAL,
    val openAiModel: String = DEFAULT_OPENAI_MODEL,
    val claudeModel: String = DEFAULT_CLAUDE_MODEL,
    /** Optional backend proxy that holds the keys (base URL); empty = call the provider directly. */
    val proxyUrl: String = "",
    val hasOpenAiKey: Boolean = false,
    val hasClaudeKey: Boolean = false,
    /** The assistant may tell the AI service the current city/coordinates. Off by default. */
    val shareLocation: Boolean = false,
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
        provider = runCatching { AiProvider.valueOf(prefs.getString("provider", null) ?: "LOCAL") }.getOrDefault(AiProvider.LOCAL),
        openAiModel = prefs.getString("openai_model", null)?.takeIf { it.isNotBlank() } ?: AiConfig.DEFAULT_OPENAI_MODEL,
        claudeModel = prefs.getString("claude_model", null)?.takeIf { it.isNotBlank() } ?: AiConfig.DEFAULT_CLAUDE_MODEL,
        proxyUrl = prefs.getString("proxy", "") ?: "",
        hasOpenAiKey = prefs.contains("openai_key"),
        hasClaudeKey = prefs.contains("claude_key"),
        shareLocation = prefs.getBoolean("share_location", false),
    )

    fun update(block: (AiConfig) -> AiConfig) {
        val n = block(_config.value)
        prefs.edit().putString("provider", n.provider.name).putString("openai_model", n.openAiModel)
            .putString("claude_model", n.claudeModel).putString("proxy", n.proxyUrl.trim())
            .putBoolean("share_location", n.shareLocation).apply()
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
