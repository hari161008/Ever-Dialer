package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.Intent
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.zIndex
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

object DonateWebViewConfig {
    const val DEFAULT_DONATE_URL = "https://hariprabhu.com/#donate"
    const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/hariprabhu"

    var targetUrl: String = DEFAULT_DONATE_URL
    var targetTitle: String = "Donate"

    fun reset() {
        targetUrl = DEFAULT_DONATE_URL
        targetTitle = "Donate"
    }
}

@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun DonateWebViewScreen(navigator: DestinationsNavigator) {
    var webViewRef    by remember { mutableStateOf<WebView?>(null) }
    var canGoBack     by remember { mutableStateOf(false) }
    var pageLoaded    by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val currentUrl = remember { DonateWebViewConfig.targetUrl }
    val currentTitle = remember { DonateWebViewConfig.targetTitle }

    DisposableEffect(Unit) {
        onDispose {
            DonateWebViewConfig.reset()
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue   = if (pageLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label         = "webViewFade"
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceArgb  = surfaceColor.toArgb()

    BackHandler(enabled = canGoBack) { webViewRef?.goBack() }

    Box(modifier = Modifier.fillMaxSize().settingsMotionBlur()) {
        com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar(
            title = currentTitle,
            onBackClick = { if (canGoBack) webViewRef?.goBack() else navigator.navigateUp() },
            modifier = Modifier.align(Alignment.TopCenter).zIndex(10f)
        )

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
                        ): Boolean = false

                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            canGoBack  = view.canGoBack()
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            canGoBack  = view.canGoBack()
                            pageLoaded = true
                        }
                    }
                    webChromeClient = WebChromeClient()
                    loadUrl(currentUrl)
                    webViewRef = this
                }
            },
            update = { view -> webViewRef = view }
        )

        if (!pageLoaded) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
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
                        putExtra(Intent.EXTRA_TEXT, currentUrl)
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
