package tachiyomi.domain.collection.interactor

import tachiyomi.domain.collection.repository.CollectionRepository

class DeleteCollection(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(id: Long) {
        collectionRepository.deleteCollection(id)
    }
}
