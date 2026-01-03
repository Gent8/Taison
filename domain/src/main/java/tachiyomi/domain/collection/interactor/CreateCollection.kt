package tachiyomi.domain.collection.interactor

import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.repository.CollectionRepository

class CreateCollection(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(name: String, description: String? = null): Long {
        val collection = Collection.create(
            name = name,
            description = description,
        )
        return collectionRepository.insertCollection(collection)
    }
}
