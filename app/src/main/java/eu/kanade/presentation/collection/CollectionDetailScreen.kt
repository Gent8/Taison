package eu.kanade.presentation.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.collection.components.CollectionEntryEditRow
import eu.kanade.presentation.collection.components.CollectionEntryRow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.tachiyomi.ui.collection.CollectionScreenState
import kotlinx.collections.immutable.persistentListOf
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.collection.model.CollectionEntryWithManga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

@Composable
fun CollectionDetailScreen(
    state: CollectionScreenState.Success,
    onClickReadEntry: (Long) -> Unit,
    onClickNavigateToManga: (Long) -> Unit,
    onClickRename: () -> Unit,
    onClickDelete: () -> Unit,
    onClickEditMode: () -> Unit,
    onClickChangeLabel: (Long, String) -> Unit,
    onClickRemoveEntry: (Long, String) -> Unit,
    onChangeOrder: (Long, Int) -> Unit,
    onClickEditDescription: () -> Unit,
    onClickContinueReading: () -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = state.collection.name,
                navigateUp = navigateUp,
                actions = {
                    AppBarActions(
                        persistentListOf(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_rename_collection),
                                onClick = onClickRename,
                            ),
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_delete_collection),
                                onClick = onClickDelete,
                            ),
                        ),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (!state.isEmpty) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SmallFloatingActionButton(
                        onClick = onClickEditMode,
                        containerColor = if (state.isEditMode) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(MR.strings.action_edit),
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = onClickContinueReading,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        if (state.isEmpty) {
            EmptyScreen(
                stringRes = MR.strings.collection_empty,
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            if (state.isEditMode) {
                CollectionEditContent(
                    entries = state.entries,
                    lazyListState = lazyListState,
                    paddingValues = paddingValues,
                    description = state.collection.description,
                    onClickEditDescription = onClickEditDescription,
                    onClickChangeLabel = onClickChangeLabel,
                    onClickRemoveEntry = onClickRemoveEntry,
                    onChangeOrder = onChangeOrder,
                )
            } else {
                CollectionViewContent(
                    entries = state.entries,
                    lazyListState = lazyListState,
                    paddingValues = paddingValues,
                    description = state.collection.description,
                    entryCount = state.entries.size,
                    onClickReadEntry = onClickReadEntry,
                    onClickNavigateToManga = onClickNavigateToManga,
                )
            }
        }
    }
}

@Composable
private fun CollectionViewContent(
    entries: List<CollectionEntryWithManga>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    description: String,
    entryCount: Int,
    onClickReadEntry: (Long) -> Unit,
    onClickNavigateToManga: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = paddingValues + topSmallPaddingValues,
    ) {
        // Description header
        if (description.isNotEmpty()) {
            item(key = "description") {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.padding.medium)
                        .padding(bottom = MaterialTheme.padding.small),
                )
            }
        }

        // Entry count header
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.padding.medium,
                        vertical = MaterialTheme.padding.small,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$entryCount entries",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
        }

        // Entries
        items(
            items = entries,
            key = { entry -> "entry-${entry.entry.id}" },
        ) { item ->
            CollectionEntryRow(
                entry = item,
                onClickRead = { onClickReadEntry(item.manga.id) },
                onClickNavigateToManga = { onClickNavigateToManga(item.manga.id) },
            )
        }
    }
}

@Composable
private fun CollectionEditContent(
    entries: List<CollectionEntryWithManga>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    description: String,
    onClickEditDescription: () -> Unit,
    onClickChangeLabel: (Long, String) -> Unit,
    onClickRemoveEntry: (Long, String) -> Unit,
    onChangeOrder: (Long, Int) -> Unit,
) {
    val entriesState = remember { entries.toMutableStateList() }

    val reorderableState = rememberReorderableLazyListState(lazyListState, paddingValues) { from, to ->
        // Offset by header items (description + count header)
        val headerCount = if (description.isNotEmpty()) 2 else 1
        val fromIndex = from.index - headerCount
        val toIndex = to.index - headerCount

        if (fromIndex >= 0 && toIndex >= 0 && fromIndex < entriesState.size && toIndex < entriesState.size) {
            val item = entriesState.removeAt(fromIndex)
            entriesState.add(toIndex, item)
            onChangeOrder(item.entry.id, toIndex)
        }
    }

    LaunchedEffect(entries) {
        if (!reorderableState.isAnyItemDragging) {
            entriesState.clear()
            entriesState.addAll(entries)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = paddingValues + topSmallPaddingValues,
    ) {
        // Description header (clickable to edit)
        item(key = "description") {
            Text(
                text = description.ifEmpty { stringResource(MR.strings.collection_add_description) },
                style = MaterialTheme.typography.bodyMedium,
                color = if (description.isEmpty()) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClickEditDescription)
                    .padding(horizontal = MaterialTheme.padding.medium)
                    .padding(bottom = MaterialTheme.padding.small),
            )
        }

        // Entry count header
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.padding.medium,
                        vertical = MaterialTheme.padding.small,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${entriesState.size} entries",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
        }

        // Reorderable entries
        items(
            items = entriesState,
            key = { entry -> "entry-${entry.entry.id}" },
        ) { item ->
            ReorderableItem(reorderableState, "entry-${item.entry.id}") {
                CollectionEntryEditRow(
                    modifier = Modifier.animateItem(),
                    entry = item,
                    onClickChangeLabel = {
                        onClickChangeLabel(item.entry.id, item.entry.label)
                    },
                    onClickRemove = {
                        onClickRemoveEntry(item.entry.id, item.manga.title)
                    },
                )
            }
        }
    }
}
