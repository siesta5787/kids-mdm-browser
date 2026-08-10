package com.kidsmdm.browser.bookmarks

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Pure in-app data, no external contract - unlike the journal, nothing outside this app reads
 * bookmarks, so this is free to use Room instead of matching some other repo's expectations. */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
)
