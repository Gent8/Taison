package eu.kanade.tachiyomi.ui.deeplink

import android.net.Uri
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.chapter.interactor.SyncChaptersWithSource
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.ResolvableSource
import eu.kanade.tachiyomi.source.online.UriType
import kotlinx.coroutines.flow.update
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.chapter.interactor.GetChapterByUrlAndMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DeepLinkScreenModel(
    query: String = "",
    private val sourceManager: SourceManager = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val getChapterByUrlAndMangaId: GetChapterByUrlAndMangaId = Injekt.get(),
    private val syncChaptersWithSource: SyncChaptersWithSource = Injekt.get(),
) : StateScreenModel<DeepLinkScreenModel.State>(State.Loading) {

    init {
        screenModelScope.launchIO {
            val parsedEntryLink = runCatching { TaisonEntryLink.parse(Uri.parse(query)) }
                .getOrNull()

            if (parsedEntryLink != null) {
                resolveTaisonEntry(parsedEntryLink)
            } else {
                resolveByUrl(query)
            }
        }
    }

    private suspend fun resolveTaisonEntry(link: TaisonEntryLink) {
        val source = sourceManager.get(link.sourceId)
        if (source !is HttpSource) {
            mutableState.update {
                State.SourceNotInstalled(
                    title = link.title,
                    sourceName = link.sourceName,
                    sourceId = link.sourceId,
                )
            }
            return
        }

        // The wire format carries the full https URL in `u`; HttpSource.getMangaDetails wants the
        // source-relative path stored in SManga.url. Strip the known baseUrl prefix to recover it,
        // falling back to the raw value if the URL doesn't sit under the source's base.
        val mangaPath = source.baseUrl
            .takeIf { link.sourceUrl.startsWith(it) }
            ?.let { link.sourceUrl.removePrefix(it) }
            ?: link.sourceUrl

        val seed = SManga.create().apply {
            url = mangaPath
            title = link.title
            link.author?.let { author = it }
            link.genres?.let { genre = it }
            link.status?.let { status = it }
        }
        // Source extensions' getMangaDetails typically only populates descriptive fields
        // (description, thumbnail, author, status, ...) and leaves the lateinit `url`/`title`
        // uninitialized in the returned SManga. Treat the fetched result as a delta and merge it
        // onto our seed so the identifiers survive.
        runCatching { source.getMangaDetails(seed) }.onSuccess { fetched ->
            if (fetched !== seed) seed.mergeDescriptiveFieldsFrom(fetched)
        }
        seed.initialized = true
        val manga = networkToLocalManga(seed.toDomainManga(source.id))

        val chapter = if (link.chapterUrl != null) {
            val chapterPath = source.baseUrl
                .takeIf { link.chapterUrl.startsWith(it) }
                ?.let { link.chapterUrl.removePrefix(it) }
                ?: link.chapterUrl
            resolveChapter(source, manga, chapterPath)
        } else {
            null
        }

        mutableState.update {
            if (chapter == null) State.Result(manga) else State.Result(manga, chapter.id)
        }
    }

    private suspend fun resolveByUrl(query: String) {
        val source = sourceManager.getCatalogueSources()
            .filterIsInstance<ResolvableSource>()
            .firstOrNull { it.getUriType(query) != UriType.Unknown }

        val manga = source?.getManga(query)?.let {
            networkToLocalManga(it.toDomainManga(source.id))
        }

        val chapter = if (source?.getUriType(query) == UriType.Chapter && manga != null) {
            source.getChapter(query)?.let { getChapterFromSChapter(it, manga, source) }
        } else {
            null
        }

        mutableState.update {
            if (manga == null) {
                State.NoResults
            } else {
                if (chapter == null) {
                    State.Result(manga)
                } else {
                    State.Result(manga, chapter.id)
                }
            }
        }
    }

    private suspend fun resolveChapter(source: HttpSource, manga: Manga, chapterUrl: String): Chapter? {
        val local = getChapterByUrlAndMangaId.await(chapterUrl, manga.id)
        if (local != null) return local
        val sourceChapters = source.getChapterList(manga.toSManga())
        val newChapters = syncChaptersWithSource.await(sourceChapters, manga, source, false)
        return newChapters.find { it.url == chapterUrl }
    }

    private suspend fun getChapterFromSChapter(sChapter: SChapter, manga: Manga, source: Source): Chapter? {
        val localChapter = getChapterByUrlAndMangaId.await(sChapter.url, manga.id)

        return if (localChapter == null) {
            val sourceChapters = source.getChapterList(manga.toSManga())
            val newChapters = syncChaptersWithSource.await(sourceChapters, manga, source, false)
            newChapters.find { it.url == sChapter.url }
        } else {
            localChapter
        }
    }

    /**
     * Copy only descriptive fields from [other] onto the receiver. The lateinit identifiers
     * (`url`, `title`) on [other] may not be initialized when extensions return a partially-filled
     * details object, so we never touch them. Property access is wrapped because the underlying
     * SMangaImpl uses lateinit and accessing an unset property throws.
     */
    private fun SManga.mergeDescriptiveFieldsFrom(other: SManga) {
        runCatching { other.artist }.getOrNull()?.let { artist = it }
        runCatching { other.author }.getOrNull()?.let { author = it }
        runCatching { other.description }.getOrNull()?.let { description = it }
        runCatching { other.genre }.getOrNull()?.let { genre = it }
        runCatching { other.thumbnail_url }.getOrNull()?.let { thumbnail_url = it }
        runCatching { other.status }.getOrNull()?.takeIf { it != 0 }?.let { status = it }
        runCatching { other.update_strategy }.getOrNull()?.let { update_strategy = it }
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data object NoResults : State

        @Immutable
        data class SourceNotInstalled(
            val title: String,
            val sourceName: String?,
            val sourceId: Long,
        ) : State

        @Immutable
        data class Result(val manga: Manga, val chapterId: Long? = null) : State
    }
}
