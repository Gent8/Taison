package eu.kanade.presentation.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.history.components.HistoryItem
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    state: HistoryScreenModel.State,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String?) -> Unit,
    onToggleNonLibraryEntries: () -> Unit,
    onSelectCategory: (Long) -> Unit,
    onClickCover: (mangaId: Long) -> Unit,
    onClickResume: (mangaId: Long, chapterId: Long) -> Unit,
    onClickFavorite: (mangaId: Long) -> Unit,
    onDialogChange: (HistoryScreenModel.Dialog?) -> Unit,
) {
    val showToolbarDropdown = state.historyScopeEnabled &&
        state.categoryNavigationEnabled &&
        state.categoryNavigationMode == LibraryPreferences.CategoryNavigationMode.DROPDOWN &&
        state.categories.size > 1
    var toolbarDropdownExpanded by remember(showToolbarDropdown, state.categories) { mutableStateOf(false) }

    Scaffold(
        topBar = { scrollBehavior ->
            Box {
                SearchToolbar(
                    titleContent = {
                        val showSubtitle = state.historyScopeEnabled &&
                            !(state.categoryNavigationEnabled &&
                                state.categoryNavigationMode == LibraryPreferences.CategoryNavigationMode.TABS)
                        if (showToolbarDropdown) {
                            HistoryToolbarTitle(
                                title = stringResource(MR.strings.history),
                                label = state.activeCategory?.visualName ?: stringResource(MR.strings.label_default),
                                onExpandedChange = { toolbarDropdownExpanded = it },
                                expanded = toolbarDropdownExpanded,
                            )
                        } else {
                            val subtitle = if (showSubtitle) state.activeCategory?.visualName else null
                            AppBarTitle(
                                title = stringResource(MR.strings.history),
                                subtitle = subtitle,
                            )
                        }
                    },
                    searchQuery = state.searchQuery,
                    onChangeSearchQuery = onSearchQueryChange,
                    actions = {
                    if (state.historyScopeEnabled) {
                        val tooltip = if (state.showNonLibraryEntries) {
                            stringResource(MR.strings.history_hide_non_library_entries)
                        } else {
                            stringResource(MR.strings.history_show_non_library_entries)
                        }
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(tooltip) } },
                            state = rememberTooltipState(),
                            focusable = false,
                        ) {
                            IconToggleButton(
                                checked = state.showNonLibraryEntries,
                                onCheckedChange = { onToggleNonLibraryEntries() },
                                enabled = state.hasNonLibraryEntries,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Visibility,
                                    contentDescription = tooltip,
                                )
                            }
                        }
                    }
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text(stringResource(MR.strings.pref_clear_history))
                            }
                        },
                        state = rememberTooltipState(),
                        focusable = false,
                    ) {
                        IconButton(onClick = { onDialogChange(HistoryScreenModel.Dialog.DeleteAll) }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = stringResource(MR.strings.pref_clear_history),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                )
                DropdownMenu(
                    expanded = toolbarDropdownExpanded,
                    onDismissRequest = { toolbarDropdownExpanded = false },
                ) {
                    state.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.visualName) },
                            onClick = {
                                toolbarDropdownExpanded = false
                                onSelectCategory(category.id)
                            },
                            trailingIcon = if (category.id == state.activeCategoryId) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        state.list.let { historyList ->
            if (historyList == null) {
                LoadingScreen(Modifier.padding(contentPadding))
            } else {
                val emptyMessage = if (!state.searchQuery.isNullOrEmpty()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.information_no_recent_manga
                }
                Column(
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxSize(),
                ) {
                    val showCategoryTabs = state.historyScopeEnabled &&
                        state.categoryNavigationEnabled &&
                        state.categories.size > 1 &&
                        state.categoryNavigationMode == LibraryPreferences.CategoryNavigationMode.TABS
                    val selectedIndex = state.categories.indexOfFirst { category ->
                        category.id == state.activeCategoryId
                    }.takeIf { index -> index >= 0 } ?: 0
                    val pagerState = if (showCategoryTabs) {
                        rememberPagerState(initialPage = selectedIndex) {
                            state.categories.size
                        }
                    } else {
                        null
                    }
                    val coroutineScope = rememberCoroutineScope()

                    if (showCategoryTabs && pagerState != null) {
                        var pagerSelectionInProgress by remember(pagerState) { mutableStateOf(false) }

                        LaunchedEffect(state.activeCategoryId, state.categories) {
                            if (state.categories.isEmpty()) {
                                pagerSelectionInProgress = false
                                return@LaunchedEffect
                            }
                            val targetIndex = state.categories.indexOfFirst { it.id == state.activeCategoryId }
                                .takeIf { it >= 0 } ?: 0
                            val lastIndex = pagerState.pageCount - 1
                            if (lastIndex < 0) return@LaunchedEffect
                            val clamped = targetIndex.coerceIn(0, lastIndex)
                            if (pagerState.currentPage == clamped) {
                                pagerSelectionInProgress = false
                                return@LaunchedEffect
                            }
                            if (pagerSelectionInProgress) {
                                pagerSelectionInProgress = false
                                return@LaunchedEffect
                            }
                            pagerState.scrollToPage(clamped)
                        }

                        LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                            if (pagerState.isScrollInProgress) return@LaunchedEffect
                            if (state.categories.isEmpty()) return@LaunchedEffect
                            val currentPage = pagerState.currentPage.coerceIn(
                                0,
                                state.categories.lastIndex,
                            )
                            val categoryId = state.categories[currentPage].id
                            if (categoryId != state.activeCategoryId) {
                                pagerSelectionInProgress = true
                                onSelectCategory(categoryId)
                            }
                        }

                        HistoryCategoryTabs(
                            categories = state.categories,
                            pagerState = pagerState,
                            onTabItemClick = tab@{ index ->
                                if (index == pagerState.currentPage) return@tab
                                val targetCategoryId = state.categories.getOrNull(index)?.id ?: return@tab
                                pagerSelectionInProgress = true
                                onSelectCategory(targetCategoryId)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalPager(
                            modifier = Modifier.fillMaxSize(),
                            state = pagerState,
                            verticalAlignment = Alignment.Top,
                        ) { page ->
                            val categoryId = state.categories.getOrNull(page)?.id
                            val categoryHistory = when {
                                categoryId != null -> state.categoryHistories[categoryId].orEmpty()
                                else -> historyList
                            }
                            HistoryListOrEmpty(
                                history = categoryHistory,
                                emptyMessage = emptyMessage,
                                onClickCover = { historyItem -> onClickCover(historyItem.mangaId) },
                                onClickResume = { historyItem ->
                                    onClickResume(historyItem.mangaId, historyItem.chapterId)
                                },
                                onClickDelete = { item ->
                                    onDialogChange(HistoryScreenModel.Dialog.Delete(item))
                                },
                                onClickFavorite = { historyItem -> onClickFavorite(historyItem.mangaId) },
                            )
                        }
                    } else {
                        HistoryListOrEmpty(
                            history = historyList,
                            emptyMessage = emptyMessage,
                            onClickCover = { historyItem -> onClickCover(historyItem.mangaId) },
                            onClickResume = { historyItem ->
                                onClickResume(historyItem.mangaId, historyItem.chapterId)
                            },
                            onClickDelete = { item -> onDialogChange(HistoryScreenModel.Dialog.Delete(item)) },
                            onClickFavorite = { historyItem -> onClickFavorite(historyItem.mangaId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCategoryTabs(
    categories: List<Category>,
    pagerState: PagerState,
    onTabItemClick: (Int) -> Unit,
) {
    if (categories.isEmpty()) return
    val selectedIndex = pagerState.currentPage.coerceAtMost(categories.lastIndex)
    Column {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 0.dp,
            divider = {},
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { onTabItemClick(index) },
                    text = { Text(category.visualName) },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun HistoryListOrEmpty(
    history: List<HistoryUiModel>,
    emptyMessage: StringResource,
    onClickCover: (HistoryWithRelations) -> Unit,
    onClickResume: (HistoryWithRelations) -> Unit,
    onClickDelete: (HistoryWithRelations) -> Unit,
    onClickFavorite: (HistoryWithRelations) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (history.isEmpty()) {
            EmptyScreen(
                stringRes = emptyMessage,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            HistoryScreenContent(
                history = history,
                contentPadding = PaddingValues(),
                onClickCover = onClickCover,
                onClickResume = onClickResume,
                onClickDelete = onClickDelete,
                onClickFavorite = onClickFavorite,
            )
        }
    }
}

@Composable
private fun HistoryToolbarTitle(
    title: String,
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(!expanded) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
        )
    }
}

@Composable
private fun HistoryScreenContent(
    history: List<HistoryUiModel>,
    contentPadding: PaddingValues,
    onClickCover: (HistoryWithRelations) -> Unit,
    onClickResume: (HistoryWithRelations) -> Unit,
    onClickDelete: (HistoryWithRelations) -> Unit,
    onClickFavorite: (HistoryWithRelations) -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        items(
            items = history,
            key = { "history-${it.hashCode()}" },
            contentType = {
                when (it) {
                    is HistoryUiModel.Header -> "header"
                    is HistoryUiModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is HistoryUiModel.Header -> {
                    ListGroupHeader(
                        modifier = Modifier.animateItemFastScroll(),
                        text = relativeDateText(item.date),
                    )
                }
                is HistoryUiModel.Item -> {
                    val value = item.item
                    HistoryItem(
                        modifier = Modifier.animateItemFastScroll(),
                        history = value,
                        onClickCover = { onClickCover(value) },
                        onClickResume = { onClickResume(value) },
                        onClickDelete = { onClickDelete(value) },
                        onClickFavorite = { onClickFavorite(value) },
                    )
                }
            }
        }
    }
}

sealed interface HistoryUiModel {
    data class Header(val date: LocalDate) : HistoryUiModel
    data class Item(val item: HistoryWithRelations) : HistoryUiModel
}

@PreviewLightDark
@Composable
internal fun HistoryScreenPreviews(
    @PreviewParameter(HistoryScreenModelStateProvider::class)
    historyState: HistoryScreenModel.State,
) {
    TachiyomiPreviewTheme {
        HistoryScreen(
            state = historyState,
            snackbarHostState = SnackbarHostState(),
            onSearchQueryChange = {},
            onToggleNonLibraryEntries = {},
            onSelectCategory = {},
            onClickCover = {},
            onClickResume = { _, _ -> run {} },
            onDialogChange = {},
            onClickFavorite = {},
        )
    }
}
