package tachiyomi.domain.collection.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.model.CollectionItem
import tachiyomi.domain.collection.model.CollectionWithItems

interface CollectionRepository {

    fun getCollections(): Flow<List<Collection>>

    fun getCollection(id: Long): Flow<Collection?>

    fun getCollectionWithItems(id: Long): Flow<CollectionWithItems?>

    fun getCollectionsForManga(mangaId: Long): Flow<List<Collection>>

    suspend fun insertCollection(collection: Collection): Long

    suspend fun updateCollection(collection: Collection)

    suspend fun updateCollectionPartial(
        id: Long,
        name: String? = null,
        description: String? = null,
    )

    suspend fun deleteCollection(id: Long)

    suspend fun insertCollectionItem(item: CollectionItem): Long

    suspend fun updateCollectionItemBadge(itemId: Long, badge: String?)

    suspend fun removeCollectionItem(collectionId: Long, mangaId: Long)

    suspend fun reorderCollectionItems(collectionId: Long, items: List<Long>)
}
