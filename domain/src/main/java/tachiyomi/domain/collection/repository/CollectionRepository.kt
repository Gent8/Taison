package tachiyomi.domain.collection.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.model.CollectionItem
import tachiyomi.domain.collection.model.CollectionItemUpdate
import tachiyomi.domain.collection.model.CollectionUpdate
import tachiyomi.domain.manga.model.Manga

interface CollectionRepository {
    // Collection CRUD
    fun getCollections(): Flow<List<Collection>>
    fun getCollectionById(id: Long): Flow<Collection?>
    suspend fun insertCollection(collection: Collection): Long
    suspend fun updateCollection(update: CollectionUpdate)
    suspend fun deleteCollection(id: Long)

    // Collection items
    fun getItemsByCollectionId(collectionId: Long): Flow<List<CollectionItem>>
    fun getItemsWithMangaByCollectionId(collectionId: Long): Flow<List<Pair<CollectionItem, Manga>>>
    fun getCollectionsContainingManga(mangaId: Long): Flow<List<Collection>>
    suspend fun getItemCount(collectionId: Long): Long
    suspend fun insertItem(item: CollectionItem): Long
    suspend fun updateItem(update: CollectionItemUpdate)
    suspend fun deleteItem(collectionId: Long, mangaId: Long)
    suspend fun getMaxSortOrder(collectionId: Long): Int

    // Categories
    fun getCategoriesForCollection(collectionId: Long): Flow<List<Long>>
    suspend fun setCollectionCategories(collectionId: Long, categoryIds: List<Long>)
}
