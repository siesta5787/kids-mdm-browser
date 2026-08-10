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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kidsmdm.browser.pdf.PdfExporter
import com.kidsmdm.browser.tabs.TabId
import com.kidsmdm.browser.tabs.TabManager

@Composable
fun BrowserScreen(viewModel: BrowserViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val customView by viewModel.customView.collectAsState()
    val overlay by viewModel.overlay.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val isCurrentUrlBookmarked by viewModel.isCurrentUrlBookmarked.collectAsState()
    val historyItems by viewModel.historyItems.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AddressBar(
                url = activeTab?.url.orEmpty(),
                progress = activeTab?.progress ?: 0,
                isLoading = activeTab?.isLoading ?: false,
                isBookmarked = isCurrentUrlBookmarked,
                onSubmit = viewModel::onSubmitAddress,
                onNewTab = viewModel::newTab,
                onToggleBookmark = viewModel::toggleBookmarkForCurrentTab,
                onShowBookmarks = viewModel::showBookmarks,
                onShowHistory = viewModel::showHistory,
                onSaveAsPdf = {
                    val webView = viewModel.tabManager.activeWebView() ?: return@AddressBar
                    PdfExporter.export(context, webView, activeTab?.title)
                },
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
                    NewTabScreen(
                        bookmarks = bookmarks,
                        onOpenBookmark = viewModel::onSubmitAddress,
                        modifier = Modifier.fillMaxSize(),
                    )
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

        when (overlay) {
            OverlayScreen.BOOKMARKS -> BookmarksScreen(
                bookmarks = bookmarks,
                onOpen = viewModel::openFromOverlay,
                onDelete = viewModel::deleteBookmark,
                onBack = viewModel::dismissOverlay,
                modifier = Modifier.fillMaxSize(),
            )
            OverlayScreen.HISTORY -> HistoryScreen(
                items = historyItems,
                onOpen = viewModel::openFromOverlay,
                onBack = viewModel::dismissOverlay,
                modifier = Modifier.fillMaxSize(),
            )
            OverlayScreen.NONE -> Unit
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
