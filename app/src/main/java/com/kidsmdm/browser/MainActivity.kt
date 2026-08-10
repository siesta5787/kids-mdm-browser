package com.kidsmdm.browser

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kidsmdm.browser.journal.JournalDatabase
import com.kidsmdm.browser.ui.BrowserTheme

/**
 * Scaffold-stage placeholder: a single hardcoded-URL WebView in a Compose Material3 Scaffold,
 * just to get a green build before layering the real tab/journal/bookmarks/history features on
 * top - see the plan's verification order (steps 1-2). Wires onPageFinished -> recordVisit here
 * (pre-multi-tab) specifically to verify the journal write path end-to-end via
 * `adb shell content query --uri content://com.kidsmdm.browser.journal/entries/0` before
 * building TabManager/BrowserWebViewClient on top of it. Replaced by BrowserScreen/
 * BrowserViewModel once TabManager exists.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrowserTheme {
                Scaffold { innerPadding ->
                    PlaceholderWebView(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PlaceholderWebView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                            JournalDatabase.getInstance(context).recordVisit(url, view.title)
                        }
                    }
                }
                loadUrl("https://www.kiddle.co")
            }
        },
    )
}
