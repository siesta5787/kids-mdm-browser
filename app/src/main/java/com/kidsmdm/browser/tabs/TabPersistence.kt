package com.kidsmdm.browser.tabs

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Just a URL/title pair - not full WebView state (scroll position, form data, in-page back
 * history). A restored tab re-navigates to its last URL fresh; see TabWebViewHost's own doc
 * comment for why full state restore is an accepted simplification, not done here either. */
data class PersistedTab(val url: String, val title: String)

/**
 * Tabs are otherwise only ViewModel-scoped, which survives rotation but not the process actually
 * dying (app swiped away, backgrounded long enough to be killed, etc.) - this is what makes
 * "reopen where I left off" work across a real app close, not just a config change.
 */
object TabPersistence {
    private const val PREFS_NAME = "tab_state"
    private const val KEY_TABS = "tabs"
    private const val KEY_ACTIVE_INDEX = "active_index"

    fun save(context: Context, tabs: List<PersistedTab>, activeIndex: Int) {
        val array = JSONArray()
        tabs.forEach { tab ->
            array.put(
                JSONObject().apply {
                    put("url", tab.url)
                    put("title", tab.title)
                },
            )
        }
        prefs(context).edit()
            .putString(KEY_TABS, array.toString())
            .putInt(KEY_ACTIVE_INDEX, activeIndex)
            .apply()
    }

    /** Second value is the index (into the returned list) of the tab that was active, or -1. */
    fun restore(context: Context): Pair<List<PersistedTab>, Int> {
        val json = prefs(context).getString(KEY_TABS, null)
            ?: return emptyList<PersistedTab>() to -1
        val array = JSONArray(json)
        val tabs = (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            PersistedTab(url = obj.getString("url"), title = obj.optString("title"))
        }
        return tabs to prefs(context).getInt(KEY_ACTIVE_INDEX, -1)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
