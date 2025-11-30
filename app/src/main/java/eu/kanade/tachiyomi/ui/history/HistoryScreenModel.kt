package eu.kanade.tachiyomi.ui.history

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.category.LastUsedCategoryState
import eu.kanade.tachiyomi.util.lang.toLocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentHashMap
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
import tachiyomi.domain.library.model.HistoryScopeMode
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetDuplicateLibraryManga
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.source.local.LocalSource
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
    private val preferences: BasePreferences = Injekt.get(),
) : StateScreenModel<HistoryScreenModel.State>(State()) {

    private val context: Context
        get() = preferences.context

    private val _events: Channel<Event> = Channel(Channel.UNLIMITED)
    val events: Flow<Event> = _events.receiveAsFlow()

    init {
        screenModelScope.launch {
            val searchQueryFlow = state.map { it.searchQuery }.distinctUntilChanged()
            val showNonLibraryEntriesFlow = state.map { it.showNonLibraryEntries }.distinctUntilChanged()
            val scopeModeFlow = libraryPreferences.groupLibraryBy()
                .changes()
                .map { HistoryScopeMode.fromLibraryGroup(it) }
                .distinctUntilChanged()
            val lastUsedHistorySectionIdFlow = libraryPreferences.lastUsedHistorySectionId().changes()
            val activeSectionIdFlow = combine(
                scopeModeFlow,
                lastUsedHistorySectionIdFlow,
                lastUsedCategoryState.state,
            ) { scopeMode, historySectionId, categoryId ->
                when (scopeMode) {
                    HistoryScopeMode.BY_CATEGORY -> categoryId
                    else -> if (historySectionId >= 0) historySectionId else 0L
                }
            }.distinctUntilChanged()
            val categoriesFlow = getCategories.subscribe().distinctUntilChanged()
            val historyNavigationEnabledFlow = libraryPreferences
                .historySectionNavigation()
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
                scopeModeFlow,
                activeSectionIdFlow,
                categoriesFlow,
                historyNavigationEnabledFlow,
                showHiddenCategoriesFlow,
                showDefaultCategoryFlow,
            ) { values ->
                HistoryFilterInputs(
                    includeNonLibraryEntries = values[0] as Boolean,
                    scopeMode = values[1] as HistoryScopeMode,
                    activeSectionId = values[2] as Long,
                    categories = values[3] as List<Category>,
                    sectionNavigationEnabled = values[4] as Boolean,
                    showHiddenCategories = values[5] as Boolean,
                    showDefaultCategory = values[6] as Boolean,
                )
            }

            val libraryMangaFlow = getLibraryManga.subscribe().distinctUntilChanged()

            val filterConfigFlow = combine(
                filterInputsFlow,
                navigationModeFlow,
                libraryMangaFlow,
            ) { inputs, navigationMode, libraryManga ->
                HistoryFilterConfig(
                    includeNonLibraryEntries = inputs.includeNonLibraryEntries,
                    scopeMode = inputs.scopeMode,
                    activeSectionId = inputs.activeSectionId,
                    categories = inputs.categories,
                    sectionNavigationEnabled = inputs.sectionNavigationEnabled,
                    navigationMode = navigationMode,
                    showHiddenCategories = inputs.showHiddenCategories,
                    showDefaultCategory = inputs.showDefaultCategory,
                    libraryManga = libraryManga,
                )
            }

            combine(
                historyFlow,
                filterConfigFlow,
            ) { histories, config ->
                histories to config
            }
                .map { (histories, config) ->
                    val scopeMode = config.scopeMode
                    val isScoped = scopeMode != HistoryScopeMode.UNGROUPED &&
                        config.sectionNavigationEnabled

                    val sections = when (scopeMode) {
                        HistoryScopeMode.BY_CATEGORY -> buildCategorySections(
                            categories = config.categories,
                            showHiddenCategories = config.showHiddenCategories,
                            showDefaultCategory = config.showDefaultCategory,
                        )
                        HistoryScopeMode.BY_SOURCE -> buildSourceSections(config.libraryManga)
                        HistoryScopeMode.BY_STATUS -> buildStatusSections(config.libraryManga)
                        HistoryScopeMode.UNGROUPED -> emptyList()
                    }

                    val sectionHistories = if (isScoped && sections.isNotEmpty()) {
                        buildSectionHistories(
                            history = histories,
                            sections = sections,
                            scopeMode = scopeMode,
                            includeNonLibraryEntries = config.includeNonLibraryEntries,
                        )
                    } else {
                        emptyMap()
                    }

                    val resolvedSectionId = config.resolvedSectionId(sections)
                    val filteredHistory = when {
                        !isScoped -> histories.toHistoryUiModels()
                        resolvedSectionId != null -> sectionHistories[resolvedSectionId].orEmpty().toHistoryUiModels()
                        else -> histories.toHistoryUiModels()
                    }

                    val activeSection = resolvedSectionId?.let { id ->
                        sections.firstOrNull { it.id == id }
                    }

                    HistoryFilterResult(
                        uiModels = filteredHistory.toImmutableList(),
                        activeSection = activeSection,
                        scopeActive = isScoped && resolvedSectionId != null,
                        hasNonLibraryEntries = histories.any { !it.coverData.isMangaFavorite },
                        sections = sections.toImmutableList(),
                        sectionNavigationEnabled = config.sectionNavigationEnabled,
                        navigationMode = config.navigationMode,
                        activeSectionId = resolvedSectionId,
                        sectionHistories = sectionHistories
                            .mapValues { (_, value) -> value.toImmutableList() }
                            .toPersistentHashMap(),
                        scopeMode = scopeMode,
                    )
                }
                .flowOn(Dispatchers.IO)
                .collect { result ->
                    mutableState.update { currentState ->
                        currentState.copy(
                            list = result.uiModels,
                            historyScopeEnabled = result.scopeActive,
                            activeSection = result.activeSection,
                            hasNonLibraryEntries = result.hasNonLibraryEntries,
                            sections = result.sections,
                            sectionNavigationEnabled = result.sectionNavigationEnabled,
                            navigationMode = result.navigationMode,
                            activeSectionId = result.activeSectionId,
                            sectionHistories = result.sectionHistories,
                            scopeMode = result.scopeMode,
                        )
                    }
                }
        }
    }

    data class HistorySection(
        val id: Long,
        val name: String,
        val order: Long,
    )

    private fun buildCategorySections(
        categories: List<Category>,
        showHiddenCategories: Boolean,
        showDefaultCategory: Boolean,
    ): List<HistorySection> {
        val systemCategory = categories.find { it.isSystemCategory }
        val userCategories = categories.filter {
            !it.isSystemCategory && (showHiddenCategories || !it.hidden)
        }
        val allCategories = if (systemCategory != null) {
            listOf(systemCategory) + userCategories
        } else {
            userCategories
        }
        return allCategories
            .filter { it.id != Category.UNCATEGORIZED_ID || showDefaultCategory }
            .map { category ->
                HistorySection(
                    id = category.id,
                    name = getCategoryVisualName(category),
                    order = category.order,
                )
            }
    }

    private fun getCategoryVisualName(category: Category): String {
        return when {
            category.isSystemCategory && category.name.isBlank() -> context.stringResource(MR.strings.label_default)
            else -> category.name
        }
    }

    private fun buildSourceSections(libraryManga: List<LibraryManga>): List<HistorySection> {
        val sourceIds = libraryManga
            .map { it.manga.source }
            .distinct()

        val sources = sourceIds.map { sourceManager.getOrStub(it) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.ifBlank { it.id.toString() } })

        return sources.mapIndexed { index, source ->
            HistorySection(
                id = source.id,
                name = if (source.id == LocalSource.ID) {
                    context.stringResource(MR.strings.local_source)
                } else {
                    source.name.ifBlank { source.id.toString() }
                },
                order = index.toLong(),
            )
        }
    }

    private fun buildStatusSections(libraryManga: List<LibraryManga>): List<HistorySection> {
        val statuses = libraryManga
            .map { it.manga.status }
            .distinct()

        return statuses.mapNotNull { status ->
            val (nameRes, order) = statusMap[status] ?: return@mapNotNull null
            HistorySection(
                id = status,
                name = context.stringResource(nameRes),
                order = order,
            )
        }.sortedBy { it.order }
    }

    private val statusMap = mapOf(
        SManga.ONGOING.toLong() to (MR.strings.ongoing to 1L),
        SManga.COMPLETED.toLong() to (MR.strings.completed to 2L),
        SManga.PUBLISHING_FINISHED.toLong() to (MR.strings.publishing_finished to 3L),
        SManga.LICENSED.toLong() to (MR.strings.licensed to 4L),
        SManga.ON_HIATUS.toLong() to (MR.strings.on_hiatus to 5L),
        SManga.CANCELLED.toLong() to (MR.strings.cancelled to 6L),
        SManga.UNKNOWN.toLong() to (MR.strings.unknown to 7L),
    )

    private fun buildSectionHistories(
        history: List<HistoryWithRelations>,
        sections: List<HistorySection>,
        scopeMode: HistoryScopeMode,
        includeNonLibraryEntries: Boolean,
    ): Map<Long, List<HistoryWithRelations>> {
        if (sections.isEmpty()) return emptyMap()

        val sectionBuckets = mutableMapOf<Long, MutableList<HistoryWithRelations>>()
        sections.forEach { section ->
            sectionBuckets[section.id] = mutableListOf()
        }

        history.forEach { entry ->
            val isNonLibraryEntry = !entry.coverData.isMangaFavorite

            when (scopeMode) {
                HistoryScopeMode.BY_CATEGORY -> {
                    if (isNonLibraryEntry) {
                        if (includeNonLibraryEntries) {
                            sectionBuckets.values.forEach { bucket -> bucket.add(entry) }
                        }
                        return@forEach
                    }

                    val entryCategoryIds = entry.categoryIds
                    var assigned = false
                    if (entryCategoryIds.isEmpty()) {
                        sectionBuckets[Category.UNCATEGORIZED_ID]?.add(entry)
                        assigned = true
                    } else {
                        entryCategoryIds.forEach { categoryId ->
                            if (sectionBuckets.containsKey(categoryId)) {
                                sectionBuckets[categoryId]?.add(entry)
                                assigned = true
                            } else if (categoryId == Category.UNCATEGORIZED_ID) {
                                sectionBuckets[Category.UNCATEGORIZED_ID]?.add(entry)
                                assigned = true
                            }
                        }
                    }
                    if (!assigned) {
                        sectionBuckets[Category.UNCATEGORIZED_ID]?.add(entry)
                    }
                }
                HistoryScopeMode.BY_SOURCE -> {
                    if (isNonLibraryEntry) {
                        if (includeNonLibraryEntries) {
                            sectionBuckets.values.forEach { bucket -> bucket.add(entry) }
                        }
                        return@forEach
                    }

                    val sourceId = entry.coverData.sourceId
                    sectionBuckets[sourceId]?.add(entry)
                }
                HistoryScopeMode.BY_STATUS -> {
                    if (isNonLibraryEntry) {
                        if (includeNonLibraryEntries) {
                            sectionBuckets.values.forEach { bucket -> bucket.add(entry) }
                        }
                        return@forEach
                    }

                    val status = entry.status
                    sectionBuckets[status]?.add(entry)
                }
                HistoryScopeMode.UNGROUPED -> {
                }
            }
        }

        return sectionBuckets.mapValues { (_, entries) -> entries.toList() }
    }

    private data class HistoryFilterInputs(
        val includeNonLibraryEntries: Boolean,
        val scopeMode: HistoryScopeMode,
        val activeSectionId: Long,
        val categories: List<Category>,
        val sectionNavigationEnabled: Boolean,
        val showHiddenCategories: Boolean,
        val showDefaultCategory: Boolean,
    )

    private data class HistoryFilterConfig(
        val includeNonLibraryEntries: Boolean,
        val scopeMode: HistoryScopeMode,
        val activeSectionId: Long,
        val categories: List<Category>,
        val sectionNavigationEnabled: Boolean,
        val navigationMode: LibraryPreferences.CategoryNavigationMode,
        val showHiddenCategories: Boolean,
        val showDefaultCategory: Boolean,
        val libraryManga: List<LibraryManga>,
    ) {
        fun resolvedSectionId(sections: List<HistorySection>): Long? {
            if (scopeMode == HistoryScopeMode.UNGROUPED) return null
            if (activeSectionId < 0) return null
            if (sections.isEmpty()) return null

            if (scopeMode == HistoryScopeMode.BY_CATEGORY && activeSectionId == 0L) {
                return if (sections.any { it.id == 0L }) 0L else sections.firstOrNull()?.id
            }

            return if (sections.any { it.id == activeSectionId }) {
                activeSectionId
            } else {
                sections.firstOrNull()?.id
            }
        }
    }

    private data class HistoryFilterResult(
        val uiModels: ImmutableList<HistoryUiModel>,
        val activeSection: HistorySection?,
        val scopeActive: Boolean,
        val hasNonLibraryEntries: Boolean,
        val sections: ImmutableList<HistorySection>,
        val sectionNavigationEnabled: Boolean,
        val navigationMode: LibraryPreferences.CategoryNavigationMode,
        val activeSectionId: Long?,
        val sectionHistories: ImmutableMap<Long, ImmutableList<HistoryWithRelations>>,
        val scopeMode: HistoryScopeMode,
    )


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

    fun updateActiveSection(sectionId: Long) {
        mutableState.update { current ->
            val activeSection = current.sections.firstOrNull { it.id == sectionId }
            current.copy(
                activeSectionId = sectionId,
                activeSection = activeSection,
            )
        }
        screenModelScope.launchIO {
            libraryPreferences.lastUsedHistorySectionId().set(sectionId)
            if (state.value.scopeMode == HistoryScopeMode.BY_CATEGORY) {
                lastUsedCategoryState.set(sectionId)
                val sections = state.value.sections
                val index = sections.indexOfFirst { it.id == sectionId }.let { found ->
                    if (found >= 0) found else 0
                }
                libraryPreferences.lastUsedCategory().set(index)
            }
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
        val activeSection: HistorySection? = null,
        val activeSectionId: Long? = null,
        val sections: ImmutableList<HistorySection> = persistentListOf(),
        val sectionNavigationEnabled: Boolean = false,
        val navigationMode: LibraryPreferences.CategoryNavigationMode =
            LibraryPreferences.CategoryNavigationMode.DROPDOWN,
        val showNonLibraryEntries: Boolean = false,
        val hasNonLibraryEntries: Boolean = false,
        val sectionHistories: ImmutableMap<Long, ImmutableList<HistoryWithRelations>> = persistentMapOf(),
        val scopeMode: HistoryScopeMode = HistoryScopeMode.BY_CATEGORY,
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
        val sectionId = currentState.activeSectionId ?: return emptyList()
        return currentState.sectionHistories[sectionId].orEmpty()
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
