package eu.kanade.tachiyomi.ui.collection

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.collection.interactor.DeleteCollection
import tachiyomi.domain.collection.interactor.GetCollectionById
import tachiyomi.domain.collection.interactor.GetCollectionEntries
import tachiyomi.domain.collection.interactor.RemoveMangaFromCollection
import tachiyomi.domain.collection.interactor.RenameCollection
import tachiyomi.domain.collection.interactor.ReorderCollectionEntry
import tachiyomi.domain.collection.interactor.UpdateCollectionDescription
import tachiyomi.domain.collection.interactor.UpdateCollectionEntryLabel
import tachiyomi.domain.collection.model.Collection
import tachiyomi.domain.collection.model.CollectionEntryWithManga
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CollectionScreenModel(
    private val collectionId: Long,
    private val getCollectionById: GetCollectionById = Injekt.get(),
    private val getCollectionEntries: GetCollectionEntries = Injekt.get(),
    private val renameCollection: RenameCollection = Injekt.get(),
    private val deleteCollection: DeleteCollection = Injekt.get(),
    private val removeMangaFromCollection: RemoveMangaFromCollection = Injekt.get(),
    private val reorderCollectionEntry: ReorderCollectionEntry = Injekt.get(),
    private val updateCollectionEntryLabel: UpdateCollectionEntryLabel = Injekt.get(),
    private val updateCollectionDescription: UpdateCollectionDescription = Injekt.get(),
    private val getNextChapters: GetNextChapters = Injekt.get(),
) : StateScreenModel<CollectionScreenState>(CollectionScreenState.Loading) {

    private val _events: Channel<CollectionEvent> = Channel()
    val events = _events.receiveAsFlow()

    init {
        screenModelScope.launch {
            getCollectionEntries.subscribe(collectionId)
                .collectLatest { entries ->
                    val collection = getCollectionById.await(collectionId)
                    mutableState.update { current ->
                        if (collection == null) {
                            CollectionScreenState.Loading
                        } else {
                            val newEntries = entries.toImmutableList()
                            when (current) {
                                is CollectionScreenState.Success -> current.copy(
                                    collection = collection,
                                    entries = newEntries,
                                )
                                else -> CollectionScreenState.Success(
                                    collection = collection,
                                    entries = newEntries,
                                )
                            }
                        }
                    }
                }
        }
    }

    fun renameCollection(name: String) {
        screenModelScope.launch {
            when (renameCollection.await(collectionId, name)) {
                is RenameCollection.Result.InternalError -> _events.send(CollectionEvent.InternalError)
                else -> {}
            }
        }
    }

    fun updateDescription(description: String) {
        screenModelScope.launch {
            when (updateCollectionDescription.await(collectionId, description)) {
                is UpdateCollectionDescription.Result.InternalError -> _events.send(CollectionEvent.InternalError)
                else -> {
                    mutableState.update { current ->
                        when (current) {
                            is CollectionScreenState.Success -> current.copy(
                                collection = current.collection.copy(description = description),
                            )
                            else -> current
                        }
                    }
                }
            }
        }
    }

    fun deleteCollection() {
        screenModelScope.launch {
            when (deleteCollection.await(collectionId)) {
                is DeleteCollection.Result.InternalError -> _events.send(CollectionEvent.InternalError)
                else -> _events.send(CollectionEvent.CollectionDeleted)
            }
        }
    }

    fun removeEntry(entryId: Long) {
        screenModelScope.launch {
            when (removeMangaFromCollection.await(entryId)) {
                is RemoveMangaFromCollection.Result.InternalError -> _events.send(CollectionEvent.InternalError)
                else -> {}
            }
        }
    }

    fun changeEntryOrder(entryId: Long, newIndex: Int) {
        screenModelScope.launch {
            when (reorderCollectionEntry.await(collectionId, entryId, newIndex)) {
                is ReorderCollectionEntry.Result.InternalError -> _events.send(CollectionEvent.InternalError)
                else -> {}
            }
        }
    }

    fun updateLabel(entryId: Long, label: String) {
        screenModelScope.launch {
            when (updateCollectionEntryLabel.await(entryId, label)) {
                is UpdateCollectionEntryLabel.Result.InternalError -> _events.send(CollectionEvent.InternalError)
                else -> {}
            }
        }
    }

    fun toggleEditMode() {
        mutableState.update {
            when (it) {
                CollectionScreenState.Loading -> it
                is CollectionScreenState.Success -> it.copy(isEditMode = !it.isEditMode)
            }
        }
    }

    suspend fun getNextUnreadChapter(mangaId: Long): Chapter? {
        return getNextChapters.await(mangaId, onlyUnread = true).firstOrNull()
    }

    fun showDialog(dialog: CollectionDialog) {
        mutableState.update {
            when (it) {
                CollectionScreenState.Loading -> it
                is CollectionScreenState.Success -> it.copy(dialog = dialog)
            }
        }
    }

    fun dismissDialog() {
        mutableState.update {
            when (it) {
                CollectionScreenState.Loading -> it
                is CollectionScreenState.Success -> it.copy(dialog = null)
            }
        }
    }
}

sealed interface CollectionDialog {
    data object Rename : CollectionDialog
    data object Delete : CollectionDialog
    data class ChangeLabel(val entryId: Long, val currentLabel: String) : CollectionDialog
    data class RemoveEntry(val entryId: Long, val mangaTitle: String) : CollectionDialog
    data class EditDescription(val currentDescription: String) : CollectionDialog
}

sealed interface CollectionEvent {
    sealed class LocalizedMessage(val stringRes: StringResource) : CollectionEvent
    data object InternalError : LocalizedMessage(MR.strings.internal_error)
    data object CollectionDeleted : CollectionEvent
}

sealed interface CollectionScreenState {

    @Immutable
    data object Loading : CollectionScreenState

    @Immutable
    data class Success(
        val collection: Collection,
        val entries: ImmutableList<CollectionEntryWithManga>,
        val dialog: CollectionDialog? = null,
        val isEditMode: Boolean = false,
    ) : CollectionScreenState {

        val isEmpty: Boolean
            get() = entries.isEmpty()
    }
}
