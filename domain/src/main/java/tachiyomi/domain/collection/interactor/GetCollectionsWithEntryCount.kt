package tachiyomi.domain.collection.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.collection.model.CollectionWithEntryCount
import tachiyomi.domain.collection.repository.CollectionRepository

class GetCollectionsWithEntryCount(
    private val collectionRepository: CollectionRepository,
) {

    fun subscribe(): Flow<List<CollectionWithEntryCount>> {
        return collectionRepository.getAllWithEntryCountAsFlow()
    }

    suspend fun await(): List<CollectionWithEntryCount> {
        return collectionRepository.getAllWithEntryCount()
    }
}
