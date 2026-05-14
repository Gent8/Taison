package tachiyomi.domain.collection.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.model.CollectionCoverData
import tachiyomi.domain.collection.model.CollectionEntryWithManga
import tachiyomi.domain.collection.model.CollectionUpdate
import tachiyomi.domain.collection.model.CollectionWithEntryCount
import tachiyomi.domain.collection.model.CollectionWithLabel
import tachiyomi.domain.manga.model.Manga

interface CollectionRepository {

    suspend fun getAll(): List<Collection>

    fun getAllAsFlow(): Flow<List<Collection>>

    suspend fun getAllWithEntryCount(): List<CollectionWithEntryCount>

    fun getAllWithEntryCountAsFlow(): Flow<List<CollectionWithEntryCount>>

    fun getAllCollectionCoversAsFlow(): Flow<Map<Long, List<CollectionCoverData>>>

    suspend fun getById(id: Long): Collection?

    suspend fun getCollectionsByMangaId(mangaId: Long): List<Collection>

    fun getCollectionsByMangaIdAsFlow(mangaId: Long): Flow<List<Collection>>

    fun getCollectionsWithLabelByMangaIdAsFlow(mangaId: Long): Flow<List<CollectionWithLabel>>

    suspend fun getTopCoverMangaForCollection(collectionId: Long): List<CollectionCoverData>

    suspend fun getEntriesWithManga(collectionId: Long): List<CollectionEntryWithManga>

    fun getEntriesWithMangaAsFlow(collectionId: Long): Flow<List<CollectionEntryWithManga>>

    suspend fun getFirstMangaForCollection(collectionId: Long): Manga?

    suspend fun getEntryCountForCollection(collectionId: Long): Long

    suspend fun insert(collection: Collection): Long

    suspend fun updatePartial(update: CollectionUpdate)

    suspend fun updatePartial(updates: List<CollectionUpdate>)

    suspend fun delete(collectionId: Long)

    suspend fun insertEntry(collectionId: Long, mangaId: Long, position: Long, label: String)

    suspend fun deleteEntry(entryId: Long)

    suspend fun updateEntryPosition(entryId: Long, position: Long)

    suspend fun updateEntryLabel(entryId: Long, label: String)

    suspend fun getMaxEntryPosition(collectionId: Long): Long?
}
