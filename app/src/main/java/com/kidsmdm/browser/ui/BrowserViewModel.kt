package com.kidsmdm.browser.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kidsmdm.browser.tabs.TabId
import com.kidsmdm.browser.tabs.TabManager
import com.kidsmdm.browser.util.UrlOrSearchResolver

/**
 * Scoped to the Activity, so [TabManager] (and the WebView instances it owns via
 * [com.kidsmdm.browser.tabs.TabWebViewHost]) survive rotation for free - see that class's own
 * doc comment for why that means process-death Bundle persistence isn't implemented yet.
 */
class BrowserViewModel(context: Context) : ViewModel() {
    val tabManager = TabManager(context.applicationContext)

    val tabs = tabManager.tabs
    val activeTabId = tabManager.activeTabId
    val customView = tabManager.customView

    init {
        if (tabManager.tabs.value.isEmpty()) {
            tabManager.openNewTab()
        }
    }

    fun onSubmitAddress(text: String) {
        val url = UrlOrSearchResolver.resolve(text)
        if (url.isEmpty()) return
        val activeId = tabManager.activeTabId.value
        if (activeId != null) {
            tabManager.loadUrl(activeId, url)
        } else {
            tabManager.openNewTab(url)
        }
    }

    fun newTab() {
        tabManager.openNewTab()
    }

    fun closeTab(tabId: TabId) {
        tabManager.closeTab(tabId)
        // Never leave the browser with literally zero tabs - land on a fresh New Tab screen
        // instead of an empty/broken state.
        if (tabManager.tabs.value.isEmpty()) {
            tabManager.openNewTab()
        }
    }

    fun switchTab(tabId: TabId) = tabManager.switchTo(tabId)

    /** True if handled (caller should not also perform the default back action). */
    fun handleBackPress(): Boolean = tabManager.handleBackPress()

    fun setSystemDark(dark: Boolean) = tabManager.setSystemDark(dark)

    override fun onCleared() {
        tabManager.destroyAll()
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer { BrowserViewModel(context) }
        }
    }
}
