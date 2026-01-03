package tachiyomi.domain.collection.interactor

import tachiyomi.domain.collection.model.CollectionItemUpdate
import tachiyomi.domain.collection.repository.CollectionRepository

class ReorderCollectionItems(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(updates: List<CollectionItemUpdate>) {
        updates.forEach { update ->
            collectionRepository.updateItem(update)
        }
    }

    suspend fun awaitMoveItem(itemId: Long, newSortOrder: Int) {
        collectionRepository.updateItem(
            CollectionItemUpdate(id = itemId, sortOrder = newSortOrder),
        )
    }
}
