package com.kidsmdm.browser.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kidsmdm.browser.bookmarks.BookmarkEntity
import com.kidsmdm.browser.bookmarks.BookmarkRepository
import com.kidsmdm.browser.history.HistoryItem
import com.kidsmdm.browser.journal.JournalDatabase
import com.kidsmdm.browser.tabs.TabId
import com.kidsmdm.browser.tabs.TabManager
import com.kidsmdm.browser.util.UrlOrSearchResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** No settings/navigation screens beyond these two overlays - a sealed enum in this one ViewModel
 * is simpler than pulling in navigation-compose for a browser this small. */
enum class OverlayScreen { NONE, BOOKMARKS, HISTORY }

/**
 * Scoped to the Activity, so [TabManager] (and the WebView instances it owns via
 * [com.kidsmdm.browser.tabs.TabWebViewHost]) survive rotation for free - see that class's own
 * doc comment for why that means process-death Bundle persistence isn't implemented yet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val bookmarkRepository = BookmarkRepository(appContext)

    val tabManager = TabManager(appContext)

    val tabs = tabManager.tabs
    val activeTabId = tabManager.activeTabId
    val customView = tabManager.customView

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val currentUrl: StateFlow<String> = combine(tabs, activeTabId) { tabList, activeId ->
        tabList.find { it.id == activeId }?.url.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val isCurrentUrlBookmarked: StateFlow<Boolean> = currentUrl
        .flatMapLatest { url ->
            if (url.isBlank()) flowOf(false) else bookmarkRepository.observeIsBookmarked(url)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _overlay = MutableStateFlow(OverlayScreen.NONE)
    val overlay: StateFlow<OverlayScreen> = _overlay.asStateFlow()

    private val _historyItems = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyItems: StateFlow<List<HistoryItem>> = _historyItems.asStateFlow()

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
    fun handleBackPress(): Boolean {
        if (_overlay.value != OverlayScreen.NONE) {
            dismissOverlay()
            return true
        }
        return tabManager.handleBackPress()
    }

    fun setSystemDark(dark: Boolean) = tabManager.setSystemDark(dark)

    fun showBookmarks() {
        _overlay.value = OverlayScreen.BOOKMARKS
    }

    fun showHistory() {
        _overlay.value = OverlayScreen.HISTORY
        refreshHistory()
    }

    fun dismissOverlay() {
        _overlay.value = OverlayScreen.NONE
    }

    fun toggleBookmarkForCurrentTab() {
        val tab = tabs.value.find { it.id == activeTabId.value } ?: return
        if (tab.url.isBlank()) return
        viewModelScope.launch {
            if (isCurrentUrlBookmarked.value) {
                bookmarkRepository.removeByUrl(tab.url)
            } else {
                bookmarkRepository.add(tab.url, tab.title)
            }
        }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch { bookmarkRepository.remove(bookmark) }
    }

    /** Used by both overlay screens: navigate the active tab and dismiss. */
    fun openFromOverlay(url: String) {
        dismissOverlay()
        onSubmitAddress(url)
    }

    private fun refreshHistory() {
        viewModelScope.launch {
            _historyItems.value = withContext(Dispatchers.IO) {
                JournalDatabase.getInstance(appContext).queryRecent().use { cursor ->
                    val urlCol = cursor.getColumnIndexOrThrow("url")
                    val titleCol = cursor.getColumnIndex("title")
                    val timestampCol = cursor.getColumnIndexOrThrow("timestamp")
                    val list = mutableListOf<HistoryItem>()
                    while (cursor.moveToNext()) {
                        list.add(
                            HistoryItem(
                                url = cursor.getString(urlCol),
                                title = if (titleCol >= 0) cursor.getString(titleCol).orEmpty() else "",
                                timestamp = cursor.getLong(timestampCol),
                            ),
                        )
                    }
                    list
                }
            }
        }
    }

    override fun onCleared() {
        tabManager.destroyAll()
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer { BrowserViewModel(context) }
        }
    }
}
