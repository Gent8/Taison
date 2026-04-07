package tachiyomi.domain.collection.interactor

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.model.CollectionUpdate
import tachiyomi.domain.collection.repository.CollectionRepository

class ReorderCollection(
    private val collectionRepository: CollectionRepository,
) {
    private val mutex = Mutex()

    suspend fun await(collection: Collection, newIndex: Int) = withNonCancellableContext {
        mutex.withLock {
            val collections = collectionRepository.getAll().toMutableList()

            val currentIndex = collections.indexOfFirst { it.id == collection.id }
            if (currentIndex == -1) {
                return@withNonCancellableContext Result.Unchanged
            }

            try {
                collections.add(newIndex, collections.removeAt(currentIndex))

                val updates = collections.mapIndexed { index, item ->
                    CollectionUpdate(
                        id = item.id,
                        order = index.toLong(),
                    )
                }

                collectionRepository.updatePartial(updates)
                Result.Success
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                Result.InternalError(e)
            }
        }
    }

    sealed interface Result {
        data object Success : Result
        data object Unchanged : Result
        data class InternalError(val error: Throwable) : Result
    }
}
