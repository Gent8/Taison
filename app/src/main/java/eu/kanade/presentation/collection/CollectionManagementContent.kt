package eu.kanade.presentation.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import eu.kanade.presentation.category.components.CategoryFloatingActionButton
import eu.kanade.presentation.collection.components.CollectionListItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.tachiyomi.ui.collection.CollectionManagementState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.collection.model.Collection
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

@Composable
fun CollectionManagementContent(
    state: CollectionManagementState.Success,
    onClickCreate: () -> Unit,
    onClickRename: (Collection) -> Unit,
    onClickDelete: (Collection) -> Unit,
    onClickCollection: (Collection) -> Unit,
    onChangeOrder: (Collection, Int) -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.action_manage_collections),
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
        if (state.isEmpty) {
            EmptyScreen(
                stringRes = MR.strings.collection_empty,
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            CollectionList(
                collections = state.collections,
                lazyListState = lazyListState,
                paddingValues = paddingValues,
                onClickCollection = onClickCollection,
                onClickRename = onClickRename,
                onClickDelete = onClickDelete,
                onChangeOrder = onChangeOrder,
            )
        }
    }
}

@Composable
private fun CollectionList(
    collections: List<Collection>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onClickCollection: (Collection) -> Unit,
    onClickRename: (Collection) -> Unit,
    onClickDelete: (Collection) -> Unit,
    onChangeOrder: (Collection, Int) -> Unit,
) {
    val collectionsState = remember { collections.toMutableStateList() }

    val reorderableState = rememberReorderableLazyListState(lazyListState, paddingValues) { from, to ->
        val fromIndex = from.index
        val toIndex = to.index

        if (fromIndex >= 0 && toIndex >= 0 && fromIndex < collectionsState.size && toIndex < collectionsState.size) {
            val item = collectionsState.removeAt(fromIndex)
            collectionsState.add(toIndex, item)
            onChangeOrder(item, toIndex)
        }
    }

    LaunchedEffect(collections) {
        if (!reorderableState.isAnyItemDragging) {
            collectionsState.clear()
            collectionsState.addAll(collections)
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
        items(
            items = collectionsState,
            key = { collection -> "collection-${collection.id}" },
        ) { collection ->
            ReorderableItem(reorderableState, "collection-${collection.id}") {
                CollectionListItem(
                    modifier = Modifier.animateItem(),
                    collection = collection,
                    onClick = { onClickCollection(collection) },
                    onRename = { onClickRename(collection) },
                    onDelete = { onClickDelete(collection) },
                )
            }
        }
    }
}
