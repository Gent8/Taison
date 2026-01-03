package tachiyomi.domain.collection.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.repository.CollectionRepository

class GetCollection(
    private val collectionRepository: CollectionRepository,
) {

    fun subscribe(id: Long): Flow<Collection?> {
        return collectionRepository.getCollectionById(id)
    }
}
