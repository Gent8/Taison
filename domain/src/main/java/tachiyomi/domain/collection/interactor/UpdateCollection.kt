package tachiyomi.domain.collection.interactor

import tachiyomi.domain.collection.model.CollectionUpdate
import tachiyomi.domain.collection.repository.CollectionRepository

class UpdateCollection(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(update: CollectionUpdate) {
        collectionRepository.updateCollection(update)
    }

    suspend fun awaitUpdateName(id: Long, name: String) {
        await(CollectionUpdate(id = id, name = name))
    }

    suspend fun awaitUpdateDescription(id: Long, description: String?) {
        await(CollectionUpdate(id = id, description = description))
    }

    suspend fun awaitUpdateCover(id: Long, coverMangaId: Long?) {
        await(CollectionUpdate(id = id, coverMangaId = coverMangaId))
    }
}
