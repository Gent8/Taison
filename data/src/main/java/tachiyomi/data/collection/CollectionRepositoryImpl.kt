package tachiyomi.data.collection

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.model.CollectionCoverData
import tachiyomi.domain.collection.model.CollectionEntry
import tachiyomi.domain.collection.model.CollectionEntryWithManga
import tachiyomi.domain.collection.model.CollectionUpdate
import tachiyomi.domain.collection.model.CollectionWithEntryCount
import tachiyomi.domain.collection.model.CollectionWithLabel
import tachiyomi.domain.collection.repository.CollectionRepository
import tachiyomi.domain.manga.model.Manga

class CollectionRepositoryImpl(
    private val handler: DatabaseHandler,
) : CollectionRepository {

    override suspend fun getAll(): List<Collection> {
        return handler.awaitList { collectionsQueries.getCollections(::mapCollection) }
    }

    override fun getAllAsFlow(): Flow<List<Collection>> {
        return handler.subscribeToList { collectionsQueries.getCollections(::mapCollection) }
    }

    override suspend fun getAllWithEntryCount(): List<CollectionWithEntryCount> {
        return handler.awaitList {
            collectionsQueries.getCollectionsWithEntryCount(::mapCollectionWithEntryCount)
        }
    }

    override fun getAllWithEntryCountAsFlow(): Flow<List<CollectionWithEntryCount>> {
        return handler.subscribeToList {
            collectionsQueries.getCollectionsWithEntryCount(::mapCollectionWithEntryCount)
        }
    }

    override fun getAllCollectionCoversAsFlow(): Flow<Map<Long, List<CollectionCoverData>>> {
        return handler.subscribeToList {
            collectionsQueries.getAllCollectionCovers(::mapAllCollectionCoverRow)
        }.map { rows ->
            rows.groupBy { it.first }
                .mapValues { (_, list) -> list.map { it.second }.take(3) }
        }
    }

    override suspend fun getById(id: Long): Collection? {
        return handler.awaitOneOrNull { collectionsQueries.getCollectionById(id, ::mapCollection) }
    }

    override suspend fun getCollectionsByMangaId(mangaId: Long): List<Collection> {
        return handler.awaitList {
            collectionsQueries.getCollectionsByMangaId(mangaId, ::mapCollection)
        }
    }

    override fun getCollectionsByMangaIdAsFlow(mangaId: Long): Flow<List<Collection>> {
        return handler.subscribeToList {
            collectionsQueries.getCollectionsByMangaId(mangaId, ::mapCollection)
        }
    }

    override fun getCollectionsWithLabelByMangaIdAsFlow(mangaId: Long): Flow<List<CollectionWithLabel>> {
        return handler.subscribeToList {
            collectionsQueries.getCollectionsByMangaIdWithLabel(mangaId, ::mapCollectionWithLabel)
        }
    }

    override suspend fun getTopCoverMangaForCollection(collectionId: Long): List<CollectionCoverData> {
        return handler.awaitList {
            collectionsQueries.getTopCoverMangaForCollection(collectionId, ::mapCoverData)
        }
    }

    override suspend fun getEntriesWithManga(collectionId: Long): List<CollectionEntryWithManga> {
        return handler.awaitList {
            collectionsQueries.getCollectionEntriesWithManga(collectionId, ::mapCollectionEntryWithManga)
        }
    }

    override fun getEntriesWithMangaAsFlow(collectionId: Long): Flow<List<CollectionEntryWithManga>> {
        return handler.subscribeToList {
            collectionsQueries.getCollectionEntriesWithManga(collectionId, ::mapCollectionEntryWithManga)
        }
    }

    override suspend fun getFirstMangaForCollection(collectionId: Long): Manga? {
        return handler.awaitOneOrNull {
            collectionsQueries.getFirstMangaForCollection(collectionId, ::mapMangaFromCollection)
        }
    }

    override suspend fun getEntryCountForCollection(collectionId: Long): Long {
        return handler.awaitOne { collectionsQueries.getEntryCountForCollection(collectionId) }
    }

    override suspend fun insert(collection: Collection): Long {
        return handler.awaitOneExecutable {
            collectionsQueries.insertCollection(
                name = collection.name,
                description = collection.description,
                order = collection.order,
                createdAt = collection.createdAt,
                categoryId = collection.categoryId,
            )
            collectionsQueries.selectLastInsertedRowId()
        }
    }

    override suspend fun updatePartial(update: CollectionUpdate) {
        handler.await {
            updatePartialBlocking(update)
        }
    }

    override suspend fun updatePartial(updates: List<CollectionUpdate>) {
        handler.await(inTransaction = true) {
            for (update in updates) {
                updatePartialBlocking(update)
            }
        }
    }

    private fun tachiyomi.data.Database.updatePartialBlocking(update: CollectionUpdate) {
        collectionsQueries.updateCollection(
            name = update.name,
            description = update.description,
            order = update.order,
            categoryId = update.categoryId,
            collectionId = update.id,
        )
    }

    override suspend fun delete(collectionId: Long) {
        handler.await {
            collectionsQueries.deleteCollection(collectionId = collectionId)
        }
    }

    override suspend fun insertEntry(collectionId: Long, mangaId: Long, position: Long, label: String) {
        handler.await {
            collectionsQueries.insertEntry(
                collectionId = collectionId,
                mangaId = mangaId,
                position = position,
                label = label,
            )
        }
    }

    override suspend fun deleteEntry(entryId: Long) {
        handler.await {
            collectionsQueries.deleteEntry(entryId = entryId)
        }
    }

    override suspend fun updateEntryPosition(entryId: Long, position: Long) {
        handler.await {
            collectionsQueries.updateEntryPosition(position = position, entryId = entryId)
        }
    }

    override suspend fun updateEntryLabel(entryId: Long, label: String) {
        handler.await {
            collectionsQueries.updateEntryLabel(label = label, entryId = entryId)
        }
    }

    override suspend fun getMaxEntryPosition(collectionId: Long): Long? {
        return handler.awaitOneOrNull {
            collectionsQueries.getMaxEntryPosition(collectionId)
        }?.maxPosition
    }

    private fun mapCollection(
        id: Long,
        name: String,
        description: String,
        order: Long,
        createdAt: Long,
        categoryId: Long?,
    ): Collection {
        return Collection(
            id = id,
            name = name,
            description = description,
            order = order,
            createdAt = createdAt,
            categoryId = categoryId,
        )
    }

    private fun mapCollectionWithLabel(
        id: Long,
        name: String,
        description: String,
        order: Long,
        createdAt: Long,
        categoryId: Long?,
        label: String,
    ): CollectionWithLabel {
        return CollectionWithLabel(
            collection = Collection(
                id = id,
                name = name,
                description = description,
                order = order,
                createdAt = createdAt,
                categoryId = categoryId,
            ),
            label = label,
        )
    }

    private fun mapCoverData(
        mangaId: Long,
        sourceId: Long,
        thumbnailUrl: String?,
        coverLastModified: Long,
        isFavorite: Boolean,
    ): CollectionCoverData {
        return CollectionCoverData(
            mangaId = mangaId,
            sourceId = sourceId,
            thumbnailUrl = thumbnailUrl,
            coverLastModified = coverLastModified,
            isFavorite = isFavorite,
        )
    }

    private fun mapCollectionEntryWithManga(
        entryId: Long,
        collectionId: Long,
        mangaId: Long,
        position: Long,
        label: String,
        mId: Long,
        source: Long,
        url: String,
        artist: String?,
        author: String?,
        description: String?,
        genre: List<String>?,
        title: String,
        status: Long,
        thumbnailUrl: String?,
        favorite: Boolean,
        lastUpdate: Long?,
        nextUpdate: Long?,
        initialized: Boolean,
        viewerFlags: Long,
        chapterFlags: Long,
        coverLastModified: Long,
        dateAdded: Long,
        updateStrategy: UpdateStrategy,
        fetchInterval: Long,
        lastModifiedAt: Long,
        favoriteModifiedAt: Long?,
        version: Long,
        notes: String,
    ): CollectionEntryWithManga {
        return CollectionEntryWithManga(
            entry = CollectionEntry(
                id = entryId,
                collectionId = collectionId,
                mangaId = mangaId,
                position = position,
                label = label,
            ),
            manga = buildMangaFromColumns(
                mId = mId,
                source = source,
                url = url,
                artist = artist,
                author = author,
                description = description,
                genre = genre,
                title = title,
                status = status,
                thumbnailUrl = thumbnailUrl,
                favorite = favorite,
                lastUpdate = lastUpdate,
                nextUpdate = nextUpdate,
                initialized = initialized,
                viewerFlags = viewerFlags,
                chapterFlags = chapterFlags,
                coverLastModified = coverLastModified,
                dateAdded = dateAdded,
                updateStrategy = updateStrategy,
                fetchInterval = fetchInterval,
                lastModifiedAt = lastModifiedAt,
                favoriteModifiedAt = favoriteModifiedAt,
                version = version,
                notes = notes,
            ),
        )
    }

    private fun mapMangaFromCollection(
        mId: Long,
        source: Long,
        url: String,
        artist: String?,
        author: String?,
        description: String?,
        genre: List<String>?,
        title: String,
        status: Long,
        thumbnailUrl: String?,
        favorite: Boolean,
        lastUpdate: Long?,
        nextUpdate: Long?,
        initialized: Boolean,
        viewerFlags: Long,
        chapterFlags: Long,
        coverLastModified: Long,
        dateAdded: Long,
        updateStrategy: UpdateStrategy,
        fetchInterval: Long,
        lastModifiedAt: Long,
        favoriteModifiedAt: Long?,
        version: Long,
        notes: String,
    ): Manga {
        return buildMangaFromColumns(
            mId = mId,
            source = source,
            url = url,
            artist = artist,
            author = author,
            description = description,
            genre = genre,
            title = title,
            status = status,
            thumbnailUrl = thumbnailUrl,
            favorite = favorite,
            lastUpdate = lastUpdate,
            nextUpdate = nextUpdate,
            initialized = initialized,
            viewerFlags = viewerFlags,
            chapterFlags = chapterFlags,
            coverLastModified = coverLastModified,
            dateAdded = dateAdded,
            updateStrategy = updateStrategy,
            fetchInterval = fetchInterval,
            lastModifiedAt = lastModifiedAt,
            favoriteModifiedAt = favoriteModifiedAt,
            version = version,
            notes = notes,
        )
    }

    private fun buildMangaFromColumns(
        mId: Long,
        source: Long,
        url: String,
        artist: String?,
        author: String?,
        description: String?,
        genre: List<String>?,
        title: String,
        status: Long,
        thumbnailUrl: String?,
        favorite: Boolean,
        lastUpdate: Long?,
        nextUpdate: Long?,
        initialized: Boolean,
        viewerFlags: Long,
        chapterFlags: Long,
        coverLastModified: Long,
        dateAdded: Long,
        updateStrategy: UpdateStrategy,
        fetchInterval: Long,
        lastModifiedAt: Long,
        favoriteModifiedAt: Long?,
        version: Long,
        notes: String,
    ): Manga {
        return Manga(
            id = mId,
            source = source,
            favorite = favorite,
            lastUpdate = lastUpdate ?: 0,
            nextUpdate = nextUpdate ?: 0,
            fetchInterval = fetchInterval.toInt(),
            dateAdded = dateAdded,
            viewerFlags = viewerFlags,
            chapterFlags = chapterFlags,
            coverLastModified = coverLastModified,
            url = url,
            title = title,
            artist = artist,
            author = author,
            description = description,
            genre = genre,
            status = status,
            thumbnailUrl = thumbnailUrl,
            updateStrategy = updateStrategy,
            initialized = initialized,
            lastModifiedAt = lastModifiedAt,
            favoriteModifiedAt = favoriteModifiedAt,
            version = version,
            notes = notes,
        )
    }

    private fun mapCollectionWithEntryCount(
        id: Long,
        name: String,
        description: String,
        order: Long,
        createdAt: Long,
        categoryId: Long?,
        entryCount: Long,
    ): CollectionWithEntryCount {
        return CollectionWithEntryCount(
            collection = Collection(
                id = id,
                name = name,
                description = description,
                order = order,
                createdAt = createdAt,
                categoryId = categoryId,
            ),
            entryCount = entryCount,
        )
    }

    private fun mapAllCollectionCoverRow(
        collectionId: Long,
        mangaId: Long,
        sourceId: Long,
        thumbnailUrl: String?,
        coverLastModified: Long,
        isFavorite: Boolean,
        position: Long,
    ): Pair<Long, CollectionCoverData> {
        // position is included by SQL ordering; we don't need to keep it.
        @Suppress("UNUSED_VARIABLE")
        val pos = position
        return collectionId to CollectionCoverData(
            mangaId = mangaId,
            sourceId = sourceId,
            thumbnailUrl = thumbnailUrl,
            coverLastModified = coverLastModified,
            isFavorite = isFavorite,
        )
    }
}
