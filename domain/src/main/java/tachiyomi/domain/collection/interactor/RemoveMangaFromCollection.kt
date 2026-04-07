package tachiyomi.domain.collection.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.collection.repository.CollectionRepository

class RemoveMangaFromCollection(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(entryId: Long) = withNonCancellableContext {
        try {
            collectionRepository.deleteEntry(entryId)
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
