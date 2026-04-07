package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.library.LibraryCollectionItem
import eu.kanade.tachiyomi.ui.library.LibraryGridItem
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.util.plus

@Composable
internal fun LibraryList(
    items: List<LibraryGridItem>,
    contentPadding: PaddingValues,
    selection: Set<Long>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
    onClickCollection: (Long) -> Unit,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
) {
    FastScrollLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
    ) {
        item {
            if (!searchQuery.isNullOrEmpty()) {
                GlobalSearchItem(
                    modifier = Modifier.fillMaxWidth(),
                    searchQuery = searchQuery,
                    onClick = onGlobalSearchClicked,
                )
            }
        }

        items(
            items = items,
            contentType = { item ->
                when (item) {
                    is LibraryItem -> "library_list_item"
                    is LibraryCollectionItem -> "library_collection_list_item"
                }
            },
        ) { gridItem ->
            when (gridItem) {
                is LibraryItem -> {
                    val manga = gridItem.libraryManga.manga
                    MangaListItem(
                        isSelected = manga.id in selection,
                        title = manga.title,
                        coverData = MangaCover(
                            mangaId = manga.id,
                            sourceId = manga.source,
                            isMangaFavorite = manga.favorite,
                            url = manga.thumbnailUrl,
                            lastModified = manga.coverLastModified,
                        ),
                        badge = {
                            DownloadsBadge(count = gridItem.downloadCount)
                            UnreadBadge(count = gridItem.unreadCount)
                            LanguageBadge(
                                isLocal = gridItem.isLocal,
                                sourceLanguage = gridItem.sourceLanguage,
                            )
                        },
                        onLongClick = { onLongClick(gridItem.libraryManga) },
                        onClick = { onClick(gridItem.libraryManga) },
                        onClickContinueReading = if (onClickContinueReading != null && gridItem.unreadCount > 0) {
                            { onClickContinueReading(gridItem.libraryManga) }
                        } else {
                            null
                        },
                    )
                }
                is LibraryCollectionItem -> {
                    CollectionListItem(
                        title = gridItem.collection.name,
                        coverData = gridItem.coverData,
                        entryCount = gridItem.entryCount,
                        onClick = { onClickCollection(gridItem.collection.id) },
                        onLongClick = {},
                    )
                }
            }
        }
    }
}
