package tachiyomi.domain.collection.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.collection.model.CollectionUpdate
import tachiyomi.domain.collection.repository.CollectionRepository

class DeleteCollection(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(collectionId: Long) = withNonCancellableContext {
        try {
            collectionRepository.delete(collectionId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            return@withNonCancellableContext Result.InternalError(e)
        }

        val collections = collectionRepository.getAll()
        val updates = collections.mapIndexed { index, collection ->
            CollectionUpdate(
                id = collection.id,
                order = index.toLong(),
            )
        }

        try {
            collectionRepository.updatePartial(updates)
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
