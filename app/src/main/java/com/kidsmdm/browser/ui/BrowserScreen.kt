package com.kidsmdm.browser.ui

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.kidsmdm.browser.tabs.TabId
import com.kidsmdm.browser.tabs.TabManager

@Composable
fun BrowserScreen(viewModel: BrowserViewModel, modifier: Modifier = Modifier) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val customView by viewModel.customView.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AddressBar(
                url = activeTab?.url.orEmpty(),
                progress = activeTab?.progress ?: 0,
                isLoading = activeTab?.isLoading ?: false,
                onSubmit = viewModel::onSubmitAddress,
                onNewTab = viewModel::newTab,
                onMenu = { /* overflow menu (bookmark toggle, PDF, share, history) - task 13 */ },
            )
            TabStrip(
                tabs = tabs,
                activeTabId = activeTabId,
                onSelect = viewModel::switchTab,
                onClose = viewModel::closeTab,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (activeTab != null && activeTab.url.isNotBlank()) {
                    WebViewContainer(tabManager = viewModel.tabManager, tabId = activeTab.id)
                } else {
                    NewTabScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }

        // Fullscreen video overlay (onShowCustomView/onHideCustomView) - drawn above everything
        // else while active, same z-order approach every browser uses for this.
        customView?.let { state ->
            AndroidView(
                factory = { state.view },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun WebViewContainer(tabManager: TabManager, tabId: TabId, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context -> FrameLayout(context) },
        update = { container -> tabManager.webViewHost().attach(container, tabId) },
    )
}
