package com.abu00salman.novadrive.ui.video

import android.annotation.SuppressLint
import android.graphics.Color as AColor
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerticalSplit
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.components.Pill
import com.abu00salman.novadrive.ui.components.RoundButton
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr

/** Hosts allowed to open inside NovaDrive's YouTube view; anything else opens in its own app. */
private val YOUTUBE_HOSTS = listOf("youtube.com", "youtu.be", "ytimg.com", "googlevideo.com", "google.com", "gstatic.com", "googleusercontent.com", "ggpht.com")

fun isYouTubeUrl(url: String?): Boolean {
    val host = runCatching { android.net.Uri.parse(url ?: return false).host }.getOrNull()?.lowercase() ?: return false
    return YOUTUBE_HOSTS.any { host == it || host.endsWith(".$it") }
}

/**
 * YouTube inside NovaDrive: the official YouTube mobile website (m.youtube.com) in a WebView, so videos
 * play on the home screen instead of leaving the launcher. Parked only — the moment the car moves the
 * video is paused and covered. Links outside YouTube open in their own app.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoScreen(
    driving: Boolean,
    onHome: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenBeside: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalNova.current
    val ctx = LocalContext.current
    var web by remember { mutableStateOf<WebView?>(null) }
    var full by remember { mutableStateOf<View?>(null) }
    var fullCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val host = remember {
        FrameLayout(ctx).apply { setBackgroundColor(AColor.BLACK) }
    }
    val view = remember {
        WebView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(AColor.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            CookieManager.getInstance().setAcceptCookie(true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url.toString()
                    if (isYouTubeUrl(url)) return false
                    runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, request.url).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                // The player's full-screen button.
                override fun onShowCustomView(v: View, callback: CustomViewCallback) {
                    full?.let { host.removeView(it) }
                    host.addView(v, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    full = v; fullCallback = callback
                }
                override fun onHideCustomView() {
                    full?.let { host.removeView(it) }
                    full = null; fullCallback?.onCustomViewHidden(); fullCallback = null
                }
            }
            loadUrl(START_URL)
        }.also { host.addView(it); web = it }
    }

    // Moving: stop the picture and the sound; parked again: the page resumes, the video stays paused.
    LaunchedEffect(driving) {
        if (driving) {
            full?.let { host.removeView(it) }; full = null; fullCallback?.onCustomViewHidden(); fullCallback = null
            view.evaluateJavascript(PAUSE_JS, null)
            view.onPause()
        } else view.onResume()
    }
    DisposableEffect(Unit) {
        onDispose {
            view.evaluateJavascript(PAUSE_JS, null)
            host.removeAllViews()
            view.stopLoading(); view.destroy()
        }
    }

    Glass(modifier.fillMaxSize(), corner = 24.dp) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoundButton(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back", "رجوع"), 52.dp) { if (view.canGoBack()) view.goBack() else onHome() }
                RoundButton(Icons.Rounded.Refresh, tr("Reload", "تحديث"), 52.dp) { view.reload() }
                RoundButton(Icons.Rounded.Home, "YouTube", 52.dp) { view.loadUrl(START_URL) }
                Text("YouTube", style = NovaType.title, color = p.text, modifier = Modifier.weight(1f))
                Pill(tr("Beside NovaDrive", "بجانب NovaDrive"), p.accent, icon = Icons.Rounded.VerticalSplit) { onOpenBeside() }
                Pill(tr("Open the app", "افتح التطبيق"), p.accent2, icon = Icons.Rounded.OpenInNew) { onOpenApp() }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.Black)) {
                AndroidView({ host }, Modifier.fillMaxSize())
                if (driving) Column(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.96f)),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.VideocamOff, null, tint = Color.White, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(tr("Video is available when parked", "الفيديو متاح عند التوقف"), style = NovaType.title, color = Color.White)
                    Spacer(Modifier.width(1.dp))
                }
            }
        }
    }
}

private const val START_URL = "https://m.youtube.com/"
private const val PAUSE_JS = "document.querySelectorAll('video').forEach(function(v){v.pause();});"
