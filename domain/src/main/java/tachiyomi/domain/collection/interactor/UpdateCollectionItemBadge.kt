package tachiyomi.domain.collection.interactor

import tachiyomi.domain.collection.repository.CollectionRepository

class UpdateCollectionItemBadge(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(itemId: Long, badge: String?) {
        collectionRepository.updateCollectionItemBadge(itemId, badge)
    }
}
