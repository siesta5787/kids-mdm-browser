package com.kidsmdm.browser.tabs

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.http.SslError
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.kidsmdm.browser.journal.JournalDatabase

/**
 * Every tab shares this same hardening posture - see individual overrides for reasoning. Written
 * once here rather than per-call-site so there's exactly one place to audit.
 */
class BrowserWebViewClient(
    private val context: Context,
    private val tabId: TabId,
    private val sink: TabEventSink,
) : WebViewClient() {

    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (url != null) sink.onPageStarted(tabId, url)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        sink.onNavigationStateChanged(tabId)
        if (url != null && isHttpOrHttps(url)) {
            // Independent journal write, immune to whatever this app's own (delete-free) History
            // screen does - see JournalDatabase's own doc comment.
            JournalDatabase.getInstance(context).recordVisit(url, view.title)
            sink.onPageFinished(tabId, url, view.title)
        }
    }

    /** Real navigation errors only (not e.g. `ERR_BLOCKED_BY_CLIENT` for a subresource) - a
     * DNS-filtered domain and a genuinely-down site are indistinguishable from WebView's own
     * vantage point at this layer, so this can only show a generic "couldn't load this page",
     * never filter-aware messaging. */
    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        super.onReceivedError(view, request, error)
        if (request.isForMainFrame) sink.onLoadError(tabId)
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request.isForMainFrame && errorResponse.statusCode >= 400) sink.onLoadError(tabId)
    }

    /** Never bypassable - no "proceed anyway" path exists anywhere in this app. */
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()
        sink.onLoadError(tabId)
    }

    /** One tab's renderer OOM-crashing is a real WebView event on this hardware, not
     * hypothetical - close/reload just this tab rather than letting it take the whole app down.
     * Returning true tells the framework we've handled it (don't crash the host app). */
    override fun onRenderProcessGone(
        view: WebView,
        detail: android.webkit.RenderProcessGoneDetail,
    ): Boolean {
        sink.onRequestCloseTab(tabId)
        return true
    }

    /** Block with no "visit unsafe site anyway" escape hatch - the stock WebView interstitial
     * does offer one, which is suppressed here on purpose. */
    override fun onSafeBrowsingHit(
        view: WebView,
        request: WebResourceRequest,
        threatType: Int,
        callback: SafeBrowsingResponse,
    ) {
        callback.backToSafety(true)
        sink.onLoadError(tabId)
    }

    /** http/https stay in-WebView; hand off tel:/mailto:/intent:/market: etc. to the system.
     * Silently swallow anything the system can't resolve rather than crashing. */
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url
        if (isHttpOrHttps(url.toString())) return false
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            true
        }
    }

    private fun isHttpOrHttps(url: String) = url.startsWith("http://") || url.startsWith("https://")
}
