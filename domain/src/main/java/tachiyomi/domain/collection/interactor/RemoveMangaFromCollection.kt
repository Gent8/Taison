package tachiyomi.domain.collection.interactor

import tachiyomi.domain.collection.repository.CollectionRepository

class RemoveMangaFromCollection(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(collectionId: Long, mangaId: Long) {
        collectionRepository.deleteItem(collectionId, mangaId)
    }
}
