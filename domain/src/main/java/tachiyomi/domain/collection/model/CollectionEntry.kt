package tachiyomi.domain.collection.model

data class CollectionEntry(
    val id: Long,
    val collectionId: Long,
    val mangaId: Long,
    val position: Long,
    val label: String,
)
