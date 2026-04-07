package tachiyomi.domain.collection.model

import tachiyomi.domain.manga.model.Manga

data class CollectionEntryWithManga(
    val entry: CollectionEntry,
    val manga: Manga,
)
