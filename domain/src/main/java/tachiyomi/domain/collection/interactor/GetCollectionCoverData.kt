package tachiyomi.domain.collection.interactor

import tachiyomi.domain.collection.model.CollectionCoverData
import tachiyomi.domain.collection.repository.CollectionRepository

class GetCollectionCoverData(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(collectionId: Long): List<CollectionCoverData> {
        return collectionRepository.getTopCoverMangaForCollection(collectionId)
    }
}
