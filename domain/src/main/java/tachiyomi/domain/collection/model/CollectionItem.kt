package tachiyomi.domain.collection.model

import java.io.Serializable

data class CollectionItem(
    val id: Long = 0,
    val collectionId: Long,
    val mangaId: Long,
    val sortOrder: Int = 0,
    val badge: String? = null,
) : Serializable
