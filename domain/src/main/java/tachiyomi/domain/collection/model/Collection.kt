package tachiyomi.domain.collection.model

import java.io.Serializable

data class Collection(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val coverMangaId: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) : Serializable
