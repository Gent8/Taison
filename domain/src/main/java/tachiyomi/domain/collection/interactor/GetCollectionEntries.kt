package tachiyomi.domain.collection.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.collection.model.CollectionEntryWithManga
import tachiyomi.domain.collection.repository.CollectionRepository

class GetCollectionEntries(
    private val collectionRepository: CollectionRepository,
) {

    fun subscribe(collectionId: Long): Flow<List<CollectionEntryWithManga>> {
        return collectionRepository.getEntriesWithMangaAsFlow(collectionId)
    }

    suspend fun await(collectionId: Long): List<CollectionEntryWithManga> {
        return collectionRepository.getEntriesWithManga(collectionId)
    }
}
