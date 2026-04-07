package tachiyomi.domain.collection.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.collection.model.CollectionUpdate
import tachiyomi.domain.collection.repository.CollectionRepository

class RenameCollection(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(collectionId: Long, name: String) = withNonCancellableContext {
        val update = CollectionUpdate(
            id = collectionId,
            name = name,
        )

        try {
            collectionRepository.updatePartial(update)
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
