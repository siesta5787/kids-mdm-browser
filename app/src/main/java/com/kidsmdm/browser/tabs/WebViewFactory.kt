package com.kidsmdm.browser.tabs

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import com.kidsmdm.browser.BuildConfig
import com.kidsmdm.browser.util.DarkModeWebViewConfigurator

/**
 * Single place constructing and configuring every [WebView] in this app - one audit point for
 * the whole hardening checklist, instead of settings scattered per call site.
 */
object WebViewFactory {
    @Suppress("SetJavaScriptEnabled")
    fun create(context: Context, tabId: TabId, sink: TabEventSink, isSystemDark: Boolean): WebView {
        // Never true in release builds - deliberate, not just "happens to default false" (see
        // the old Chromium-fork's DeveloperToolsAvailability policy for why dev tools access is
        // treated as a lockdown item on this product).
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        val webView = WebView(context)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // No local/content file access from web content - nothing in this app's feature
            // set needs it, and it's a real exfiltration vector if left on.
            allowFileAccess = false
            allowContentAccess = false
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true // gated at the WebChromeClient layer by isUserGesture instead
            DarkModeWebViewConfigurator.apply(this, isSystemDark)
        }

        CookieManager.getInstance().let { cookies ->
            cookies.setAcceptCookie(true)
            cookies.setAcceptThirdPartyCookies(webView, false)
        }

        webView.webViewClient = BrowserWebViewClient(context, tabId, sink)
        webView.webChromeClient = BrowserWebChromeClient(tabId, sink)
        return webView
    }
}
