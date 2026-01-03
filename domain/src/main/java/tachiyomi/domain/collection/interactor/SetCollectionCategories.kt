package tachiyomi.domain.collection.interactor

import tachiyomi.domain.collection.repository.CollectionRepository

class SetCollectionCategories(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(collectionId: Long, categoryIds: List<Long>) {
        collectionRepository.setCollectionCategories(collectionId, categoryIds)
    }
}
