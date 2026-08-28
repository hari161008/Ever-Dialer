package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

private const val MORE_APPS_URL = "https://hariprabhu.com"

private fun downloadFile(context: Context, url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
    try {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setMimeType(mimeType)
            if (userAgent != null) addRequestHeader("User-Agent", userAgent)
            setTitle(fileName)
            setDescription("Downloading $fileName")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(context, "Downloading $fileName…", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "Couldn't start the download", Toast.LENGTH_SHORT).show()
    }
}

@Destination<RootGraph>
@Composable
fun MoreAppsWebViewScreen(navigator: DestinationsNavigator) {
    var webViewRef    by remember { mutableStateOf<WebView?>(null) }
    var canGoBack     by remember { mutableStateOf(false) }
    var pageLoaded    by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Smooth fade-in once the page finishes loading — eliminates white flash
    val contentAlpha by animateFloatAsState(
        targetValue   = if (pageLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label         = "webViewFade"
    )

    // Background that matches the app surface — shown while WebView loads
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceArgb  = surfaceColor.toArgb()

    BackHandler(enabled = canGoBack) { webViewRef?.goBack() }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── WebView ────────────────────────────────────────────────────────
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .alpha(contentAlpha),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Match app background so there is no white blink before paint
                    setBackgroundColor(surfaceArgb)
                    settings.apply {
                        javaScriptEnabled  = true
                        domStorageEnabled  = true
                        loadWithOverviewMode = true
                        useWideViewPort    = true
                        setSupportZoom(true)
                        builtInZoomControls  = true
                        displayZoomControls  = false
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView, request: WebResourceRequest
                        ): Boolean = false          // stay inside app

                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            canGoBack  = view.canGoBack()
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            canGoBack  = view.canGoBack()
                            pageLoaded = true
                        }
                    }
                    webChromeClient = WebChromeClient()
                    // Lets the site's APK download links/buttons actually save
                    // the file into the device's Downloads folder.
                    setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                        downloadFile(ctx, url, userAgent, contentDisposition, mimeType)
                    }
                    loadUrl(MORE_APPS_URL)
                    webViewRef = this
                }
            },
            update = { view -> webViewRef = view }
        )

        // ── Loading spinner — visible until fade completes ─────────────────
        if (!pageLoaded) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // ── Floating circular back and share buttons — bottom centre ──
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 90.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = {
                    if (canGoBack) webViewRef?.goBack() else navigator.navigateUp()
                },
                shape          = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation      = FloatingActionButtonDefaults.elevation(6.dp),
                modifier       = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp)
                )
            }

            FloatingActionButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, MORE_APPS_URL)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share"))
                },
                shape          = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation      = FloatingActionButtonDefaults.elevation(6.dp),
                modifier       = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Share",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
