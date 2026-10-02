package com.ogzhngms.ihalebak

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// What the user picked, kept on the device: the provinces and types in the agenda, saved tenders, and whether to
// announce new tenders.
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    // Provinces by slug, in the order they were picked. Empty until the first-run questions are answered.
    var cities: List<String>
        get() = JSONArray(prefs.getString("cities", "[]")).let { array -> List(array.length()) { array.getString(it) } }
        set(value) = prefs.edit().putString("cities", JSONArray(value).toString()).apply()

    // The tender types of interest; empty means all of them.
    var categories: Set<Category>
        get() = prefs.getStringSet("categories", emptySet())!!.mapNotNull(Category::of).toSet()
        set(value) = prefs.edit().putStringSet("categories", value.map { it.id }.toSet()).apply()

    var notify: Boolean
        get() = prefs.getBoolean("notify", true)
        set(value) = prefs.edit().putBoolean("notify", value).apply()

    // The provinces checked in the background: the agenda's own, while notifications are on.
    val watchedProvinces: List<String> get() = if (notify) cities else emptyList()

    // Saved tenders are stored whole, so the list still shows them after they leave the published data.
    var favorites: List<Tender>
        get() {
            val array = JSONArray(prefs.getString("favorites", "[]"))
            return List(array.length()) { Tender.parse(JSONObject(array.getString(it))) }
        }
        set(value) = prefs.edit().putString("favorites", JSONArray(value.map { it.json }).toString()).apply()

    // The tenders already seen in each watched province, so only new ones are announced.
    fun seen(slug: String): Set<String>? = prefs.getStringSet("seen_$slug", null)?.toSet()

    fun markSeen(slug: String, ikns: Set<String>) = prefs.edit().putStringSet("seen_$slug", ikns).apply()
}
