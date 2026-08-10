package com.kidsmdm.browser.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidsmdm.browser.tabs.BrowserTab
import com.kidsmdm.browser.tabs.TabId

@Composable
fun TabStrip(
    tabs: List<BrowserTab>,
    activeTabId: TabId?,
    onSelect: (TabId) -> Unit,
    onClose: (TabId) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.size <= 1) return // no reason to show a strip for a single tab
    LazyRow(modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        items(tabs, key = { it.id.value }) { tab ->
            FilterChip(
                modifier = Modifier.padding(end = 4.dp),
                selected = tab.id == activeTabId,
                onClick = { onSelect(tab.id) },
                label = {
                    Text(
                        text = tab.title.ifBlank { tab.url.ifBlank { "New tab" } },
                        maxLines = 1,
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { onClose(tab.id) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close tab")
                    }
                },
            )
        }
    }
}
