package com.kidsmdm.browser.tabs

import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * Callback surface [BrowserWebViewClient]/[BrowserWebChromeClient] use to report events back to
 * whatever owns the tab (in practice, always [TabManager]) - kept as a small interface rather
 * than a direct [TabManager] reference so [WebViewFactory] doesn't need to know about
 * [TabManager]'s full API, just this narrow slice of it.
 */
interface TabEventSink {
    fun onPageStarted(tabId: TabId, url: String)
    fun onPageFinished(tabId: TabId, url: String, title: String?)
    fun onProgressChanged(tabId: TabId, progress: Int)
    fun onReceivedTitle(tabId: TabId, title: String)
    fun onNavigationStateChanged(tabId: TabId)
    fun onLoadError(tabId: TabId)

    /** [android.webkit.WebChromeClient.onCreateWindow] - return the new tab's WebView for the
     * caller to attach its transport, or null to refuse (e.g. not a real user gesture). */
    fun onRequestNewTab(isUserGesture: Boolean): WebView?
    fun onRequestCloseTab(tabId: TabId)
    fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback)
    fun onHideCustomView()
}
