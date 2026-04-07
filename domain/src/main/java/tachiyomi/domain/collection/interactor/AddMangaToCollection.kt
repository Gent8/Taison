package tachiyomi.domain.collection.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.collection.repository.CollectionRepository

class AddMangaToCollection(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(collectionId: Long, mangaId: Long, label: String = "") = withNonCancellableContext {
        try {
            val maxPosition = collectionRepository.getMaxEntryPosition(collectionId) ?: -1
            collectionRepository.insertEntry(
                collectionId = collectionId,
                mangaId = mangaId,
                position = maxPosition + 1,
                label = label,
            )
            Result.Success
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.InternalError(e)
        }
    }

    sealed interface Result {
        data object Success : Result
        data class InternalError(val error: Throwable) : Result
    }
}
