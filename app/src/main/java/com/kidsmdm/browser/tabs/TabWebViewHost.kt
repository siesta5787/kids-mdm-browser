package com.kidsmdm.browser.tabs

import android.view.ViewGroup
import android.webkit.WebView
import com.kidsmdm.browser.util.DarkModeWebViewConfigurator

/**
 * Owns the actual `Map<TabId, WebView>` - the one class allowed to touch WebView instances
 * directly; everything else (Compose UI, [TabManager]) goes through [BrowserTab]'s
 * Compose-state-friendly metadata instead.
 *
 * Scoping note: as long as this (and [TabManager]) live inside a `ViewModel`, WebView instances
 * survive rotation for free - a ViewModel isn't recreated on a config change, so there's no need
 * for [WebView.saveState]/[WebView.restoreState] Bundle round-tripping for that case. Full
 * process-death survival (the app getting killed entirely in the background) would need that -
 * not implemented yet, deliberately deferred; a process-death loses open tabs today, same as
 * most simple browsers do in practice for anything beyond a single "last URL" restore.
 */
class TabWebViewHost {
    private val webViews = mutableMapOf<TabId, WebView>()
    private var attachedContainer: ViewGroup? = null
    private var attachedTabId: TabId? = null

    fun put(tabId: TabId, webView: WebView) {
        webViews[tabId] = webView
    }

    fun get(tabId: TabId): WebView? = webViews[tabId]

    fun remove(tabId: TabId) {
        val webView = webViews.remove(tabId) ?: return
        if (attachedTabId == tabId) detach()
        webView.stopLoading()
        webView.destroy()
    }

    fun attach(container: ViewGroup, tabId: TabId) {
        if (attachedTabId == tabId && attachedContainer === container) return
        detach()
        val webView = webViews[tabId] ?: return
        (webView.parent as? ViewGroup)?.removeView(webView)
        container.addView(
            webView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        webView.onResume()
        webView.resumeTimers()
        attachedContainer = container
        attachedTabId = tabId
    }

    fun detach() {
        val tabId = attachedTabId ?: return
        val webView = webViews[tabId] ?: return
        webView.onPause()
        webView.pauseTimers()
        (webView.parent as? ViewGroup)?.removeView(webView)
        attachedTabId = null
        attachedContainer = null
    }

    fun destroyAll() {
        detach()
        webViews.values.forEach { it.destroy() }
        webViews.clear()
    }

    /** Re-applies content dark-mode to every already-open tab, not just tabs created after a
     * system theme change - [WebViewFactory] only sets this once, at WebView construction time. */
    fun updateDarkMode(isDark: Boolean) {
        webViews.values.forEach { webView ->
            DarkModeWebViewConfigurator.apply(webView.settings, isDark)
        }
    }
}
