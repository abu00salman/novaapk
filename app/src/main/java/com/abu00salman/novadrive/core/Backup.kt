package com.abu00salman.novadrive.core

import android.content.Context
import android.content.Intent
import org.json.JSONObject

/**
 * NOVA Backup: every NovaDrive setting (all profiles, interfaces, widgets, scenes, sounds, voice
 * options) as one JSON file the user can keep or move to another unit. API keys are never exported,
 * and importing never touches the keys already saved on the unit. Photos (car image, wallpapers) and
 * imported sound files stay on the unit and are not part of the file.
 */
object NovaBackup {
    const val FORMAT = 1
    const val APP = "NovaDrive"
    const val MAX_BYTES = 2 * 1024 * 1024

    /** Preference files in a backup → keys that are never exported and never overwritten by an import. */
    val FILES: Map<String, Set<String>> = mapOf(
        "nova_settings" to emptySet(),
        "style_options" to emptySet(),
        "widgets" to emptySet(),
        "scenes" to emptySet(),
        "sounds" to emptySet(),
        "dimmer" to emptySet(),
        "app_usage" to emptySet(),
        "quickswitch" to emptySet(),
        "nova_ai" to setOf("openai_key", "claude_key", "mic_asked"),
    )

    sealed class Result {
        data class Ok(val values: Int, val fromVersion: String) : Result()
        data class Invalid(val reason: Reason) : Result()
    }

    enum class Reason { NOT_JSON, NOT_NOVADRIVE, NEWER_FORMAT, TOO_BIG, BAD_VALUE }

    fun export(ctx: Context, versionName: String, now: Long = System.currentTimeMillis()): String {
        val files = JSONObject()
        for ((name, secret) in FILES) {
            val out = JSONObject()
            for ((k, v) in ctx.getSharedPreferences(name, Context.MODE_PRIVATE).all) {
                if (k in secret || v == null) continue
                val (t, value) = when (v) {
                    is String -> "s" to v
                    is Boolean -> "b" to v
                    is Int -> "i" to v
                    is Long -> "l" to v
                    is Float -> "f" to v.toDouble()
                    else -> continue
                }
                out.put(k, JSONObject().put("t", t).put("v", value))
            }
            files.put(name, out)
        }
        return JSONObject().put("app", APP).put("format", FORMAT).put("version", versionName).put("created", now).put("prefs", files).toString(1)
    }

    /** Checks the whole file first; nothing is changed unless every value in it is valid. */
    fun import(ctx: Context, json: String): Result {
        if (json.length > MAX_BYTES) return Result.Invalid(Reason.TOO_BIG)
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return Result.Invalid(Reason.NOT_JSON)
        if (root.optString("app") != APP || !root.has("prefs")) return Result.Invalid(Reason.NOT_NOVADRIVE)
        if (root.optInt("format", 0) !in 1..FORMAT) return Result.Invalid(Reason.NEWER_FORMAT)
        val prefs = root.optJSONObject("prefs") ?: return Result.Invalid(Reason.NOT_NOVADRIVE)

        val parsed = HashMap<String, Map<String, Any>>()
        for ((name, secret) in FILES) {
            val file = prefs.optJSONObject(name) ?: continue
            val values = HashMap<String, Any>()
            for (k in file.keys()) {
                if (k in secret) continue
                val e = file.optJSONObject(k) ?: return Result.Invalid(Reason.BAD_VALUE)
                val v: Any? = when (e.optString("t")) {
                    "s" -> e.opt("v") as? String
                    "b" -> e.opt("v") as? Boolean
                    "i" -> (e.opt("v") as? Number)?.toInt()
                    "l" -> (e.opt("v") as? Number)?.toLong()
                    "f" -> (e.opt("v") as? Number)?.toFloat()
                    else -> null
                }
                values[k] = v ?: return Result.Invalid(Reason.BAD_VALUE)
            }
            parsed[name] = values
        }

        var count = 0
        for ((name, values) in parsed) {
            val sp = ctx.getSharedPreferences(name, Context.MODE_PRIVATE)
            val keep = FILES.getValue(name).mapNotNull { k -> sp.all[k]?.let { k to it } }
            val ed = sp.edit().clear()
            for ((k, v) in keep + values.toList()) when (v) {
                is String -> ed.putString(k, v)
                is Boolean -> ed.putBoolean(k, v)
                is Int -> ed.putInt(k, v)
                is Long -> ed.putLong(k, v)
                is Float -> ed.putFloat(k, v)
            }
            ed.commit()
            count += values.size
        }
        return Result.Ok(count, root.optString("version"))
    }

    /** Restarts NovaDrive so every screen reads the imported settings. */
    fun restart(ctx: Context) {
        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        if (launch != null) ctx.startActivity(Intent.makeRestartActivityTask(launch.component))
        Runtime.getRuntime().exit(0)
    }
}
