package com.kidsmdm.browser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kidsmdm.browser.bookmarks.BookmarkEntity

private const val MAX_SHORTCUTS = 5

/**
 * Native (not a loaded website) new-tab landing screen - avoids depending on any third-party
 * site's uptime/design, and avoids an implicit "endorsed" landing page. Deliberately just
 * bookmark shortcuts and nothing else - no placeholder copy.
 */
@Composable
fun NewTabScreen(
    bookmarks: List<BookmarkEntity> = emptyList(),
    onOpenBookmark: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(bookmarks.take(MAX_SHORTCUTS), key = { it.id }) { bookmark ->
                ListItem(
                    headlineContent = { Text(bookmark.title, maxLines = 1) },
                    supportingContent = { Text(bookmark.url, maxLines = 1) },
                    leadingContent = { Icon(Icons.Filled.Bookmark, contentDescription = null) },
                    modifier = Modifier.clickable { onOpenBookmark(bookmark.url) },
                )
            }
        }
    }
}
