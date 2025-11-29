package eu.kanade.tachiyomi.util

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun Manga.isMature(): Boolean {
    val sourceName = Injekt.get<SourceManager>().get(source)?.name

    return source in 6905L..6913L ||
        (sourceName != null && isMatureSource(sourceName)) ||
        genre.orEmpty().any { tag -> isMatureTag(tag) }
}

private fun isMatureTag(tag: String): Boolean {
    return tag.contains("hentai", ignoreCase = true) ||
        tag.contains("adult", ignoreCase = true) ||
        tag.contains("smut", ignoreCase = true) ||
        tag.contains("lewd", ignoreCase = true) ||
        tag.contains("nsfw", ignoreCase = true) ||
        tag.contains("erotica", ignoreCase = true) ||
        tag.contains("pornographic", ignoreCase = true) ||
        tag.contains("mature", ignoreCase = true) ||
        tag.contains("18+", ignoreCase = true)
}

private fun isMatureSource(source: String): Boolean {
    return source.contains("allporncomic", ignoreCase = true) ||
        source.contains("hentai cafe", ignoreCase = true) ||
        source.contains("hentai2read", ignoreCase = true) ||
        source.contains("hentaifox", ignoreCase = true) ||
        source.contains("hentainexus", ignoreCase = true) ||
        source.contains("manhwahentai.me", ignoreCase = true) ||
        source.contains("milftoon", ignoreCase = true) ||
        source.contains("myhentaicomics", ignoreCase = true) ||
        source.contains("myhentaigallery", ignoreCase = true) ||
        source.contains("ninehentai", ignoreCase = true) ||
        source.contains("pururin", ignoreCase = true) ||
        source.contains("simply hentai", ignoreCase = true) ||
        source.contains("tsumino", ignoreCase = true) ||
        source.contains("8muses", ignoreCase = true) ||
        source.contains("hbrowse", ignoreCase = true) ||
        source.contains("nhentai", ignoreCase = true) ||
        source.contains("erofus", ignoreCase = true) ||
        source.contains("luscious", ignoreCase = true) ||
        source.contains("doujins", ignoreCase = true) ||
        source.contains("multporn", ignoreCase = true) ||
        source.contains("vcp", ignoreCase = true) ||
        source.contains("vmp", ignoreCase = true) ||
        source.contains("hentai", ignoreCase = true)
}
