package eu.kanade.presentation.collection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.collection.components.BadgeInputDialog
import eu.kanade.presentation.collection.components.CollectionItemRow
import eu.kanade.presentation.collection.components.ConfirmDeleteCollectionDialog
import eu.kanade.presentation.collection.components.EditCollectionDialog
import eu.kanade.tachiyomi.ui.collection.CollectionScreenModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    state: CollectionScreenModel.State.Success,
    onBackClick: () -> Unit,
    onMangaClick: (Long) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onConfirmDeleteClick: () -> Unit,
    onRemoveItemClick: (Long) -> Unit,
    onBadgeClick: (itemId: Long, currentBadge: String?) -> Unit,
    onUpdateBadgeClick: (Long, String?) -> Unit,
    onUpdateCollection: (name: String, description: String?) -> Unit,
    onDismissDialog: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val collection = state.collectionWithItems

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(collection.collection.name)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(MR.strings.action_bar_up_description),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(MR.strings.action_edit),
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(MR.strings.action_delete),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { contentPadding ->
        if (collection.items.isEmpty()) {
            EmptyScreen(
                stringRes = MR.strings.collection_empty,
                modifier = Modifier.padding(contentPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                // Description section
                collection.collection.description?.let { desc ->
                    item(key = "description") {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }

                // Item count
                item(key = "count") {
                    Text(
                        text = stringResource(MR.strings.collection_items_count, collection.items.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                // Manga items
                items(
                    items = collection.items,
                    key = { it.item.id },
                ) { itemWithManga ->
                    CollectionItemRow(
                        manga = itemWithManga.manga,
                        badge = itemWithManga.item.badge,
                        onClick = { onMangaClick(itemWithManga.manga.id) },
                        onRemoveClick = { onRemoveItemClick(itemWithManga.manga.id) },
                        onBadgeClick = {
                            onBadgeClick(itemWithManga.item.id, itemWithManga.item.badge)
                        },
                    )
                }
            }
        }

        // Dialogs
        when (val dialog = state.dialog) {
            is CollectionScreenModel.Dialog.Edit -> {
                EditCollectionDialog(
                    currentName = collection.collection.name,
                    currentDescription = collection.collection.description,
                    onDismiss = onDismissDialog,
                    onConfirm = { name, desc ->
                        onUpdateCollection(name, desc)
                    },
                )
            }
            is CollectionScreenModel.Dialog.Badge -> {
                BadgeInputDialog(
                    currentBadge = dialog.currentBadge,
                    onDismiss = onDismissDialog,
                    onConfirm = { badge ->
                        onUpdateBadgeClick(dialog.itemId, badge)
                    },
                )
            }
            is CollectionScreenModel.Dialog.ConfirmDelete -> {
                ConfirmDeleteCollectionDialog(
                    collectionName = collection.collection.name,
                    onDismiss = onDismissDialog,
                    onConfirm = onConfirmDeleteClick,
                )
            }
            null -> {}
        }
    }
}

@Composable
fun CollectionLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun CollectionErrorScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Collection not found",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
