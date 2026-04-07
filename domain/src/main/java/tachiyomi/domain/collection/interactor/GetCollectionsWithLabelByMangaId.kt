package tachiyomi.domain.collection.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.collection.model.CollectionWithLabel
import tachiyomi.domain.collection.repository.CollectionRepository

class GetCollectionsWithLabelByMangaId(
    private val collectionRepository: CollectionRepository,
) {

    fun subscribe(mangaId: Long): Flow<List<CollectionWithLabel>> {
        return collectionRepository.getCollectionsWithLabelByMangaIdAsFlow(mangaId)
    }
}
