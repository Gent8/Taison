package tachiyomi.domain.collection.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.collection.model.CollectionWithItems
import tachiyomi.domain.collection.repository.CollectionRepository

class GetCollectionWithItems(
    private val collectionRepository: CollectionRepository,
) {

    fun subscribe(collectionId: Long): Flow<CollectionWithItems?> {
        return collectionRepository.getCollectionWithItems(collectionId)
    }
}
