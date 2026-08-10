package com.kidsmdm.browser

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.kidsmdm.browser.ui.BrowserTheme

/**
 * Scaffold-stage placeholder: a single hardcoded-URL WebView in a Compose Material3 Scaffold,
 * just to get a green build before layering the real tab/journal/bookmarks/history features on
 * top - see the plan's verification order (step 1). Replaced by BrowserScreen/BrowserViewModel
 * once TabManager exists.
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
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                loadUrl("https://www.kiddle.co")
            }
        },
    )
}
