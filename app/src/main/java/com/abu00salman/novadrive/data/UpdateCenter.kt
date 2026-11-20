package com.abu00salman.novadrive.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** A release published on NovaDrive's own GitHub repository. */
data class ReleaseInfo(val tag: String, val name: String, val notes: String, val url: String, val prerelease: Boolean, val apk: String?)

/** Version names like "2.4.0", "v2.5.0-beta1", "2.5.0-rc2": numbers first, then stable > rc > beta > alpha. */
object Versions {
    private fun parts(v: String): Pair<List<Int>, Pair<Int, Int>> {
        val clean = v.trim().removePrefix("v").removePrefix("V")
        val core = clean.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val pre = clean.substringAfter('-', "").lowercase()
        val rank = when {
            pre.isEmpty() -> 3
            pre.startsWith("rc") -> 2
            pre.startsWith("beta") -> 1
            else -> 0
        }
        return core to (rank to (pre.filter { it.isDigit() }.toIntOrNull() ?: 0))
    }

    /** Negative when [a] is older than [b], positive when newer, 0 when the same. */
    fun compare(a: String, b: String): Int {
        val (ca, pa) = parts(a); val (cb, pb) = parts(b)
        for (i in 0 until maxOf(ca.size, cb.size)) {
            val d = (ca.getOrElse(i) { 0 }).compareTo(cb.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return if (pa.first != pb.first) pa.first.compareTo(pb.first) else pa.second.compareTo(pb.second)
    }
}

/**
 * NOVA Update Center. Checks only when the user asks (no background network use), only NovaDrive's own
 * repository over HTTPS, and never downloads or installs anything itself: it shows the release and
 * opens its page.
 */
object UpdateCenter {
    const val REPO = "abu00salman/novaapk"
    private const val RELEASE_PREFIX = "https://github.com/$REPO/releases/"

    sealed class Check {
        data class UpToDate(val current: String) : Check()
        data class Available(val release: ReleaseInfo) : Check()
        /** The repository has no published release yet. */
        object NoReleases : Check()
        data class Failed(val reason: String) : Check()
    }

    fun parseReleases(json: String): List<ReleaseInfo> {
        val arr = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            if (o.optBoolean("draft")) return@mapNotNull null
            val url = o.optString("html_url")
            val tag = o.optString("tag_name")
            // Only releases of the official repository are ever shown.
            if (tag.isBlank() || !url.startsWith(RELEASE_PREFIX)) return@mapNotNull null
            val assets = o.optJSONArray("assets")
            val apk = (0 until (assets?.length() ?: 0)).mapNotNull { assets?.optJSONObject(it)?.optString("name") }.firstOrNull { it.endsWith(".apk") }
            ReleaseInfo(tag, o.optString("name").ifBlank { tag }, o.optString("body").take(4000), url, o.optBoolean("prerelease"), apk)
        }
    }

    /** The newest release newer than [current]; pre-releases only when [includeBeta]. */
    fun pick(releases: List<ReleaseInfo>, current: String, includeBeta: Boolean): Check {
        val usable = releases.filter { includeBeta || !it.prerelease }
        if (usable.isEmpty()) return Check.NoReleases
        val newest = usable.sortedWith { a, b -> Versions.compare(b.tag, a.tag) }.first()
        return if (Versions.compare(newest.tag, current) > 0) Check.Available(newest) else Check.UpToDate(current)
    }

    suspend fun check(current: String, includeBeta: Boolean): Check = withContext(Dispatchers.IO) {
        val conn = runCatching { URL("https://api.github.com/repos/$REPO/releases?per_page=15").openConnection() as HttpURLConnection }
            .getOrElse { return@withContext Check.Failed(it.javaClass.simpleName) }
        try {
            conn.connectTimeout = 10_000; conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "NovaDrive")
            val code = conn.responseCode
            if (code != 200) return@withContext Check.Failed("HTTP $code")
            pick(parseReleases(conn.inputStream.bufferedReader().use { it.readText() }), current, includeBeta)
        } catch (e: Exception) {
            Check.Failed(e.javaClass.simpleName)
        } finally { conn.disconnect() }
    }

    /** The newest section of the bundled CHANGELOG.md ("## x.y.z" up to the next "## "). */
    fun latestSection(changelog: String): Pair<String, String>? {
        val lines = changelog.lines()
        val start = lines.indexOfFirst { it.startsWith("## ") }.takeIf { it >= 0 } ?: return null
        val end = lines.drop(start + 1).indexOfFirst { it.startsWith("## ") }.let { if (it < 0) lines.size else start + 1 + it }
        val body = lines.subList(start + 1, end).joinToString("\n") { it.replace("**", "").replace("`", "") }.trim()
        return lines[start].removePrefix("## ").trim() to body
    }
}
