package tachiyomi.domain.collection.model

data class CollectionItemUpdate(
    val id: Long,
    val sortOrder: Int? = null,
    val badge: String? = null,
)
