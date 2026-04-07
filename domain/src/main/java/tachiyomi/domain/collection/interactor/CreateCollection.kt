package tachiyomi.domain.collection.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.repository.CollectionRepository

class CreateCollection(
    private val collectionRepository: CollectionRepository,
) {

    suspend fun await(name: String): Result = withNonCancellableContext {
        val collections = collectionRepository.getAll()
        val nextOrder = collections.maxOfOrNull { it.order }?.plus(1) ?: 0
        val newCollection = Collection(
            id = 0,
            name = name,
            order = nextOrder,
            createdAt = System.currentTimeMillis(),
        )

        try {
            collectionRepository.insert(newCollection)
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
