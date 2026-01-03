package tachiyomi.data.collection

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.model.CollectionItem
import tachiyomi.domain.collection.model.CollectionItemUpdate
import tachiyomi.domain.collection.model.CollectionUpdate
import tachiyomi.domain.collection.repository.CollectionRepository
import tachiyomi.domain.manga.model.Manga

class CollectionRepositoryImpl(
    private val handler: DatabaseHandler,
) : CollectionRepository {

    override fun getCollections(): Flow<List<Collection>> {
        return handler.subscribeToList {
            collectionsQueries.selectAll(CollectionMapper::mapCollection)
        }
    }

    override fun getCollectionById(id: Long): Flow<Collection?> {
        return handler.subscribeToOneOrNull {
            collectionsQueries.selectById(id, CollectionMapper::mapCollection)
        }
    }

    override suspend fun insertCollection(collection: Collection): Long {
        return handler.await(inTransaction = true) {
            collectionsQueries.insert(
                name = collection.name,
                description = collection.description,
                coverMangaId = collection.coverMangaId,
                sortOrder = collection.sortOrder.toLong(),
                createdAt = collection.createdAt,
                updatedAt = collection.updatedAt,
            )
            collectionsQueries.selectLastInsertedRowId().executeAsOne()
        }
    }

    override suspend fun updateCollection(update: CollectionUpdate) {
        handler.await {
            collectionsQueries.update(
                id = update.id,
                name = update.name,
                description = update.description,
                coverMangaId = update.coverMangaId,
                sortOrder = update.sortOrder?.toLong(),
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun deleteCollection(id: Long) {
        handler.await {
            collectionsQueries.delete(id)
        }
    }

    override fun getItemsByCollectionId(collectionId: Long): Flow<List<CollectionItem>> {
        return handler.subscribeToList {
            collection_itemsQueries.selectByCollectionId(
                collectionId,
                CollectionMapper::mapCollectionItem,
            )
        }
    }

    override fun getItemsWithMangaByCollectionId(collectionId: Long): Flow<List<Pair<CollectionItem, Manga>>> {
        return handler.subscribeToList {
            collection_itemsQueries.selectWithMangaByCollectionId(
                collectionId,
                CollectionMapper::mapCollectionItemWithManga,
            )
        }
    }

    override fun getCollectionsContainingManga(mangaId: Long): Flow<List<Collection>> {
        return handler.subscribeToList {
            collection_itemsQueries.selectCollectionsByMangaId(
                mangaId,
                CollectionMapper::mapCollection,
            )
        }
    }

    override suspend fun getItemCount(collectionId: Long): Long {
        return handler.await {
            collection_itemsQueries.selectItemCountByCollectionId(collectionId)
                .executeAsOne()
        }
    }

    override suspend fun insertItem(item: CollectionItem): Long {
        return handler.await(inTransaction = true) {
            collection_itemsQueries.insert(
                collectionId = item.collectionId,
                mangaId = item.mangaId,
                sortOrder = item.sortOrder.toLong(),
                badge = item.badge,
            )
            collection_itemsQueries.selectLastInsertedRowId().executeAsOne()
        }
    }

    override suspend fun updateItem(update: CollectionItemUpdate) {
        handler.await {
            update.sortOrder?.let {
                collection_itemsQueries.updateSortOrder(it.toLong(), update.id)
            }
            update.badge?.let {
                collection_itemsQueries.updateBadge(it, update.id)
            }
        }
    }

    override suspend fun deleteItem(collectionId: Long, mangaId: Long) {
        handler.await {
            collection_itemsQueries.deleteByCollectionAndManga(collectionId, mangaId)
        }
    }

    override suspend fun getMaxSortOrder(collectionId: Long): Int {
        return handler.await {
            collection_itemsQueries.getMaxSortOrder(collectionId)
                .executeAsOne()
                .toInt()
        }
    }

    override fun getCategoriesForCollection(collectionId: Long): Flow<List<Long>> {
        return handler.subscribeToList {
            collections_categoriesQueries.selectByCollectionId(collectionId)
        }
    }

    override suspend fun setCollectionCategories(collectionId: Long, categoryIds: List<Long>) {
        handler.await(inTransaction = true) {
            collections_categoriesQueries.deleteByCollectionId(collectionId)
            categoryIds.forEach { categoryId ->
                collections_categoriesQueries.insert(collectionId, categoryId)
            }
        }
    }
}
