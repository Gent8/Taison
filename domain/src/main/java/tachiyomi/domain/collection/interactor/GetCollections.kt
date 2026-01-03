package tachiyomi.domain.collection.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.repository.CollectionRepository

class GetCollections(
    private val collectionRepository: CollectionRepository,
) {

    fun subscribe(): Flow<List<Collection>> {
        return collectionRepository.getCollections()
    }
}
