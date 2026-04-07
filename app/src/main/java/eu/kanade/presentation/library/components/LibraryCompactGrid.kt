package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.library.LibraryCollectionItem
import eu.kanade.tachiyomi.ui.library.LibraryGridItem
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover

@Composable
internal fun LibraryCompactGrid(
    items: List<LibraryGridItem>,
    showTitle: Boolean,
    columns: Int,
    contentPadding: PaddingValues,
    selection: Set<Long>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
    onClickCollection: (Long) -> Unit,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
) {
    LazyLibraryGrid(
        modifier = Modifier.fillMaxSize(),
        columns = columns,
        contentPadding = contentPadding,
    ) {
        globalSearchItem(searchQuery, onGlobalSearchClicked)

        items(
            items = items,
            contentType = { item ->
                when (item) {
                    is LibraryItem -> "library_compact_grid_item"
                    is LibraryCollectionItem -> "library_collection_grid_item"
                }
            },
        ) { gridItem ->
            when (gridItem) {
                is LibraryItem -> {
                    val manga = gridItem.libraryManga.manga
                    MangaCompactGridItem(
                        isSelected = manga.id in selection,
                        title = manga.title.takeIf { showTitle },
                        coverData = MangaCover(
                            mangaId = manga.id,
                            sourceId = manga.source,
                            isMangaFavorite = manga.favorite,
                            url = manga.thumbnailUrl,
                            lastModified = manga.coverLastModified,
                        ),
                        coverBadgeStart = {
                            DownloadsBadge(count = gridItem.downloadCount)
                            UnreadBadge(count = gridItem.unreadCount)
                        },
                        coverBadgeEnd = {
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
                    CollectionCompactGridItem(
                        title = gridItem.collection.name,
                        coverData = gridItem.coverData,
                        showTitle = showTitle,
                        onClick = { onClickCollection(gridItem.collection.id) },
                        onLongClick = {},
                    )
                }
            }
        }
    }
}
