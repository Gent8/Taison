package eu.kanade.tachiyomi.ui.deeplink

import android.net.Uri
import android.util.Base64

/**
 * Taison-native deep link for a manga entry. Carries enough information for the recipient device
 * to render a faithful preview without fetching anything from the network, and to resolve the
 * entry inside Taison without needing the source to implement
 * [eu.kanade.tachiyomi.source.online.ResolvableSource].
 *
 * Two equivalent wire formats are accepted on the receive side:
 * - App Link (preferred for sharing): `https://taison.gent8.com/e/?<params>`
 * - Custom scheme (no-infra fallback): `taison://entry?<params>`
 *
 * Encoding rules:
 * - `s`, `v`, `st` are decimal integers.
 * - `n`, `l`, `t`, `a`, `g` are URL-encoded UTF-8 (no Base64).
 * - `u`, `cu`, `c`, `d` are URL-safe Base64 (no padding) of the UTF-8 bytes.
 *
 * Hard caps applied by the sender to keep the URL chat-safe (~2 KB total):
 * - `d` (description) is truncated to [DESCRIPTION_MAX] characters.
 * - `g` (genres) is the first [GENRES_MAX] genres joined by `, `.
 * - `a` (author) is truncated to [AUTHOR_MAX] characters.
 */
data class TaisonEntryLink(
    val sourceId: Long,
    val sourceUrl: String,
    val title: String,
    val sourceName: String? = null,
    val sourceLang: String? = null,
    val versionId: Int? = null,
    val chapterUrl: String? = null,
    val author: String? = null,
    val genres: String? = null,
    val status: Int? = null,
) {
    fun toAppLinkUri(): Uri = Uri.Builder()
        .scheme(APP_LINK_SCHEME)
        .authority(APP_LINK_HOST)
        .appendEncodedPath(APP_LINK_PATH)
        .appendCommonParams()
        .build()

    fun toCustomSchemeUri(): Uri = Uri.Builder()
        .scheme(CUSTOM_SCHEME)
        .authority(CUSTOM_HOST)
        .appendCommonParams()
        .build()

    private fun Uri.Builder.appendCommonParams(): Uri.Builder = apply {
        appendQueryParameter(PARAM_SOURCE_ID, sourceId.toString())
        sourceName?.let { appendQueryParameter(PARAM_SOURCE_NAME, it) }
        sourceLang?.let { appendQueryParameter(PARAM_SOURCE_LANG, it) }
        versionId?.let { appendQueryParameter(PARAM_VERSION_ID, it.toString()) }
        appendQueryParameter(PARAM_TITLE, title)
        appendQueryParameter(PARAM_URL, encodeB64(sourceUrl))
        chapterUrl?.let { appendQueryParameter(PARAM_CHAPTER_URL, encodeB64(it)) }
        author?.let { appendQueryParameter(PARAM_AUTHOR, truncate(it, AUTHOR_MAX)) }
        genres?.let { appendQueryParameter(PARAM_GENRES, joinedGenres(it)) }
        status?.takeIf { it != 0 }?.let { appendQueryParameter(PARAM_STATUS, it.toString()) }
    }

    companion object {
        const val CUSTOM_SCHEME = "taison"
        const val CUSTOM_HOST = "entry"

        const val APP_LINK_SCHEME = "https"
        const val APP_LINK_HOST = "taison.gent8.com"
        const val APP_LINK_PATH = "e/"

        private const val PARAM_SOURCE_ID = "s"
        private const val PARAM_SOURCE_NAME = "n"
        private const val PARAM_SOURCE_LANG = "l"
        private const val PARAM_VERSION_ID = "v"
        private const val PARAM_TITLE = "t"
        private const val PARAM_URL = "u"
        private const val PARAM_CHAPTER_URL = "cu"
        private const val PARAM_AUTHOR = "a"
        private const val PARAM_GENRES = "g"
        private const val PARAM_STATUS = "st"

        private const val AUTHOR_MAX = 80
        private const val GENRES_MAX = 5

        private const val B64_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

        fun matches(uri: Uri): Boolean = isCustomScheme(uri) || isAppLink(uri)

        private fun isCustomScheme(uri: Uri): Boolean =
            uri.scheme.equals(CUSTOM_SCHEME, ignoreCase = true) &&
                uri.host.equals(CUSTOM_HOST, ignoreCase = true)

        private fun isAppLink(uri: Uri): Boolean =
            uri.scheme.equals(APP_LINK_SCHEME, ignoreCase = true) &&
                uri.host.equals(APP_LINK_HOST, ignoreCase = true) &&
                (uri.path?.trimStart('/')?.startsWith("e") == true)

        fun parse(uri: Uri): TaisonEntryLink? {
            if (!matches(uri)) return null
            val sid = uri.getQueryParameter(PARAM_SOURCE_ID)?.toLongOrNull() ?: return null
            val title = uri.getQueryParameter(PARAM_TITLE) ?: return null
            val url = uri.getQueryParameter(PARAM_URL)?.let(::decodeB64) ?: return null
            return TaisonEntryLink(
                sourceId = sid,
                sourceUrl = url,
                title = title,
                sourceName = uri.getQueryParameter(PARAM_SOURCE_NAME),
                sourceLang = uri.getQueryParameter(PARAM_SOURCE_LANG),
                versionId = uri.getQueryParameter(PARAM_VERSION_ID)?.toIntOrNull(),
                chapterUrl = uri.getQueryParameter(PARAM_CHAPTER_URL)?.let(::decodeB64),
                author = uri.getQueryParameter(PARAM_AUTHOR),
                genres = uri.getQueryParameter(PARAM_GENRES),
                status = uri.getQueryParameter(PARAM_STATUS)?.toIntOrNull(),
            )
        }

        private fun encodeB64(value: String): String =
            Base64.encodeToString(value.toByteArray(Charsets.UTF_8), B64_FLAGS)

        private fun decodeB64(value: String): String? = try {
            String(Base64.decode(value, B64_FLAGS), Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            null
        }

        private fun truncate(value: String, max: Int): String =
            if (value.length <= max) value else value.substring(0, max).trimEnd() + "…"

        private fun joinedGenres(raw: String): String {
            val parts = raw.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(GENRES_MAX)
            return parts.joinToString(", ")
        }
    }
}
