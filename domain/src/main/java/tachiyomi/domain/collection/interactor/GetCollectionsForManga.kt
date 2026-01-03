package tachiyomi.domain.collection.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.repository.CollectionRepository

class GetCollectionsForManga(
    private val collectionRepository: CollectionRepository,
) {

    fun subscribe(mangaId: Long): Flow<List<Collection>> {
        return collectionRepository.getCollectionsContainingManga(mangaId)
    }
}
