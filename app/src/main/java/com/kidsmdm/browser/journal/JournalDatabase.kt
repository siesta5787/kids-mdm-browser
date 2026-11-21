package com.kidsmdm.browser.journal

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Own physical SQLite file, entirely separate from anything a "clear browsing data" feature in
 * this app's own History UI might ever touch - deliberately, so a kid clearing history doesn't
 * also erase what the parent already synced. Written on every real navigation (see
 * tabs/BrowserWebViewClient.kt's onPageFinished), read by [JournalProvider] (external,
 * kids-launcher-mdm-facing sync contract) and [queryRecent] (internal, this app's own read-only
 * History screen). Kotlin port of the same class from the abandoned Chromium-fork attempt -
 * schema/contract unchanged, kids-launcher-mdm's BrowserHistorySync.kt already expects exactly
 * this shape.
 */
class JournalDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "kids_history_journal.db"
        private const val DB_VERSION = 2

        /** Lowest possible id base (2023-11-14), in case the clock is unset at creation. */
        private const val MIN_ID_BASE = 1_700_000_000_000L

        /**
         * Ids start at the journal's creation time in milliseconds instead of 1, so a reinstall
         * (or cleared app data) keeps counting up past every id kids-launcher-mdm has already
         * synced. The launcher only asks for `_id > ` its last synced id, so ids restarting at 1
         * would hide every new visit from it. An install would need more than one visit per
         * millisecond over its whole life to overlap the next one's base.
         */
        private fun newIdBase(): Long = maxOf(System.currentTimeMillis(), MIN_ID_BASE)

        const val TABLE = "journal_entry"
        const val COL_ID = "_id"
        const val COL_URL = "url"
        const val COL_TITLE = "title"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_CREATED_AT = "created_at"

        @Volatile
        private var instance: JournalDatabase? = null

        fun getInstance(context: Context): JournalDatabase =
            instance ?: synchronized(this) {
                instance ?: JournalDatabase(context.applicationContext).also { instance = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE (" +
                "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_URL TEXT NOT NULL, " +
                "$COL_TITLE TEXT, " +
                "$COL_TIMESTAMP INTEGER NOT NULL, " +
                "$COL_CREATED_AT INTEGER NOT NULL)"
        )
        setNextId(db, newIdBase())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Version 1 numbered from 1. Move existing rows above the new base, so a launcher still
            // holding a cursor from an earlier install picks them up too.
            val base = newIdBase()
            db.execSQL("UPDATE $TABLE SET $COL_ID = $COL_ID + $base")
            setNextId(db, base)
        }
    }

    /** The next row gets an id above [base] (AUTOINCREMENT also never goes below the highest existing id). */
    private fun setNextId(db: SQLiteDatabase, base: Long) {
        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '$TABLE'")
        db.execSQL("INSERT INTO sqlite_sequence (name, seq) VALUES ('$TABLE', $base)")
    }

    /** Called from BrowserWebViewClient on every completed top-level http(s) navigation. */
    fun recordVisit(url: String, title: String?) {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(COL_URL, url)
            put(COL_TITLE, title)
            put(COL_TIMESTAMP, now)
            put(COL_CREATED_AT, now)
        }
        writableDatabase.insert(TABLE, null, values)
    }

    /**
     * Up to 200 rows, oldest-first, `_id > sinceId` - the frozen external sync contract, see
     * [JournalProvider]. Do not repurpose this for UI display; use [queryRecent] instead, which
     * has different ordering/pagination needs.
     */
    fun queryEntriesSince(sinceId: Long): Cursor =
        readableDatabase.query(
            TABLE,
            null,
            "$COL_ID > ?",
            arrayOf(sinceId.toString()),
            null,
            null,
            "$COL_ID ASC",
            "200",
        )

    /** Newest-first, for this app's own read-only History screen only - never exposed via
     * [JournalProvider], and deliberately has no corresponding delete/clear method anywhere in
     * this class. */
    fun queryRecent(limit: Int = 200): Cursor =
        readableDatabase.query(
            TABLE,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC",
            limit.toString(),
        )
}
