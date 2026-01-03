package tachiyomi.data.collection

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.model.CollectionItem
import tachiyomi.domain.manga.model.Manga

object CollectionMapper {

    fun mapCollection(
        id: Long,
        name: String,
        description: String?,
        coverMangaId: Long?,
        sortOrder: Long,
        createdAt: Long,
        updatedAt: Long,
    ): Collection = Collection(
        id = id,
        name = name,
        description = description,
        coverMangaId = coverMangaId,
        sortOrder = sortOrder.toInt(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun mapCollectionItem(
        id: Long,
        collectionId: Long,
        mangaId: Long,
        sortOrder: Long,
        badge: String?,
    ): CollectionItem = CollectionItem(
        id = id,
        collectionId = collectionId,
        mangaId = mangaId,
        sortOrder = sortOrder.toInt(),
        badge = badge,
    )

    fun mapCollectionItemWithManga(
        // Collection item columns
        itemId: Long,
        collectionId: Long,
        mangaId: Long,
        sortOrder: Long,
        badge: String?,
        // Manga columns - match mangas table
        mangaDbId: Long,
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
        calculateInterval: Long,
        lastModifiedAt: Long,
        favoriteModifiedAt: Long?,
        version: Long,
        @Suppress("UNUSED_PARAMETER")
        isSyncing: Long,
        notes: String,
    ): Pair<CollectionItem, Manga> {
        val item = CollectionItem(
            id = itemId,
            collectionId = collectionId,
            mangaId = mangaId,
            sortOrder = sortOrder.toInt(),
            badge = badge,
        )
        val manga = Manga(
            id = mangaDbId,
            source = source,
            favorite = favorite,
            lastUpdate = lastUpdate ?: 0,
            nextUpdate = nextUpdate ?: 0,
            fetchInterval = calculateInterval.toInt(),
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
        return item to manga
    }
}
