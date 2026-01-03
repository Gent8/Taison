package tachiyomi.domain.collection.interactor

import tachiyomi.domain.collection.model.CollectionItem
import tachiyomi.domain.collection.repository.CollectionRepository

class AddMangaToCollection(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(collectionId: Long, mangaId: Long, badge: String? = null): Long {
        val maxOrder = collectionRepository.getMaxSortOrder(collectionId)
        val item = CollectionItem(
            collectionId = collectionId,
            mangaId = mangaId,
            sortOrder = maxOrder + 1,
            badge = badge,
        )
        return collectionRepository.insertItem(item)
    }

    suspend fun awaitBulk(collectionId: Long, mangaIds: List<Long>) {
        var order = collectionRepository.getMaxSortOrder(collectionId)
        mangaIds.forEach { mangaId ->
            order++
            val item = CollectionItem(
                collectionId = collectionId,
                mangaId = mangaId,
                sortOrder = order,
            )
            collectionRepository.insertItem(item)
        }
    }
}
