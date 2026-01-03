package tachiyomi.domain.collection.model

data class CollectionUpdate(
    val id: Long,
    val name: String? = null,
    val description: String? = null,
    val coverMangaId: Long? = null,
    val sortOrder: Int? = null,
)
