package com.kidsmdm.browser

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import com.kidsmdm.browser.ui.BrowserScreen
import com.kidsmdm.browser.ui.BrowserTheme
import com.kidsmdm.browser.ui.BrowserViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: BrowserViewModel by viewModels {
        BrowserViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!viewModel.handleBackPress()) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )

        setContent {
            val isSystemDark = isSystemInDarkTheme()
            LaunchedEffect(isSystemDark) { viewModel.setSystemDark(isSystemDark) }

            BrowserTheme {
                BrowserScreen(viewModel = viewModel)
            }
        }

        // First launch: load straight into the one blank tab BrowserViewModel's init already
        // opened, rather than leaving it on the New Tab screen - this is what makes the
        // VIEW/BROWSABLE intent-filter (kids-mdm-im message links etc.) actually work, not just
        // be declared in the manifest.
        intent?.dataString?.let { url -> viewModel.onSubmitAddress(url) }
    }

    /** singleTask launchMode means an already-running instance gets this instead of a fresh
     * onCreate - open the link in a new tab rather than clobbering whatever the kid is already
     * doing, matching how every other browser handles "opened from another app while running." */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.dataString?.let { url -> viewModel.tabManager.openNewTab(url) }
    }

    override fun onPause() {
        super.onPause()
        viewModel.tabManager.activeWebView()?.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.tabManager.activeWebView()?.onResume()
    }
}
