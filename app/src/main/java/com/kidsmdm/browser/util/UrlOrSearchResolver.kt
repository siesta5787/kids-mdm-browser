package com.kidsmdm.browser.util

import android.content.Context
import android.util.Patterns
import java.net.URLEncoder

/**
 * Address-bar text -> either a normalized URL or a search query, using the same "looks-like-a-
 * host" heuristic every browser uses. Purely a UX choice, not a safety one - the DNS-layer filter
 * applies regardless of which search engine (or none) is used, see the plan. Search engine itself
 * defaults to Kiddle but can be overridden by MDM policy - see [SearchProviderPolicy].
 */
object UrlOrSearchResolver {
    fun resolve(context: Context, input: String): String {
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

        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        return SearchProviderPolicy.searchUrlTemplate(context)
            .replace(SearchProviderPolicy.SEARCH_TERMS_PLACEHOLDER, encoded)
    }

    /** Catches simple bare hosts like "example.com" or "localhost" that [Patterns.WEB_URL]
     * sometimes misses without a path component. */
    private fun isLikelyBareHost(input: String): Boolean {
        if (input.contains("/")) return false
        val parts = input.split(".")
        return parts.size >= 2 && parts.last().length in 2..24 && parts.all { it.isNotBlank() }
    }
}
