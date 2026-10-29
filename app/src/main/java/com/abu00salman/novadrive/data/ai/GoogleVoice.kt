package com.abu00salman.novadrive.data.ai

import android.app.SearchManager
import android.content.Context
import android.content.Intent

/**
 * The official Google assistants, opened through their public intents — no API or key. Each call
 * returns false when the unit has no app for it.
 */
object GoogleVoice {
    const val GOOGLE_APP = "com.google.android.googlequicksearchbox"
    const val GEMINI_APP = "com.google.android.apps.bard"

    private fun start(ctx: Context, i: Intent): Boolean = runCatching {
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    }.getOrDefault(false)

    private fun installed(ctx: Context, pkg: String) =
        runCatching { ctx.packageManager.getLaunchIntentForPackage(pkg) != null }.getOrDefault(false)

    /** Google Assistant listening (the unit's voice-command handler). */
    fun assistant(ctx: Context): Boolean =
        start(ctx, Intent(Intent.ACTION_VOICE_COMMAND)) || start(ctx, Intent(Intent.ACTION_ASSIST))

    /** The Gemini app; null-safe when it isn't installed. */
    fun gemini(ctx: Context): Boolean =
        installed(ctx, GEMINI_APP) && start(ctx, ctx.packageManager.getLaunchIntentForPackage(GEMINI_APP)!!)

    fun hasGemini(ctx: Context) = installed(ctx, GEMINI_APP)
    fun hasGoogle(ctx: Context) = installed(ctx, GOOGLE_APP)

    /** Hands a question to the Google app (answers and search results). */
    fun search(ctx: Context, query: String): Boolean {
        val i = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
        return start(ctx, Intent(i).setPackage(GOOGLE_APP)) || start(ctx, i)
    }
}
