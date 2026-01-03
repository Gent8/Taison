package tachiyomi.domain.collection.model

import androidx.compose.runtime.Immutable
import tachiyomi.domain.manga.model.Manga

@Immutable
data class CollectionWithItems(
    val collection: Collection,
    val items: List<CollectionItemWithManga>,
)

@Immutable
data class CollectionItemWithManga(
    val item: CollectionItem,
    val manga: Manga,
)
