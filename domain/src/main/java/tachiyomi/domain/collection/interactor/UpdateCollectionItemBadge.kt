package tachiyomi.domain.collection.interactor

import tachiyomi.domain.collection.model.CollectionItemUpdate
import tachiyomi.domain.collection.repository.CollectionRepository

class UpdateCollectionItemBadge(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(itemId: Long, badge: String?) {
        collectionRepository.updateItem(
            CollectionItemUpdate(id = itemId, badge = badge),
        )
    }
}
