package com.kidsmdm.browser.tabs

import android.content.Context
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Fullscreen-video overlay state (onShowCustomView/onHideCustomView) for the UI layer to render. */
data class CustomViewState(val view: View, val callback: WebChromeClient.CustomViewCallback)

/**
 * Top-level tab coordinator: owns tab list/active-tab state as [StateFlow]s for Compose, enforces
 * the hard tab cap, and implements [TabEventSink] to receive events from every tab's
 * [BrowserWebViewClient]/[BrowserWebChromeClient]. Delegates actual WebView instance lifecycle to
 * [TabWebViewHost].
 */
class TabManager(
    private val context: Context,
    private val host: TabWebViewHost = TabWebViewHost(),
) : TabEventSink {

    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<TabId?>(null)
    val activeTabId: StateFlow<TabId?> = _activeTabId.asStateFlow()

    private val _customView = MutableStateFlow<CustomViewState?>(null)
    val customView: StateFlow<CustomViewState?> = _customView.asStateFlow()

    private var nextId = 1L
    private var isSystemDark = false

    fun setSystemDark(dark: Boolean) {
        isSystemDark = dark
    }

    fun webViewHost(): TabWebViewHost = host

    /** Returns null (and opens nothing) once [MAX_TABS] is already open - unbounded tabs risk
     * real memory pressure on this fleet's hardware, see CLAUDE.md/the plan. */
    fun openNewTab(url: String? = null): TabId? {
        if (_tabs.value.size >= MAX_TABS) return null
        val id = TabId(nextId++)
        val webView = WebViewFactory.create(context, id, this, isSystemDark)
        host.put(id, webView)
        _tabs.update { it + BrowserTab(id = id) }
        _activeTabId.value = id
        if (url != null) loadUrl(id, url)
        return id
    }

    fun closeTab(tabId: TabId) {
        host.remove(tabId)
        val remaining = _tabs.value.filterNot { it.id == tabId }
        _tabs.value = remaining
        if (_activeTabId.value == tabId) {
            _activeTabId.value = remaining.lastOrNull()?.id
        }
    }

    fun switchTo(tabId: TabId) {
        if (_tabs.value.any { it.id == tabId }) _activeTabId.value = tabId
    }

    fun loadUrl(tabId: TabId, url: String) {
        host.get(tabId)?.loadUrl(url)
    }

    fun activeWebView(): WebView? = _activeTabId.value?.let { host.get(it) }

    /** System back-gesture handling: back within the active tab's own history first, then close
     * the tab (if others remain open) rather than exiting the app, mirroring how every other
     * browser's back button behaves. Returns false only when there's truly nowhere left to go
     * (last tab, no back history) - the caller should let the system handle back normally then. */
    fun handleBackPress(): Boolean {
        val id = _activeTabId.value ?: return false
        val webView = host.get(id)
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        if (_tabs.value.size > 1) {
            closeTab(id)
            return true
        }
        return false
    }

    fun destroyAll() {
        host.destroyAll()
    }

    // --- TabEventSink ---

    override fun onPageStarted(tabId: TabId, url: String) {
        updateTab(tabId) { it.copy(url = url, isLoading = true, isBlockedOrErrored = false) }
    }

    override fun onPageFinished(tabId: TabId, url: String, title: String?) {
        updateTab(tabId) { it.copy(url = url, title = title ?: it.title, isLoading = false) }
    }

    override fun onProgressChanged(tabId: TabId, progress: Int) {
        updateTab(tabId) { it.copy(progress = progress, isLoading = progress < 100) }
    }

    override fun onReceivedTitle(tabId: TabId, title: String) {
        updateTab(tabId) { it.copy(title = title) }
    }

    override fun onNavigationStateChanged(tabId: TabId) {
        val webView = host.get(tabId) ?: return
        updateTab(tabId) {
            it.copy(canGoBack = webView.canGoBack(), canGoForward = webView.canGoForward())
        }
    }

    override fun onLoadError(tabId: TabId) {
        updateTab(tabId) { it.copy(isLoading = false, isBlockedOrErrored = true) }
    }

    override fun onRequestNewTab(isUserGesture: Boolean): WebView? {
        if (!isUserGesture) return null
        val id = openNewTab() ?: return null
        return host.get(id)
    }

    override fun onRequestCloseTab(tabId: TabId) {
        closeTab(tabId)
    }

    override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        _customView.value = CustomViewState(view, callback)
    }

    override fun onHideCustomView() {
        _customView.value = null
    }

    private fun updateTab(tabId: TabId, transform: (BrowserTab) -> BrowserTab) {
        _tabs.update { list -> list.map { if (it.id == tabId) transform(it) else it } }
    }

    companion object {
        const val MAX_TABS = 6
    }
}
