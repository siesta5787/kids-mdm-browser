package com.kidsmdm.browser.tabs

import android.os.Message
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * Every tab shares this same hardening posture - see individual overrides for reasoning. Written
 * once here rather than per-call-site so there's exactly one place to audit.
 */
class BrowserWebChromeClient(
    private val tabId: TabId,
    private val sink: TabEventSink,
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        sink.onProgressChanged(tabId, newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        if (title != null) sink.onReceivedTitle(tabId, title)
    }

    /** New in-app tab, gated on [isUserGesture] - blocks JS popup-spam while still supporting a
     * real `target=_blank` link. Never spawns a second Activity/window. */
    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message,
    ): Boolean {
        val newTabWebView = sink.onRequestNewTab(isUserGesture) ?: return false
        (resultMsg.obj as WebView.WebViewTransport).webView = newTabWebView
        resultMsg.sendToTarget()
        return true
    }

    override fun onCloseWindow(window: WebView) {
        sink.onRequestCloseTab(tabId)
    }

    // Deny-by-default surface - see the class doc in WebViewFactory for the rationale shared
    // across every tab: this is a kid-safety product, every one of these is extra attack/
    // exfiltration/social-engineering surface with no established need in the requested feature
    // set, on top of not being declared in the manifest in the first place.
    override fun onPermissionRequest(request: PermissionRequest) {
        request.deny()
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback,
    ) {
        callback.invoke(origin, false, false)
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<android.net.Uri>>,
        fileChooserParams: FileChooserParams,
    ): Boolean {
        // No-op refusal, not a crash: WebView expects either a call to onReceiveValue or a
        // returned false to mean "chooser not shown."
        return false
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        sink.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        sink.onHideCustomView()
    }
}
