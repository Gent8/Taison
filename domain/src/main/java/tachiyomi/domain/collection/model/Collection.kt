package tachiyomi.domain.collection.model

import androidx.compose.runtime.Immutable
import java.io.Serializable

@Immutable
data class Collection(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val coverMangaId: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) : Serializable {

    companion object {
        fun create(
            name: String,
            description: String? = null,
        ) = Collection(
            id = 0,
            name = name,
            description = description,
            coverMangaId = null,
            sortOrder = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
    }
}
