package com.kidsmdm.browser.journal

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri

/**
 * Read-only, exported, signature-permission-gated provider at authority
 * "com.kidsmdm.browser.journal" - same authority/permission-naming shape as kids-mdm-im's own
 * JournalProvider, so kids-launcher-mdm's existing BrowserHistorySync.kt (already written, this
 * session, against exactly this contract) needs zero changes to work against this app.
 *
 * Only URI handled: content://com.kidsmdm.browser.journal/entries/<sinceId> - returns rows with
 * _id > sinceId, oldest-first, capped at 200. Caller-supplied projection/selection/sortOrder are
 * ignored on purpose, same contract as kids-mdm-im's provider.
 *
 * Enforcement of the com.kidsmdm.browser.ACCESS_JOURNAL signature permission is declared on the
 * <provider> manifest entry, not here - Android denies the call before it reaches this class for
 * a caller signed with a different certificate.
 */
class JournalProvider : ContentProvider() {
    private val matcher = UriMatcher(UriMatcher.NO_MATCH)

    override fun onCreate(): Boolean {
        matcher.addURI(AUTHORITY, "entries/#", ENTRIES)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        if (matcher.match(uri) != ENTRIES) return null
        val sinceId = uri.lastPathSegment?.toLongOrNull() ?: return null
        val cursor = JournalDatabase.getInstance(requireContext()).queryEntriesSince(sinceId)
        cursor.setNotificationUri(requireContext().contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.kidsmdm.journal-entry"

    // Read-only surface - the journal is written only from BrowserWebViewClient, in-process.
    override fun insert(uri: Uri, values: ContentValues?): Uri =
        throw UnsupportedOperationException("read-only provider")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("read-only provider")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("read-only provider")

    companion object {
        private const val AUTHORITY = "com.kidsmdm.browser.journal"
        private const val ENTRIES = 1
    }
}
