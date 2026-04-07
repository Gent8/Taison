package tachiyomi.domain.collection.model

data class CollectionCoverData(
    val mangaId: Long,
    val sourceId: Long,
    val thumbnailUrl: String?,
    val coverLastModified: Long,
    val isFavorite: Boolean,
)
