package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

private const val RATINGS_URL =
    "https://hari161008.github.io/Website-For-Everlasting-Android-Tweak/Ratings%20Reviews/Ever%20Dialer/Ever%20Dialer.html"

@Destination<RootGraph>
@Composable
fun RatingsWebViewScreen(navigator: DestinationsNavigator) {
    var webViewRef    by remember { mutableStateOf<WebView?>(null) }
    var canGoBack     by remember { mutableStateOf(false) }
    var pageLoaded    by remember { mutableStateOf(false) }

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
                    loadUrl(RATINGS_URL)
                    webViewRef = this
                }
            },
            update = { view -> webViewRef = view }
        )

        // ── Loading spinner — visible until fade completes ─────────────────
        if (!pageLoaded) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // ── Floating circular back button — bottom centre ──────────────────
        FloatingActionButton(
            onClick = {
                if (canGoBack) webViewRef?.goBack() else navigator.navigateUp()
            },
            shape             = CircleShape,
            containerColor    = MaterialTheme.colorScheme.primaryContainer,
            contentColor      = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation         = FloatingActionButtonDefaults.elevation(6.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .size(56.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
