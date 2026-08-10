package com.kidsmdm.browser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidsmdm.browser.bookmarks.BookmarkEntity

/** Bookmarks *are* user-editable, unlike History - a delete affordance here is intentional and
 * fine, this is just the kid's own saved-shortcuts list, not a tamper-resistance concern. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    onOpen: (String) -> Unit,
    onDelete: (BookmarkEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (bookmarks.isEmpty()) {
            Column(modifier = Modifier.padding(padding)) {
                Text(
                    text = "No bookmarks yet.",
                    modifier = Modifier.padding(24.dp),
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(bookmarks, key = { it.id }) { bookmark ->
                ListItem(
                    headlineContent = { Text(bookmark.title, maxLines = 1) },
                    supportingContent = { Text(bookmark.url, maxLines = 1) },
                    trailingContent = {
                        IconButton(onClick = { onDelete(bookmark) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove bookmark")
                        }
                    },
                    modifier = Modifier.clickable { onOpen(bookmark.url) },
                )
            }
        }
    }
}
