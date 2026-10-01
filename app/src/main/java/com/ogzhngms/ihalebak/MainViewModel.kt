package com.ogzhngms.ihalebak

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class Tab { TENDERS, FAVORITES, WATCH }

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(application)
    private val settings = Settings(application)

    var tab by mutableStateOf(Tab.TENDERS)
    var index by mutableStateOf<Index?>(null)
        private set
    var province by mutableStateOf(settings.province)
        private set
    var tenders by mutableStateOf<List<Tender>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var offline by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
    var categories by mutableStateOf<Set<Category>>(emptySet())
    var selected by mutableStateOf<Tender?>(null)
    var favorites by mutableStateOf(settings.favorites)
        private set
    var watchedProvinces by mutableStateOf(settings.watchedProvinces)
        private set
    var watchedCategories by mutableStateOf(settings.watchedCategories)
        private set

    private var job: Job? = null

    init {
        load(refresh = false)
    }

    // Shows the cached copy at once when there is one, then fetches the latest.
    fun load(refresh: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            loading = true
            failed = false
            try {
                val loadedIndex = repository.index(refresh)
                index = loadedIndex.value
                province?.let { slug ->
                    val loaded = repository.province(slug, refresh)
                    tenders = loaded.value
                    offline = loaded.fromCache && refresh
                }
                if (!refresh && (loadedIndex.fromCache)) {
                    // The cached copy is on screen; now quietly bring it up to date.
                    loading = false
                    refreshQuietly()
                    return@launch
                }
                offline = loadedIndex.fromCache && refresh
            } catch (e: Exception) {
                failed = index == null || (province != null && tenders.isEmpty())
                offline = true
            } finally {
                loading = false
            }
        }
    }

    private suspend fun refreshQuietly() {
        try {
            index = repository.index(refresh = true).value
            province?.let { tenders = repository.province(it, refresh = true).value }
            offline = false
        } catch (e: Exception) {
            offline = true
        }
    }

    fun choose(slug: String) {
        province = slug
        settings.province = slug
        tenders = emptyList()
        query = ""
        load(refresh = false)
    }

    fun toggleCategory(category: Category) {
        categories = if (category in categories) categories - category else categories + category
    }

    fun isFavorite(tender: Tender) = favorites.any { it.ikn == tender.ikn }

    fun toggleFavorite(tender: Tender) {
        favorites = if (isFavorite(tender)) favorites.filterNot { it.ikn == tender.ikn } else favorites + tender
        settings.favorites = favorites
    }

    fun toggleWatchedProvince(slug: String) {
        watchedProvinces = if (slug in watchedProvinces) watchedProvinces - slug else watchedProvinces + slug
        settings.watchedProvinces = watchedProvinces
        WatchWorker.schedule(getApplication(), settings)
    }

    fun toggleWatchedCategory(category: Category) {
        watchedCategories = if (category in watchedCategories) watchedCategories - category else watchedCategories + category
        settings.watchedCategories = watchedCategories
    }

    fun provinceName(slug: String?): String? = index?.provinces?.firstOrNull { it.slug == slug }?.name
}
