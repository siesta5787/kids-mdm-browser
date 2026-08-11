package com.kidsmdm.browser.tabs

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast

/**
 * WebView has no download handling at all without an explicit `DownloadListener` - hands the
 * request off to Android's own `DownloadManager`, which saves into the public Downloads
 * directory and shows its own system progress/completion notification. No extra permission
 * needed for that public-directory write on API 29+ (scoped storage grants DownloadManager
 * itself write access there), and the notification is posted by the system process, not this
 * app, so it doesn't need `POST_NOTIFICATIONS` either.
 */
object DownloadHandler {
    fun enqueue(
        context: Context,
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String?,
    ) {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setMimeType(mimeType)
            addRequestHeader("User-Agent", userAgent)
            addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
            setTitle(fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(context, "Downloading $fileName", Toast.LENGTH_SHORT).show()
    }
}
