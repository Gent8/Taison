package tachiyomi.domain.collection.interactor

import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.repository.CollectionRepository

class GetCollectionById(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(id: Long): Collection? {
        return collectionRepository.getById(id)
    }
}
