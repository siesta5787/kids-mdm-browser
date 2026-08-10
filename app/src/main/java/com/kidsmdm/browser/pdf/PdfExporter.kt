package com.kidsmdm.browser.pdf

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Hands off to the system Print framework - its own UI already includes a "Save as PDF" printer,
 * so there's no need for this app to touch storage permissions or write files itself.
 */
object PdfExporter {
    fun export(context: Context, webView: WebView, documentName: String?) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = documentName?.takeIf { it.isNotBlank() } ?: defaultJobName()
        val adapter = webView.createPrintDocumentAdapter(jobName)
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    private fun defaultJobName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        return "page-$stamp"
    }
}
