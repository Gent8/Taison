package eu.kanade.presentation.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import eu.kanade.presentation.category.components.CategoryFloatingActionButton
import eu.kanade.presentation.category.components.CategoryListItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.tachiyomi.ui.category.CategoryScreenState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

@Composable
fun CategoryScreen(
    state: CategoryScreenState.Success,
    onClickCreate: () -> Unit,
    onClickRename: (Category) -> Unit,
    onClickDelete: (Category) -> Unit,
    onClickHide: (Category) -> Unit,
    onChangeOrder: (Category, Int) -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.action_edit_categories),
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            CategoryFloatingActionButton(
                lazyListState = lazyListState,
                onCreate = onClickCreate,
            )
        },
    ) { paddingValues ->
        CategoryContent(
            systemCategory = state.systemCategory,
            categories = state.categories,
            lazyListState = lazyListState,
            paddingValues = paddingValues,
            onClickRename = onClickRename,
            onClickDelete = onClickDelete,
            onClickHide = onClickHide,
            onChangeOrder = onChangeOrder,
        )
    }
}

@Composable
private fun CategoryContent(
    systemCategory: Category?,
    categories: List<Category>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onClickRename: (Category) -> Unit,
    onClickDelete: (Category) -> Unit,
    onClickHide: (Category) -> Unit,
    onChangeOrder: (Category, Int) -> Unit,
) {
    val categoriesState = remember { categories.toMutableStateList() }

    val reorderOffset = if (systemCategory != null &&
        categories.isNotEmpty()
    ) {
        2
    } else if (systemCategory != null) {
        1
    } else {
        0
    }

    val reorderableState = rememberReorderableLazyListState(lazyListState, paddingValues) { from, to ->
        val fromIndex = from.index - reorderOffset
        val toIndex = to.index - reorderOffset

        if (fromIndex >= 0 && toIndex >= 0 && fromIndex < categoriesState.size && toIndex < categoriesState.size) {
            val item = categoriesState.removeAt(fromIndex)
            categoriesState.add(toIndex, item)
            onChangeOrder(item, toIndex)
        }
    }

    LaunchedEffect(categories) {
        if (!reorderableState.isAnyItemDragging) {
            categoriesState.clear()
            categoriesState.addAll(categories)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = paddingValues +
            topSmallPaddingValues +
            PaddingValues(horizontal = MaterialTheme.padding.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        systemCategory?.let { category ->
            item(key = SYSTEM_CATEGORY_KEY) {
                ReorderableItem(reorderableState, SYSTEM_CATEGORY_KEY) {
                    CategoryListItem(
                        modifier = Modifier.animateItem(),
                        category = category,
                        onRename = { onClickRename(category) },
                        onDelete = { },
                        onHide = { onClickHide(category) },
                        canReorder = false,
                        canDelete = false,
                    )
                }
            }

            if (categories.isNotEmpty()) {
                item(key = DIVIDER_KEY) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = MaterialTheme.padding.small),
                    )
                }
            }
        }

        items(
            items = categoriesState,
            key = { category -> category.key },
        ) { category ->
            ReorderableItem(reorderableState, category.key) {
                CategoryListItem(
                    modifier = Modifier.animateItem(),
                    category = category,
                    onRename = { onClickRename(category) },
                    onDelete = { onClickDelete(category) },
                    onHide = { onClickHide(category) },
                    canReorder = true,
                    canDelete = true,
                )
            }
        }
    }
}

private const val SYSTEM_CATEGORY_KEY = "system-category"
private const val DIVIDER_KEY = "category-divider"
private val Category.key inline get() = "category-$id"
