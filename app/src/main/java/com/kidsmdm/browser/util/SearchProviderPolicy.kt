package com.kidsmdm.browser.util

import android.content.Context
import android.content.RestrictionsManager

/**
 * Reads an MDM-pushed default search engine, if the Device Owner (kids-launcher-mdm) has set one
 * via `DevicePolicyManager.setApplicationRestrictions` - same delivery mechanism as the rest of
 * this app's enterprise policy bundle (see CLAUDE.md's "MDM policy bundle" section). Falls back
 * to Kiddle when unset, which is also how a fresh/unenrolled install behaves. No broadcast
 * receiver needed to react to a live policy push - `RestrictionsManager.getApplicationRestrictions`
 * is read fresh on every call, so a new value takes effect on the very next address-bar search.
 *
 * Contract (browser side only for now - launcher/server-side push is a separate, not-yet-built
 * piece): the `SearchEngineUrl` restriction string must contain the literal placeholder
 * `{searchTerms}`, replaced here with the URL-encoded query - the same convention Chrome's own
 * `DefaultSearchProviderSearchURL` enterprise policy uses, even though this app doesn't read
 * Chrome's actual policy (it isn't Chrome) - reusing the convention just means a future admin UI
 * can lean on the same mental model. A pushed value missing the placeholder is treated as unset
 * rather than silently swallowing every search into a fixed URL.
 */
object SearchProviderPolicy {
    const val RESTRICTION_KEY = "SearchEngineUrl"
    const val SEARCH_TERMS_PLACEHOLDER = "{searchTerms}"
    private const val DEFAULT_SEARCH_URL_TEMPLATE =
        "https://www.kiddle.co/s.php?q=$SEARCH_TERMS_PLACEHOLDER"

    fun searchUrlTemplate(context: Context): String {
        val manager = context.getSystemService(Context.RESTRICTIONS_SERVICE) as? RestrictionsManager
            ?: return DEFAULT_SEARCH_URL_TEMPLATE
        val pushed = manager.applicationRestrictions.getString(RESTRICTION_KEY)
        return if (!pushed.isNullOrBlank() && pushed.contains(SEARCH_TERMS_PLACEHOLDER)) {
            pushed
        } else {
            DEFAULT_SEARCH_URL_TEMPLATE
        }
    }
}
