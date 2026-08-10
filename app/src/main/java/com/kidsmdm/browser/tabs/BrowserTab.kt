package com.kidsmdm.browser.tabs

/** Opaque tab identifier - just a wrapped counter, no meaning beyond identity. */
@JvmInline
value class TabId(val value: Long)

/**
 * UI-observable tab metadata only - deliberately holds no [android.webkit.WebView] reference, so
 * it's safe to pass around as Compose state without worrying about View lifecycle. The actual
 * WebView instances live in [TabWebViewHost], keyed by the same [TabId].
 */
data class BrowserTab(
    val id: TabId,
    val title: String = "",
    val url: String = "",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isBlockedOrErrored: Boolean = false,
)
