package com.kidsmdm.browser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidsmdm.browser.history.HistoryItem
import java.text.DateFormat
import java.util.Date

/**
 * Read-only: no delete/clear affordance anywhere on this screen, matching
 * [com.kidsmdm.browser.history.HistoryViewModel] having no delete method to wire one to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    items: List<HistoryItem>,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Column(modifier = Modifier.padding(padding)) {
                Text(
                    text = "No history yet.",
                    modifier = Modifier.padding(24.dp),
                )
            }
            return@Scaffold
        }
        val dateFormat = remember(items) { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(items, key = { it.timestamp }) { item ->
                ListItem(
                    headlineContent = { Text(item.title.ifBlank { item.url }, maxLines = 1) },
                    supportingContent = { Text(item.url, maxLines = 1) },
                    trailingContent = { Text(dateFormat.format(Date(item.timestamp))) },
                    modifier = Modifier.clickable { onOpen(item.url) },
                )
            }
        }
    }
}
