package tachiyomi.domain.collection.interactor

import tachiyomi.domain.collection.repository.CollectionRepository

class UpdateCollection(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun awaitUpdateName(collectionId: Long, name: String) {
        collectionRepository.updateCollectionPartial(
            id = collectionId,
            name = name,
        )
    }

    suspend fun awaitUpdateDescription(collectionId: Long, description: String?) {
        collectionRepository.updateCollectionPartial(
            id = collectionId,
            description = description,
        )
    }

    suspend fun awaitUpdate(collectionId: Long, name: String, description: String?) {
        collectionRepository.updateCollectionPartial(
            id = collectionId,
            name = name,
            description = description,
        )
    }
}
