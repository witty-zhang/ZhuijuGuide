package com.lzzhang.zhuijuguide.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * APP 内的网页播放页：点开一个在线影视站点后，站点的搜索、选集和播放全部在这里完成，
 * 不再跳转到系统浏览器。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(
    title: String,
    url: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var progress by remember { mutableIntStateOf(0) }
    var pageTitle by remember { mutableStateOf(title) }
    var error by remember { mutableStateOf<String?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }

    val holder = remember(url) {
        WebViewHolder(
            context = context,
            startUrl = url,
            callbacks = object : WebViewHolder.Callbacks {
                override fun onProgress(percent: Int) {
                    progress = percent
                }

                override fun onTitle(text: String?) {
                    if (!text.isNullOrBlank()) pageTitle = text
                }

                override fun onError(message: String?) {
                    error = message
                }

                override fun onFullscreen(fullscreen: Boolean) {
                    isFullscreen = fullscreen
                }
            },
        )
    }

    DisposableEffect(holder) {
        onDispose { holder.release() }
    }

    DisposableEffect(lifecycleOwner, holder) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> holder.webView.onPause()
                Lifecycle.Event.ON_RESUME -> holder.webView.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        when {
            holder.isFullscreen() -> holder.exitFullscreen()
            holder.webView.canGoBack() -> holder.webView.goBack()
            else -> onBack()
        }
    }

    val currentUrl = holder.webView.url?.takeIf { it.startsWith("http") } ?: url

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = pageTitle.ifBlank { title },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = Uri.parse(currentUrl).host ?: url,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { openInBrowser(context, currentUrl) }) {
                            Icon(Icons.Rounded.OpenInBrowser, contentDescription = "用浏览器打开")
                        }
                        IconButton(onClick = { holder.webView.reload() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "重新加载")
                        }
                    },
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding),
        ) {
            AndroidView(
                factory = { holder },
                modifier = Modifier.fillMaxSize(),
            )
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                )
            }
            error?.let { message ->
                WebErrorState(
                    message = message,
                    onRetry = {
                        error = null
                        holder.webView.reload()
                    },
                    onOpenExternal = { openInBrowser(context, currentUrl) },
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private class WebViewHolder(
    context: Context,
    startUrl: String,
    private val callbacks: Callbacks,
) : FrameLayout(context) {

    interface Callbacks {
        fun onProgress(percent: Int)
        fun onTitle(text: String?)
        fun onError(message: String?)
        fun onFullscreen(fullscreen: Boolean)
    }

    val webView: WebView = WebView(context)

    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    private val chromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            callbacks.onProgress(newProgress)
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            callbacks.onTitle(title)
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            if (view == null || callback == null) return
            if (fullscreenView != null) {
                callback.onCustomViewHidden()
                return
            }
            fullscreenView = view
            fullscreenCallback = callback
            addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            callbacks.onFullscreen(true)
        }

        override fun onHideCustomView() {
            exitFullscreen()
        }
    }

    private val webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
        ): Boolean {
            val scheme = request?.url?.scheme.orEmpty()
            // 站点的推广链接常用自定义 scheme，留在本页处理，避免跳出 APP
            return scheme != "http" && scheme != "https"
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            callbacks.onError(null)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
        ) {
            if (request?.isForMainFrame == true) {
                callbacks.onError(error?.description?.toString() ?: "网页加载失败")
            }
        }
    }

    init {
        setBackgroundColor(android.graphics.Color.BLACK)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            javaScriptCanOpenWindowsAutomatically = true
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportMultipleWindows(false)
        }
        webView.webChromeClient = chromeClient
        webView.webViewClient = webViewClient
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        webView.loadUrl(startUrl)
    }

    fun isFullscreen(): Boolean = fullscreenView != null

    fun exitFullscreen() {
        fullscreenView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        fullscreenView = null
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
        callbacks.onFullscreen(false)
    }

    fun release() {
        exitFullscreen()
        webView.stopLoading()
        webView.loadUrl("about:blank")
        removeAllViews()
        webView.destroy()
    }
}

@Composable
private fun WebErrorState(
    message: String,
    onRetry: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
        Text("网页打不开", style = MaterialTheme.typography.titleMedium)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRetry) { Text("重试") }
        TextButton(onClick = onOpenExternal) { Text("用系统浏览器打开") }
    }
}

private fun openInBrowser(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}
