package com.kidsmdm.browser.util

import android.util.Patterns

/**
 * Address-bar text -> either a normalized URL or a Kiddle search query, using the same
 * "looks-like-a-host" heuristic every browser uses. Purely a UX choice, not a safety one - the
 * DNS-layer filter applies regardless of which search engine (or none) is used, see the plan.
 */
object UrlOrSearchResolver {
    private const val SEARCH_BASE_URL = "https://www.kiddle.co/s.php?q="

    fun resolve(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }

        val looksLikeUrl = !trimmed.contains(" ") &&
            (Patterns.WEB_URL.matcher(trimmed).matches() || isLikelyBareHost(trimmed))
        if (looksLikeUrl) {
            return "https://$trimmed"
        }

        return SEARCH_BASE_URL + java.net.URLEncoder.encode(trimmed, "UTF-8")
    }

    /** Catches simple bare hosts like "example.com" or "localhost" that [Patterns.WEB_URL]
     * sometimes misses without a path component. */
    private fun isLikelyBareHost(input: String): Boolean {
        if (input.contains("/")) return false
        val parts = input.split(".")
        return parts.size >= 2 && parts.last().length in 2..24 && parts.all { it.isNotBlank() }
    }
}
