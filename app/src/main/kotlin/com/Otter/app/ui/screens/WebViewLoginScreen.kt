package com.Otter.app.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.Otter.app.data.auth.CookieTarget
import com.Otter.app.data.auth.CookieTargetCatalog
import com.Otter.app.ui.viewmodels.LoginViewModel
import com.Otter.app.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewLoginScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onLoginComplete: () -> Unit,
    profileId: String? = null,
    targetId: String? = null,
    viewModel: LoginViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    var isLoading by remember { mutableStateOf(true) }
    var showFetchingScreen by remember { mutableStateOf(false) }
    var handled by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val settings by settingsViewModel.settings.collectAsState()

    val tid = targetId?.takeIf { it.isNotBlank() } ?: CookieTargetCatalog.TARGET_YOUTUBE

    val target: CookieTarget? by produceState<CookieTarget?>(initialValue = null, tid) {
        value = runCatching { viewModel.getTargetOnce(tid) }.getOrNull()
    }

    val loginUrl =
        remember(tid, target) {
            target?.loginUrl ?: when (tid) {
                CookieTargetCatalog.TARGET_INSTAGRAM -> "https://www.instagram.com/accounts/login/"
                CookieTargetCatalog.TARGET_TWITTER -> "https://x.com/i/flow/login"
                CookieTargetCatalog.TARGET_REDDIT -> "https://www.reddit.com/login/"
                else -> "https://accounts.google.com/signin/v2/identifier?service=youtube"
            }
        }

    fun urlMatchesAnyDomain(
        url: String,
        domains: List<String>,
    ): Boolean {
        val host = runCatching { android.net.Uri.parse(url).host.orEmpty() }.getOrDefault("")
        if (host.isBlank()) return false
        val h = host.lowercase()
        return domains.any { domain ->
            val d = domain.lowercase().removePrefix("www.").removePrefix(".")
            h == d || h.endsWith(".$d")
        }
    }

    fun hasAnyCookie(
        url: String,
        cookieNames: List<String>,
    ): Boolean {
        val raw = CookieManager.getInstance().getCookie(url).orEmpty()
        if (raw.isBlank()) return false
        return cookieNames.any { name ->
            raw.contains("$name=", ignoreCase = false)
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        // Loading screen
        if (isLoading || showFetchingScreen) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                surfaceColor,
                                                backgroundColor,
                                            ),
                                    ),
                            )
                        },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                    )
                    Text(
                        text = if (showFetchingScreen) "Extracting cookies..." else "Loading sign-in page...",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Please wait while we connect to YouTube",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // WebView (hidden when loading)
        if (!isLoading && !showFetchingScreen) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        val cm = CookieManager.getInstance()
                        // Clear all cookies to ensure fresh WebView session
                        cm.removeAllCookies(null)
                        cm.setAcceptCookie(true)
                        cm.setAcceptThirdPartyCookies(this, settings.webViewThirdPartyCookies)

                        this.settings.javaScriptEnabled = true
                        this.settings.domStorageEnabled = true
                        this.settings.userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"
                        this.settings.setSupportZoom(false)
                        this.settings.builtInZoomControls = false
                        this.settings.displayZoomControls = false
                        this.settings.defaultTextEncodingName = "utf-8"

                        // Clear cache and history to ensure fresh session
                        clearCache(true)
                        clearHistory()

                        // Make WebView transparent and blend with app theme
                        setBackgroundColor(0x00000000)

                        webViewClient =
                            object : WebViewClient() {
                                override fun onPageFinished(
                                    view: WebView?,
                                    url: String?,
                                ) {
                                    super.onPageFinished(view, url)
                                    val u = url.orEmpty()

                                    if (handled) return

                                    val shouldExport =
                                        when (tid) {
                                            CookieTargetCatalog.TARGET_YOUTUBE -> {
                                                // Wait until the user finishes Google login and reaches YouTube.
                                                val isOnYoutube =
                                                    urlMatchesAnyDomain(u, listOf("youtube.com", "music.youtube.com", "youtu.be"))
                                                // Basic signal that the session is authenticated.
                                                val hasAuthCookies =
                                                    hasAnyCookie(
                                                        "https://youtube.com",
                                                        listOf("SAPISID", "__Secure-3PAPISID", "__Secure-3PSID", "SID"),
                                                    )
                                                isOnYoutube && hasAuthCookies
                                            }
                                            CookieTargetCatalog.TARGET_INSTAGRAM -> {
                                                val isOnDomain = urlMatchesAnyDomain(u, listOf("instagram.com"))
                                                val hasAuthCookies =
                                                    hasAnyCookie(
                                                        "https://www.instagram.com",
                                                        listOf("sessionid", "ds_user_id"),
                                                    )
                                                isOnDomain && hasAuthCookies
                                            }
                                            CookieTargetCatalog.TARGET_TWITTER -> {
                                                val isOnDomain = urlMatchesAnyDomain(u, listOf("x.com", "twitter.com"))
                                                val hasAuthCookies =
                                                    hasAnyCookie(
                                                        "https://x.com",
                                                        listOf("auth_token", "ct0"),
                                                    )
                                                isOnDomain && hasAuthCookies
                                            }
                                            CookieTargetCatalog.TARGET_REDDIT -> {
                                                val isOnDomain = urlMatchesAnyDomain(u, listOf("reddit.com"))
                                                val hasAuthCookies =
                                                    hasAnyCookie(
                                                        "https://www.reddit.com",
                                                        listOf("reddit_session"),
                                                    )
                                                isOnDomain && hasAuthCookies
                                            }
                                            else -> {
                                                val t = target
                                                if (t == null) false else urlMatchesAnyDomain(u, t.domains)
                                            }
                                        }

                                    if (shouldExport) {
                                        handled = true
                                        showFetchingScreen = true
                                        viewModel.exportCookiesFromWebView(profileId, tid) {
                                            onLoginComplete()
                                        }
                                    }
                                }
                            }
                        loadUrl(loginUrl)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    LaunchedEffect(Unit) {
        // Simulate initial loading
        kotlinx.coroutines.delay(500)
        isLoading = false
    }
}
