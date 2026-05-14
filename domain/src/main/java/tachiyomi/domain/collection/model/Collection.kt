package tachiyomi.domain.collection.model

import java.io.Serializable

data class Collection(
    val id: Long,
    val name: String,
    val description: String = "",
    val order: Long,
    val createdAt: Long,
    val categoryId: Long? = null,
) : Serializable
