package com.ogzhngms.ihalebak

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

enum class View { AGENDA, SAVED }

private const val STALE_AFTER_MS = 30 * 60 * 1000L

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(application)
    private val settings = Settings(application)

    var index by mutableStateOf<Index?>(null)
        private set
    var cities by mutableStateOf(settings.cities)
        private set
    var categories by mutableStateOf(settings.categories)
        private set
    var notify by mutableStateOf(settings.notify)
        private set
    // The tenders of every chosen province together.
    var tenders by mutableStateOf<List<Tender>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var offline by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    var favorites by mutableStateOf(settings.favorites)
        private set

    var view by mutableStateOf(View.AGENDA)
    // What narrows the agenda: one of the chosen provinces, one day, a search.
    var cityFilter by mutableStateOf<String?>(null)
    var day by mutableStateOf<LocalDate?>(null)
    var searching by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
    var selected by mutableStateOf<Tender?>(null)

    // The two first-run questions, also reached from the settings button: 0 when closed, else the step. The
    // answers are drafts until "Ajandamı göster".
    var setupStep by mutableStateOf(if (settings.cities.isEmpty()) 1 else 0)
        private set
    var draftCities by mutableStateOf(settings.cities)
        private set
    var draftCategories by mutableStateOf(settings.categories)
        private set
    var draftNotify by mutableStateOf(settings.notify)

    private var job: Job? = null
    private var loadedAt = 0L

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
                val loaded = coroutineScope { cities.map { async { repository.province(it, refresh) } }.awaitAll() }
                tenders = loaded.flatMap { it.value }
                val cached = loadedIndex.fromCache || loaded.any { it.fromCache }
                if (!refresh && cached) {
                    // The cached copy is on screen; now quietly bring it up to date.
                    loading = false
                    refreshQuietly()
                    return@launch
                }
                offline = refresh && cached
                if (!offline) loadedAt = System.currentTimeMillis()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed = index == null || (cities.isNotEmpty() && tenders.isEmpty())
                offline = true
            } finally {
                loading = false
            }
        }
    }

    private suspend fun refreshQuietly() {
        try {
            index = repository.index(refresh = true).value
            tenders = coroutineScope { cities.map { async { repository.province(it, refresh = true) } }.awaitAll() }.flatMap { it.value }
            offline = false
            loadedAt = System.currentTimeMillis()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            offline = true
        }
    }

    // The agenda may have been left open in the background for hours; bring it up to date when the app comes back.
    fun refreshIfStale() {
        if (!loading && loadedAt > 0 && System.currentTimeMillis() - loadedAt > STALE_AFTER_MS) load(refresh = true)
    }

    fun provinceName(slug: String?): String? = index?.provinces?.firstOrNull { it.slug == slug }?.name

    fun toggleSearch() {
        searching = !searching
        query = ""
        if (searching) view = View.AGENDA
    }

    // Back to the whole agenda: every day, every chosen province, no search.
    fun showEverything() {
        day = null
        cityFilter = null
        query = ""
    }

    // A new-tender notification opens the agenda on its province.
    fun showCity(slug: String) {
        setupStep = 0
        selected = null
        view = View.AGENDA
        day = null
        cityFilter = slug.takeIf { it in cities }
    }

    fun isFavorite(tender: Tender) = favorites.any { it.ikn == tender.ikn }

    fun toggleFavorite(tender: Tender) {
        favorites = if (isFavorite(tender)) favorites.filterNot { it.ikn == tender.ikn } else favorites + tender
        settings.favorites = favorites
    }

    fun openSetup() {
        draftCities = cities
        draftCategories = categories
        draftNotify = notify
        setupStep = 1
    }

    // Leaves the questions without changing anything; not possible on the first run, when nothing is chosen yet.
    fun closeSetup() {
        if (cities.isNotEmpty()) setupStep = 0
    }

    fun setupBack() {
        if (setupStep == 2) setupStep = 1 else closeSetup()
    }

    fun setupNext() {
        if (setupStep == 1 && draftCities.isNotEmpty()) setupStep = 2 else if (setupStep == 2) finishSetup()
    }

    fun toggleDraftCity(slug: String) {
        draftCities = if (slug in draftCities) draftCities - slug else draftCities + slug
    }

    // At least one type stays chosen, and all four are kept as the empty set, which means every type.
    fun toggleDraftCategory(category: Category) {
        val chosen = draftCategories.ifEmpty { Category.entries.toSet() }
        val next = if (category in chosen) chosen - category else chosen + category
        if (next.isNotEmpty()) draftCategories = if (next.size == Category.entries.size) emptySet() else next
    }

    private fun finishSetup() {
        cities = draftCities
        categories = draftCategories
        notify = draftNotify
        settings.cities = cities
        settings.categories = categories
        settings.notify = notify
        WatchWorker.schedule(getApplication(), settings)
        setupStep = 0
        view = View.AGENDA
        showEverything()
        searching = false
        tenders = emptyList()
        load(refresh = false)
    }
}
