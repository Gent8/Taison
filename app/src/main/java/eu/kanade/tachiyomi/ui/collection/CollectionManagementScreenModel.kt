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
import tachiyomi.domain.collection.interactor.CreateCollection
import tachiyomi.domain.collection.interactor.DeleteCollection
import tachiyomi.domain.collection.interactor.GetCollections
import tachiyomi.domain.collection.interactor.RenameCollection
import tachiyomi.domain.collection.interactor.ReorderCollection
import tachiyomi.domain.collection.model.Collection
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CollectionManagementScreenModel(
    private val getCollections: GetCollections = Injekt.get(),
    private val createCollection: CreateCollection = Injekt.get(),
    private val deleteCollection: DeleteCollection = Injekt.get(),
    private val reorderCollection: ReorderCollection = Injekt.get(),
    private val renameCollection: RenameCollection = Injekt.get(),
) : StateScreenModel<CollectionManagementState>(CollectionManagementState.Loading) {

    private val _events: Channel<CollectionManagementEvent> = Channel()
    val events = _events.receiveAsFlow()

    init {
        screenModelScope.launch {
            getCollections.subscribe()
                .collectLatest { collections ->
                    mutableState.update {
                        CollectionManagementState.Success(
                            collections = collections.toImmutableList(),
                        )
                    }
                }
        }
    }

    fun createCollection(name: String) {
        screenModelScope.launch {
            when (createCollection.await(name)) {
                is CreateCollection.Result.InternalError -> _events.send(CollectionManagementEvent.InternalError)
                else -> {}
            }
        }
    }

    fun deleteCollection(collectionId: Long) {
        screenModelScope.launch {
            when (deleteCollection.await(collectionId)) {
                is DeleteCollection.Result.InternalError -> _events.send(CollectionManagementEvent.InternalError)
                else -> {}
            }
        }
    }

    fun changeOrder(collection: Collection, newIndex: Int) {
        screenModelScope.launch {
            when (reorderCollection.await(collection, newIndex)) {
                is ReorderCollection.Result.InternalError -> _events.send(CollectionManagementEvent.InternalError)
                else -> {}
            }
        }
    }

    fun renameCollection(collectionId: Long, name: String) {
        screenModelScope.launch {
            when (renameCollection.await(collectionId, name)) {
                is RenameCollection.Result.InternalError -> _events.send(CollectionManagementEvent.InternalError)
                else -> {}
            }
        }
    }

    fun showDialog(dialog: CollectionManagementDialog) {
        mutableState.update {
            when (it) {
                CollectionManagementState.Loading -> it
                is CollectionManagementState.Success -> it.copy(dialog = dialog)
            }
        }
    }

    fun dismissDialog() {
        mutableState.update {
            when (it) {
                CollectionManagementState.Loading -> it
                is CollectionManagementState.Success -> it.copy(dialog = null)
            }
        }
    }
}

sealed interface CollectionManagementDialog {
    data object Create : CollectionManagementDialog
    data class Rename(val collection: Collection) : CollectionManagementDialog
    data class Delete(val collection: Collection) : CollectionManagementDialog
}

sealed interface CollectionManagementEvent {
    sealed class LocalizedMessage(val stringRes: StringResource) : CollectionManagementEvent
    data object InternalError : LocalizedMessage(MR.strings.internal_error)
}

sealed interface CollectionManagementState {

    @Immutable
    data object Loading : CollectionManagementState

    @Immutable
    data class Success(
        val collections: ImmutableList<Collection>,
        val dialog: CollectionManagementDialog? = null,
    ) : CollectionManagementState {

        val isEmpty: Boolean
            get() = collections.isEmpty()
    }
}
