package tachiyomi.domain.collection.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.collection.model.CollectionCoverData
import tachiyomi.domain.collection.repository.CollectionRepository

class GetCollectionCoverData(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(collectionId: Long): List<CollectionCoverData> {
        return collectionRepository.getTopCoverMangaForCollection(collectionId)
    }

    fun subscribeAll(): Flow<Map<Long, List<CollectionCoverData>>> {
        return collectionRepository.getAllCollectionCoversAsFlow()
    }
}
