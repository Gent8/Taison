package eu.kanade.tachiyomi.ui.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.tachiyomi.util.category.LastUsedCategoryState
import eu.kanade.tachiyomi.util.lang.toLocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.history.interactor.RemoveHistory
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetDuplicateLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class HistoryScreenModel(
    private val addTracks: AddTracks = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val getDuplicateLibraryManga: GetDuplicateLibraryManga = Injekt.get(),
    private val getHistory: GetHistory = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val getNextChapters: GetNextChapters = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val lastUsedCategoryState: LastUsedCategoryState = Injekt.get(),
    private val removeHistory: RemoveHistory = Injekt.get(),
    private val setMangaCategories: SetMangaCategories = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
    private val sourceManager: SourceManager = Injekt.get(),
) : StateScreenModel<HistoryScreenModel.State>(State()) {

    private val _events: Channel<Event> = Channel(Channel.UNLIMITED)
    val events: Flow<Event> = _events.receiveAsFlow()

    init {
        screenModelScope.launch {
            val searchQueryFlow = state.map { it.searchQuery }.distinctUntilChanged()
            val showNonLibraryEntriesFlow = state.map { it.showNonLibraryEntries }.distinctUntilChanged()
            val scopeEnabledFlow = libraryPreferences.historyScopeByCategory().changes().distinctUntilChanged()
            val activeCategoryIdFlow = lastUsedCategoryState.state
            val categoriesFlow = getCategories.subscribe().distinctUntilChanged()
            val historyNavigationEnabledFlow = libraryPreferences.historyCategoryNavigation().changes().distinctUntilChanged()
            val navigationModeFlow = libraryPreferences.categoryNavigationMode().changes().distinctUntilChanged()

            val historyFlow = searchQueryFlow
                .flatMapLatest { query ->
                    getHistory.subscribe(query.orEmpty())
                        .distinctUntilChanged()
                        .catch { error ->
                            logcat(LogPriority.ERROR, error)
                            _events.send(Event.InternalError)
                            emit(emptyList())
                        }
                        .flowOn(Dispatchers.IO)
                }

            val filterInputsFlow = combine(
                showNonLibraryEntriesFlow,
                scopeEnabledFlow,
                activeCategoryIdFlow,
                categoriesFlow,
                historyNavigationEnabledFlow,
            ) { includeNonLibraryEntries, scopeEnabled, categoryId, categories, categoryNavigationEnabled ->
                HistoryFilterInputs(
                    includeNonLibraryEntries = includeNonLibraryEntries,
                    scopeEnabled = scopeEnabled,
                    activeCategoryId = categoryId,
                    categories = categories,
                    categoryNavigationEnabled = categoryNavigationEnabled,
                )
            }

            val filterConfigFlow = combine(
                filterInputsFlow,
                navigationModeFlow,
            ) { inputs, navigationMode ->
                HistoryFilterConfig(
                    includeNonLibraryEntries = inputs.includeNonLibraryEntries,
                    scopeEnabled = inputs.scopeEnabled,
                    activeCategoryId = inputs.activeCategoryId,
                    categories = inputs.categories,
                    categoryNavigationEnabled = inputs.categoryNavigationEnabled,
                    navigationMode = navigationMode,
                )
            }

            combine(
                historyFlow,
                filterConfigFlow,
            ) { histories, config ->
                histories to config
            }
                .map { (histories, config) ->
                    val resolvedCategoryId = config.resolvedCategoryId()
                    val navigationCategories = buildCategoryNavigation(config.categories)
                    val categoryHistories = if (config.scopeEnabled) {
                        navigationCategories.associate { category ->
                            category.id to filterByCategory(
                                history = histories,
                                categoryId = category.id,
                                includeNonLibraryEntries = config.includeNonLibraryEntries,
                            ).toHistoryUiModels()
                        }
                    } else {
                        emptyMap()
                    }
                    val filteredHistory = when (resolvedCategoryId) {
                        null -> histories.toHistoryUiModels()
                        else -> categoryHistories[resolvedCategoryId].orEmpty()
                    }
                    val activeCategory = resolvedCategoryId?.let { id ->
                        when (id) {
                            Category.UNCATEGORIZED_ID -> Category(
                                id = Category.UNCATEGORIZED_ID,
                                name = "",
                                order = 0,
                                flags = 0,
                            )
                            else -> navigationCategories.firstOrNull { it.id == id }
                        }
                    }
                    HistoryFilterResult(
                        uiModels = filteredHistory,
                        activeCategory = activeCategory,
                        scopeActive = resolvedCategoryId != null,
                        hasNonLibraryEntries = histories.any { !it.coverData.isMangaFavorite },
                        categories = navigationCategories,
                        categoryNavigationEnabled = config.categoryNavigationEnabled,
                        categoryNavigationMode = config.navigationMode,
                        activeCategoryId = resolvedCategoryId,
                        categoryHistories = categoryHistories,
                    )
                }
                .flowOn(Dispatchers.IO)
                .collect { result ->
                    mutableState.update { currentState ->
                        currentState.copy(
                            list = result.uiModels,
                            historyScopeEnabled = result.scopeActive,
                            activeCategory = result.activeCategory,
                            hasNonLibraryEntries = result.hasNonLibraryEntries,
                            categories = result.categories,
                            categoryNavigationEnabled = result.categoryNavigationEnabled,
                            categoryNavigationMode = result.categoryNavigationMode,
                            activeCategoryId = result.activeCategoryId,
                            categoryHistories = result.categoryHistories,
                        )
                    }
                }
        }
    }

    private fun List<HistoryWithRelations>.toHistoryUiModels(): List<HistoryUiModel> {
        return map { HistoryUiModel.Item(it) }
            .insertSeparators { before, after ->
                val beforeDate = before?.item?.readAt?.time?.toLocalDate()
                val afterDate = after?.item?.readAt?.time?.toLocalDate()
                when {
                    beforeDate != afterDate && afterDate != null -> HistoryUiModel.Header(afterDate)
                    // Return null to avoid adding a separator between two items.
                    else -> null
                }
            }
    }

    private fun buildCategoryNavigation(categories: List<Category>): List<Category> {
        val defaultCategory = Category(
            id = Category.UNCATEGORIZED_ID,
            name = "",
            order = 0,
            flags = 0,
        )
        val filtered = categories.filter { it.id != Category.UNCATEGORIZED_ID }
        return listOf(defaultCategory) + filtered
    }

    private data class HistoryFilterInputs(
        val includeNonLibraryEntries: Boolean,
        val scopeEnabled: Boolean,
        val activeCategoryId: Long,
        val categories: List<Category>,
        val categoryNavigationEnabled: Boolean,
    )

    private data class HistoryFilterConfig(
        val includeNonLibraryEntries: Boolean,
        val scopeEnabled: Boolean,
        val activeCategoryId: Long,
        val categories: List<Category>,
        val categoryNavigationEnabled: Boolean,
        val navigationMode: LibraryPreferences.CategoryNavigationMode,
    ) {
        fun resolvedCategoryId(): Long? {
            if (!scopeEnabled) return null
            if (activeCategoryId < 0) return null

            if (activeCategoryId == 0L) return 0L
            return categories.firstOrNull { it.id == activeCategoryId }?.id
        }
    }

    private data class HistoryFilterResult(
        val uiModels: List<HistoryUiModel>,
        val activeCategory: Category?,
        val scopeActive: Boolean,
        val hasNonLibraryEntries: Boolean,
        val categories: List<Category>,
        val categoryNavigationEnabled: Boolean,
        val categoryNavigationMode: LibraryPreferences.CategoryNavigationMode,
        val activeCategoryId: Long?,
        val categoryHistories: Map<Long, List<HistoryUiModel>>,
    )

    private fun filterByCategory(
        history: List<HistoryWithRelations>,
        categoryId: Long?,
        includeNonLibraryEntries: Boolean,
    ): List<HistoryWithRelations> {
        if (categoryId == null) return history

        return history.filter { entry ->
            val isNonLibraryEntry = !entry.coverData.isMangaFavorite
            when {
                categoryId == Category.UNCATEGORIZED_ID -> {
                    val matchesDefaultCategory =
                        entry.coverData.isMangaFavorite &&
                            (entry.categoryIds.isEmpty() || entry.categoryIds.contains(Category.UNCATEGORIZED_ID))
                    matchesDefaultCategory || (includeNonLibraryEntries && isNonLibraryEntry)
                }
                else -> entry.categoryIds.contains(categoryId) ||
                    (includeNonLibraryEntries && isNonLibraryEntry)
            }
        }
    }

    suspend fun getNextChapter(): Chapter? {
        return withIOContext { getNextChapters.await(onlyUnread = false).firstOrNull() }
    }

    fun getNextChapterForManga(mangaId: Long, chapterId: Long) {
        screenModelScope.launchIO {
            sendNextChapterEvent(getNextChapters.await(mangaId, chapterId, onlyUnread = false))
        }
    }

    private suspend fun sendNextChapterEvent(chapters: List<Chapter>) {
        val chapter = chapters.firstOrNull()
        _events.send(Event.OpenChapter(chapter))
    }

    fun removeFromHistory(history: HistoryWithRelations) {
        screenModelScope.launchIO {
            removeHistory.await(history)
        }
    }

    fun removeAllFromHistory(mangaId: Long) {
        screenModelScope.launchIO {
            removeHistory.await(mangaId)
        }
    }

    fun removeAllHistory() {
        screenModelScope.launchIO {
            val result = removeHistory.awaitAll()
            if (!result) return@launchIO
            _events.send(Event.HistoryCleared)
        }
    }

    fun updateSearchQuery(query: String?) {
        mutableState.update { it.copy(searchQuery = query) }
    }

    fun updateActiveCategory(categoryId: Long) {
        mutableState.update { current ->
            val activeCategory = current.categories.firstOrNull { it.id == categoryId }
            current.copy(
                activeCategoryId = categoryId,
                activeCategory = activeCategory,
            )
        }
        screenModelScope.launchIO {
            lastUsedCategoryState.set(categoryId)
            val categories = state.value.categories
            val index = categories.indexOfFirst { it.id == categoryId }.let { found ->
                if (found >= 0) found else 0
            }
            libraryPreferences.lastUsedCategory().set(index)
        }
    }

    fun toggleNonLibraryEntries() {
        mutableState.update { it.copy(showNonLibraryEntries = !it.showNonLibraryEntries) }
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    suspend fun getCategories(): List<Category> {
        return getCategories.await().filterNot { it.isSystemCategory }
    }

    private fun moveMangaToCategory(mangaId: Long, categories: Category?) {
        val categoryIds = listOfNotNull(categories).map { it.id }
        moveMangaToCategory(mangaId, categoryIds)
    }

    private fun moveMangaToCategory(mangaId: Long, categoryIds: List<Long>) {
        screenModelScope.launchIO {
            setMangaCategories.await(mangaId, categoryIds)
        }
    }

    fun moveMangaToCategoriesAndAddToLibrary(manga: Manga, categories: List<Long>) {
        moveMangaToCategory(manga.id, categories)
        if (manga.favorite) return

        screenModelScope.launchIO {
            updateManga.awaitUpdateFavorite(manga.id, true)
        }
    }

    private suspend fun getMangaCategoryIds(manga: Manga): List<Long> {
        return getCategories.await(manga.id)
            .map { it.id }
    }

    fun addFavorite(mangaId: Long) {
        screenModelScope.launchIO {
            val manga = getManga.await(mangaId) ?: return@launchIO

            val duplicates = getDuplicateLibraryManga(manga)
            if (duplicates.isNotEmpty()) {
                mutableState.update { it.copy(dialog = Dialog.DuplicateManga(manga, duplicates)) }
                return@launchIO
            }

            addFavorite(manga)
        }
    }

    fun addFavorite(manga: Manga) {
        screenModelScope.launchIO {
            // Move to default category if applicable
            val categories = getCategories()
            val defaultCategoryId = libraryPreferences.defaultCategory().get().toLong()
            val defaultCategory = categories.find { it.id == defaultCategoryId }

            when {
                // Default category set
                defaultCategory != null -> {
                    val result = updateManga.awaitUpdateFavorite(manga.id, true)
                    if (!result) return@launchIO
                    moveMangaToCategory(manga.id, defaultCategory)
                }

                // Automatic 'Default' or no categories
                defaultCategoryId == 0L || categories.isEmpty() -> {
                    val result = updateManga.awaitUpdateFavorite(manga.id, true)
                    if (!result) return@launchIO
                    moveMangaToCategory(manga.id, null)
                }

                // Choose a category
                else -> showChangeCategoryDialog(manga)
            }

            // Sync with tracking services if applicable
            addTracks.bindEnhancedTrackers(manga, sourceManager.getOrStub(manga.source))
        }
    }

    fun showMigrateDialog(target: Manga, current: Manga) {
        mutableState.update { currentState ->
            currentState.copy(dialog = Dialog.Migrate(target = target, current = current))
        }
    }

    fun showChangeCategoryDialog(manga: Manga) {
        screenModelScope.launch {
            val categories = getCategories()
            val selection = getMangaCategoryIds(manga)
            mutableState.update { currentState ->
                currentState.copy(
                    dialog = Dialog.ChangeCategory(
                        manga = manga,
                        initialSelection = categories.mapAsCheckboxState { it.id in selection }.toImmutableList(),
                    ),
                )
            }
        }
    }

    @Immutable
    data class State(
        val searchQuery: String? = null,
        val list: List<HistoryUiModel>? = null,
        val dialog: Dialog? = null,
        val historyScopeEnabled: Boolean = false,
        val activeCategory: Category? = null,
        val activeCategoryId: Long? = null,
        val categories: List<Category> = emptyList(),
        val categoryNavigationEnabled: Boolean = false,
        val categoryNavigationMode: LibraryPreferences.CategoryNavigationMode =
            LibraryPreferences.CategoryNavigationMode.DROPDOWN,
        val showNonLibraryEntries: Boolean = false,
        val hasNonLibraryEntries: Boolean = false,
        val categoryHistories: Map<Long, List<HistoryUiModel>> = emptyMap(),
    )

    sealed interface Dialog {
        data object DeleteAll : Dialog
        data class Delete(val history: HistoryWithRelations) : Dialog
        data class DuplicateManga(val manga: Manga, val duplicates: List<MangaWithChapterCount>) : Dialog
        data class ChangeCategory(
            val manga: Manga,
            val initialSelection: ImmutableList<CheckboxState<Category>>,
        ) : Dialog
        data class Migrate(val target: Manga, val current: Manga) : Dialog
    }

    sealed interface Event {
        data class OpenChapter(val chapter: Chapter?) : Event
        data object InternalError : Event
        data object HistoryCleared : Event
    }
}
