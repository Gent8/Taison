package tachiyomi.domain.collection.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.collection.repository.CollectionRepository

class GetCollectionCategories(
    private val collectionRepository: CollectionRepository,
) {

    fun subscribe(collectionId: Long): Flow<List<Long>> {
        return collectionRepository.getCategoriesForCollection(collectionId)
    }
}
