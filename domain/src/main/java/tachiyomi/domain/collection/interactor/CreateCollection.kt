package tachiyomi.domain.collection.interactor

import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.repository.CollectionRepository

class CreateCollection(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(name: String, description: String? = null): Long {
        val now = System.currentTimeMillis()
        val collection = Collection(
            name = name,
            description = description,
            createdAt = now,
            updatedAt = now,
        )
        return collectionRepository.insertCollection(collection)
    }

    suspend fun await(collection: Collection): Long {
        return collectionRepository.insertCollection(collection)
    }
}
