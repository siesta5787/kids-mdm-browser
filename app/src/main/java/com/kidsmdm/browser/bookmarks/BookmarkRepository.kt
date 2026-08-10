package com.kidsmdm.browser.bookmarks

import android.content.Context
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(context: Context) {
    private val dao = BookmarkDatabase.getInstance(context).bookmarkDao()

    fun observeAll(): Flow<List<BookmarkEntity>> = dao.observeAll()

    fun observeIsBookmarked(url: String): Flow<Boolean> = dao.observeIsBookmarked(url)

    suspend fun add(url: String, title: String) {
        dao.insert(BookmarkEntity(url = url, title = title.ifBlank { url }))
    }

    suspend fun removeByUrl(url: String) {
        dao.deleteByUrl(url)
    }

    suspend fun remove(bookmark: BookmarkEntity) {
        dao.delete(bookmark)
    }
}
