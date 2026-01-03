package eu.kanade.tachiyomi.ui.collection

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.collection.interactor.DeleteCollection
import tachiyomi.domain.collection.interactor.GetCollectionWithItems
import tachiyomi.domain.collection.interactor.RemoveMangaFromCollection
import tachiyomi.domain.collection.interactor.UpdateCollection
import tachiyomi.domain.collection.interactor.UpdateCollectionItemBadge
import tachiyomi.domain.collection.model.CollectionWithItems
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CollectionScreenModel(
    private val collectionId: Long,
    private val getCollectionWithItems: GetCollectionWithItems = Injekt.get(),
    private val deleteCollectionUseCase: DeleteCollection = Injekt.get(),
    private val removeMangaFromCollection: RemoveMangaFromCollection = Injekt.get(),
    private val updateCollectionItemBadge: UpdateCollectionItemBadge = Injekt.get(),
    private val updateCollection: UpdateCollection = Injekt.get(),
) : StateScreenModel<CollectionScreenModel.State>(State.Loading) {

    init {
        screenModelScope.launch {
            getCollectionWithItems.subscribe(collectionId).collect { data ->
                mutableState.update {
                    if (data != null) {
                        State.Success(
                            collectionWithItems = data,
                            dialog = null,
                        )
                    } else {
                        State.Error
                    }
                }
            }
        }
    }

    fun deleteCollection() {
        screenModelScope.launch {
            deleteCollectionUseCase.await(collectionId)
        }
    }

    fun removeItem(mangaId: Long) {
        screenModelScope.launch {
            removeMangaFromCollection.await(collectionId, mangaId)
        }
    }

    fun updateItemBadge(itemId: Long, badge: String?) {
        screenModelScope.launch {
            updateCollectionItemBadge.await(itemId, badge)
            dismissDialog()
        }
    }

    fun showEditDialog() {
        updateSuccessState { it.copy(dialog = Dialog.Edit) }
    }

    fun showBadgeDialog(itemId: Long, currentBadge: String?) {
        updateSuccessState { it.copy(dialog = Dialog.Badge(itemId, currentBadge)) }
    }

    fun showConfirmDeleteDialog() {
        updateSuccessState { it.copy(dialog = Dialog.ConfirmDelete) }
    }

    fun dismissDialog() {
        updateSuccessState { it.copy(dialog = null) }
    }

    fun updateCollection(name: String, description: String?) {
        screenModelScope.launch {
            updateCollection.awaitUpdate(collectionId, name, description)
            dismissDialog()
        }
    }

    private inline fun updateSuccessState(func: (State.Success) -> State.Success) {
        mutableState.update {
            when (it) {
                State.Loading -> it
                State.Error -> it
                is State.Success -> func(it)
            }
        }
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data object Error : State

        @Immutable
        data class Success(
            val collectionWithItems: CollectionWithItems,
            val dialog: Dialog? = null,
        ) : State
    }

    sealed interface Dialog {
        data object Edit : Dialog
        data class Badge(val itemId: Long, val currentBadge: String?) : Dialog
        data object ConfirmDelete : Dialog
    }
}
