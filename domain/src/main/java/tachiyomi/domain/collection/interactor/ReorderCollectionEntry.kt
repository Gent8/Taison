package tachiyomi.domain.collection.interactor

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.collection.repository.CollectionRepository

class ReorderCollectionEntry(
    private val collectionRepository: CollectionRepository,
) {
    private val mutex = Mutex()

    suspend fun await(collectionId: Long, entryId: Long, newPosition: Int) = withNonCancellableContext {
        mutex.withLock {
            val entries = collectionRepository.getEntriesWithManga(collectionId).toMutableList()

            val currentIndex = entries.indexOfFirst { it.entry.id == entryId }
            if (currentIndex == -1) {
                return@withNonCancellableContext Result.Unchanged
            }

            try {
                entries.add(newPosition, entries.removeAt(currentIndex))

                entries.forEachIndexed { index, item ->
                    collectionRepository.updateEntryPosition(item.entry.id, index.toLong())
                }

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
