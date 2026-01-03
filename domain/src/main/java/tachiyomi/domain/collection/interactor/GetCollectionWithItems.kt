package tachiyomi.domain.collection.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.collection.model.CollectionItemWithManga
import tachiyomi.domain.collection.model.CollectionWithItems
import tachiyomi.domain.collection.repository.CollectionRepository

class GetCollectionWithItems(
    private val collectionRepository: CollectionRepository,
) {

    fun subscribe(collectionId: Long): Flow<CollectionWithItems?> {
        return combine(
            collectionRepository.getCollectionById(collectionId),
            collectionRepository.getItemsWithMangaByCollectionId(collectionId),
        ) { collection, itemPairs ->
            collection?.let {
                CollectionWithItems(
                    collection = it,
                    items = itemPairs.map { (item, manga) ->
                        CollectionItemWithManga(item = item, manga = manga)
                    },
                )
            }
        }
    }
}
