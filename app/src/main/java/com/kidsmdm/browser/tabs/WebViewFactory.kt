package com.kidsmdm.browser.tabs

import android.content.Context
import android.content.res.Configuration
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

        val webView = WebView(darkAwareContext(context, isSystemDark))
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
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            DownloadHandler.enqueue(context, url, userAgent, contentDisposition, mimeType)
        }
        return webView
    }

    /** WebView's own dark-content decision reads UI_MODE_NIGHT off whatever [Context] it was
     * constructed with - explicitly stamping that here removes any dependency on how reliably
     * the ambient app/activity Context's configuration happens to propagate, rather than trusting
     * it implicitly. */
    private fun darkAwareContext(context: Context, isSystemDark: Boolean): Context {
        val config = Configuration(context.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (isSystemDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        return context.createConfigurationContext(config)
    }
}
