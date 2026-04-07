package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.getNameForMangaInfo
import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.model.CollectionCoverData
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private const val LOCAL_SOURCE_ID_ALIAS = "local"

sealed interface LibraryGridItem {
    val id: Long
    fun matches(constraint: String): Boolean
}

data class LibraryCollectionItem(
    val collection: Collection,
    val coverData: List<CollectionCoverData>,
    val entryCount: Long,
) : LibraryGridItem {
    override val id: Long = -collection.id
    override fun matches(constraint: String): Boolean {
        return collection.name.contains(constraint, true) ||
            collection.description.contains(constraint, true)
    }
}

data class LibraryItem(
    val libraryManga: LibraryManga,
    val downloadCount: Long = -1,
    val unreadCount: Long = -1,
    val isLocal: Boolean = false,
    val sourceLanguage: String = "",
    private val sourceManager: SourceManager = Injekt.get(),
) : LibraryGridItem {
    override val id: Long = libraryManga.id

    override fun matches(constraint: String): Boolean {
        val source = sourceManager.getOrStub(libraryManga.manga.source)
        val sourceName by lazy { source.getNameForMangaInfo() }
        if (constraint.startsWith("id:", true)) {
            return id == constraint.substringAfter("id:").toLongOrNull()
        } else if (constraint.startsWith("src:", true)) {
            val querySource = constraint.substringAfter("src:")
            return if (querySource.equals(LOCAL_SOURCE_ID_ALIAS, ignoreCase = true)) {
                source.id == LocalSource.ID
            } else {
                source.id == querySource.toLongOrNull()
            }
        }
        return libraryManga.manga.title.contains(constraint, true) ||
            (libraryManga.manga.author?.contains(constraint, true) ?: false) ||
            (libraryManga.manga.artist?.contains(constraint, true) ?: false) ||
            (libraryManga.manga.description?.contains(constraint, true) ?: false) ||
            constraint.split(",").map { it.trim() }.all { subconstraint ->
                checkNegatableConstraint(subconstraint) {
                    sourceName.contains(it, true) ||
                        (libraryManga.manga.genre?.any { genre -> genre.equals(it, true) } ?: false)
                }
            }
    }

    /**
     * Checks a predicate on a negatable constraint. If the constraint starts with a minus character,
     * the minus is stripped and the result of the predicate is inverted.
     *
     * @param constraint the argument to the predicate. Inverts the predicate if it starts with '-'.
     * @param predicate the check to be run against the constraint.
     * @return !predicate(x) if constraint = "-x", otherwise predicate(constraint)
     */
    private fun checkNegatableConstraint(
        constraint: String,
        predicate: (String) -> Boolean,
    ): Boolean {
        return if (constraint.startsWith("-")) {
            !predicate(constraint.substringAfter("-").trimStart())
        } else {
            predicate(constraint)
        }
    }
}
