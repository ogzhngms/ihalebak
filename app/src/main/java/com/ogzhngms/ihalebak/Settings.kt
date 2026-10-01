package com.ogzhngms.ihalebak

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// What the user picked, kept on the device: the province being browsed, saved tenders, and what to watch.
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var province: String?
        get() = prefs.getString("province", null)
        set(value) = prefs.edit().putString("province", value).apply()

    // Saved tenders are stored whole, so the list still shows them after they leave the published data.
    var favorites: List<Tender>
        get() {
            val array = JSONArray(prefs.getString("favorites", "[]"))
            return List(array.length()) { Tender.parse(JSONObject(array.getString(it))) }
        }
        set(value) = prefs.edit().putString("favorites", JSONArray(value.map { it.json }).toString()).apply()

    // Provinces (by slug) and categories to be notified about; an empty category set means all of them.
    var watchedProvinces: Set<String>
        get() = prefs.getStringSet("watched_provinces", emptySet())!!.toSet()
        set(value) = prefs.edit().putStringSet("watched_provinces", value).apply()

    var watchedCategories: Set<Category>
        get() = prefs.getStringSet("watched_categories", emptySet())!!.mapNotNull(Category::of).toSet()
        set(value) = prefs.edit().putStringSet("watched_categories", value.map { it.id }.toSet()).apply()

    // The tenders already seen in each watched province, so only new ones are announced.
    fun seen(slug: String): Set<String>? = prefs.getStringSet("seen_$slug", null)?.toSet()

    fun markSeen(slug: String, ikns: Set<String>) = prefs.edit().putStringSet("seen_$slug", ikns).apply()
}
