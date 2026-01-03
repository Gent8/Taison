package tachiyomi.domain.collection.interactor

import tachiyomi.domain.collection.model.CollectionItem
import tachiyomi.domain.collection.repository.CollectionRepository

class AddMangaToCollection(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(collectionId: Long, mangaId: Long, badge: String? = null): Long {
        val item = CollectionItem(
            collectionId = collectionId,
            mangaId = mangaId,
            badge = badge,
        )
        return collectionRepository.insertCollectionItem(item)
    }
}
