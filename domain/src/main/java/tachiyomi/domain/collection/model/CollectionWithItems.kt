package tachiyomi.domain.collection.model

import tachiyomi.domain.manga.model.Manga

data class CollectionWithItems(
    val collection: Collection,
    val items: List<CollectionItemWithManga>,
)

data class CollectionItemWithManga(
    val item: CollectionItem,
    val manga: Manga,
)
