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
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
import tachiyomi.domain.manga.interactor.GetLibraryManga
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
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
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
            val historyNavigationEnabledFlow = libraryPreferences
                .historyCategoryNavigation()
                .changes()
                .distinctUntilChanged()
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

            val showHiddenCategoriesFlow = libraryPreferences.showHiddenCategories().changes().distinctUntilChanged()

            val showDefaultCategoryFlow = combine(
                getLibraryManga.subscribe(),
                categoriesFlow,
                showHiddenCategoriesFlow,
            ) { libraryManga, categories, showHiddenCategories ->
                val systemCategory = categories.find { it.isSystemCategory }
                val hasMangaInDefault = libraryManga.any {
                    it.categories.isEmpty() || it.categories.contains(Category.UNCATEGORIZED_ID)
                }
                val hasCustomName = systemCategory?.name?.isNotBlank() == true
                val systemCategoryHasContent = hasCustomName || hasMangaInDefault
                systemCategory != null &&
                    systemCategoryHasContent &&
                    (showHiddenCategories || !systemCategory.hidden)
            }
                .onStart { emit(false) }
                .distinctUntilChanged()

            @Suppress("UNCHECKED_CAST")
            val filterInputsFlow = combine(
                showNonLibraryEntriesFlow,
                scopeEnabledFlow,
                activeCategoryIdFlow,
                categoriesFlow,
                historyNavigationEnabledFlow,
                showHiddenCategoriesFlow,
                showDefaultCategoryFlow,
            ) { values ->
                HistoryFilterInputs(
                    includeNonLibraryEntries = values[0] as Boolean,
                    scopeEnabled = values[1] as Boolean,
                    activeCategoryId = values[2] as Long,
                    categories = values[3] as List<Category>,
                    categoryNavigationEnabled = values[4] as Boolean,
                    showHiddenCategories = values[5] as Boolean,
                    showDefaultCategory = values[6] as Boolean,
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
                    showHiddenCategories = inputs.showHiddenCategories,
                    showDefaultCategory = inputs.showDefaultCategory,
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
                    val allNavigationCategories =
                        buildCategoryNavigation(config.categories, config.showHiddenCategories)
                    val categoryHistories = if (config.scopeEnabled) {
                        buildCategoryHistories(
                            history = histories,
                            categories = allNavigationCategories,
                            includeNonLibraryEntries = config.includeNonLibraryEntries,
                        )
                    } else {
                        emptyMap()
                    }
                    val navigationCategories = if (config.scopeEnabled) {
                        allNavigationCategories.filter { category ->
                            category.id != Category.UNCATEGORIZED_ID || config.showDefaultCategory
                        }
                    } else {
                        allNavigationCategories
                    }
                    val filteredHistory = when (resolvedCategoryId) {
                        null -> histories.toHistoryUiModels()
                        else -> categoryHistories[resolvedCategoryId].orEmpty().toHistoryUiModels()
                    }
                    val activeCategory = resolvedCategoryId?.let { id ->
                        navigationCategories.firstOrNull { it.id == id }
                            ?: config.categories.firstOrNull { it.id == id }
                    }
                    HistoryFilterResult(
                        uiModels = filteredHistory.toImmutableList(),
                        activeCategory = activeCategory,
                        scopeActive = resolvedCategoryId != null,
                        hasNonLibraryEntries = histories.any { !it.coverData.isMangaFavorite },
                        categories = navigationCategories.toImmutableList(),
                        categoryNavigationEnabled = config.categoryNavigationEnabled,
                        categoryNavigationMode = config.navigationMode,
                        activeCategoryId = resolvedCategoryId,
                        categoryHistories = categoryHistories
                            .mapValues { (_, value) -> value.toImmutableList() }
                            .toPersistentHashMap(),
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

    private fun buildCategoryNavigation(categories: List<Category>, showHiddenCategories: Boolean): List<Category> {
        val systemCategory = categories.find { it.isSystemCategory }
        val userCategories = categories.filter {
            !it.isSystemCategory && (showHiddenCategories || !it.hidden)
        }
        return if (systemCategory != null) {
            listOf(systemCategory) + userCategories
        } else {
            userCategories
        }
    }

    private fun buildCategoryHistories(
        history: List<HistoryWithRelations>,
        categories: List<Category>,
        includeNonLibraryEntries: Boolean,
    ): Map<Long, List<HistoryWithRelations>> {
        if (categories.isEmpty()) return emptyMap()

        val categoryBuckets = mutableMapOf<Long, MutableList<HistoryWithRelations>>()
        categories.forEach { category ->
            categoryBuckets[category.id] = mutableListOf()
        }

        val defaultBucket = categoryBuckets[Category.UNCATEGORIZED_ID]
        val includeNonLibrary = includeNonLibraryEntries && categories.isNotEmpty()

        history.forEach { entry ->
            val isNonLibraryEntry = !entry.coverData.isMangaFavorite
            if (isNonLibraryEntry) {
                if (includeNonLibrary) {
                    categoryBuckets.values.forEach { bucket -> bucket.add(entry) }
                }
                return@forEach
            }

            var assigned = false
            val entryCategoryIds = entry.categoryIds
            if (entryCategoryIds.isEmpty()) {
                defaultBucket?.add(entry)
                assigned = true
            } else {
                entryCategoryIds.forEach { categoryId ->
                    when {
                        categoryId == Category.UNCATEGORIZED_ID -> {
                            defaultBucket?.add(entry)
                            assigned = true
                        }
                        categoryBuckets.containsKey(categoryId) -> {
                            categoryBuckets[categoryId]?.add(entry)
                            assigned = true
                        }
                    }
                }
            }

            if (!assigned) {
                defaultBucket?.add(entry)
            }
        }

        return categoryBuckets.mapValues { (_, entries) -> entries.toList() }
    }

    private data class HistoryFilterInputs(
        val includeNonLibraryEntries: Boolean,
        val scopeEnabled: Boolean,
        val activeCategoryId: Long,
        val categories: List<Category>,
        val categoryNavigationEnabled: Boolean,
        val showHiddenCategories: Boolean,
        val showDefaultCategory: Boolean,
    )

    private data class HistoryFilterConfig(
        val includeNonLibraryEntries: Boolean,
        val scopeEnabled: Boolean,
        val activeCategoryId: Long,
        val categories: List<Category>,
        val categoryNavigationEnabled: Boolean,
        val navigationMode: LibraryPreferences.CategoryNavigationMode,
        val showHiddenCategories: Boolean,
        val showDefaultCategory: Boolean,
    ) {
        fun resolvedCategoryId(): Long? {
            if (!scopeEnabled) return null
            if (activeCategoryId < 0) return null

            if (activeCategoryId == 0L) return 0L
            return categories.firstOrNull { it.id == activeCategoryId }?.id
        }
    }

    private data class HistoryFilterResult(
        val uiModels: ImmutableList<HistoryUiModel>,
        val activeCategory: Category?,
        val scopeActive: Boolean,
        val hasNonLibraryEntries: Boolean,
        val categories: ImmutableList<Category>,
        val categoryNavigationEnabled: Boolean,
        val categoryNavigationMode: LibraryPreferences.CategoryNavigationMode,
        val activeCategoryId: Long?,
        val categoryHistories: ImmutableMap<Long, ImmutableList<HistoryWithRelations>>,
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

    fun clearHistory(scope: HistoryDeletionScope) {
        screenModelScope.launchIO {
            when (scope) {
                HistoryDeletionScope.EVERYTHING -> {
                    val result = removeHistory.awaitAll()
                    if (!result) return@launchIO
                }
                HistoryDeletionScope.ACTIVE_SCOPE -> {
                    val mangaIds = getEntriesForActiveScope()
                        .map { it.mangaId }
                        .distinct()
                    if (mangaIds.isEmpty()) return@launchIO
                    mangaIds.forEach { removeHistory.await(it) }
                }
            }
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
        val list: ImmutableList<HistoryUiModel>? = null,
        val dialog: Dialog? = null,
        val historyScopeEnabled: Boolean = false,
        val activeCategory: Category? = null,
        val activeCategoryId: Long? = null,
        val categories: ImmutableList<Category> = persistentListOf(),
        val categoryNavigationEnabled: Boolean = false,
        val categoryNavigationMode: LibraryPreferences.CategoryNavigationMode =
            LibraryPreferences.CategoryNavigationMode.DROPDOWN,
        val showNonLibraryEntries: Boolean = false,
        val hasNonLibraryEntries: Boolean = false,
        val categoryHistories: ImmutableMap<Long, ImmutableList<HistoryWithRelations>> = persistentMapOf(),
    )

    sealed interface Dialog {
        data class DeleteAll(val scope: HistoryDeletionScope = HistoryDeletionScope.ACTIVE_SCOPE) : Dialog
        data class Delete(val history: HistoryWithRelations) : Dialog
        data class DuplicateManga(val manga: Manga, val duplicates: List<MangaWithChapterCount>) : Dialog
        data class ChangeCategory(
            val manga: Manga,
            val initialSelection: ImmutableList<CheckboxState<Category>>,
        ) : Dialog
        data class Migrate(val target: Manga, val current: Manga) : Dialog
    }

    enum class HistoryDeletionScope {
        EVERYTHING,
        ACTIVE_SCOPE,
    }

    sealed interface Event {
        data class OpenChapter(val chapter: Chapter?) : Event
        data object InternalError : Event
        data object HistoryCleared : Event
    }

    private fun getEntriesForActiveScope(): List<HistoryWithRelations> {
        val currentState = state.value
        if (!currentState.historyScopeEnabled) return emptyList()
        val categoryId = currentState.activeCategoryId ?: return emptyList()
        return currentState.categoryHistories[categoryId].orEmpty()
    }
}

internal fun List<HistoryWithRelations>.toHistoryUiModels(): List<HistoryUiModel> {
    return map { HistoryUiModel.Item(it) }
        .insertSeparators { before, after ->
            val beforeDate = before?.item?.readAt?.time?.toLocalDate()
            val afterDate = after?.item?.readAt?.time?.toLocalDate()
            when {
                beforeDate != afterDate && afterDate != null -> HistoryUiModel.Header(afterDate)
                else -> null
            }
        }
}
