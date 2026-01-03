package eu.kanade.tachiyomi.ui.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.collection.CollectionScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import tachiyomi.presentation.core.screens.LoadingScreen

class CollectionScreen(private val collectionId: Long) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { CollectionScreenModel(collectionId) }
        val state by screenModel.state.collectAsState()

        if (state is CollectionScreenModel.State.Loading) {
            LoadingScreen()
            return
        }

        val successState = state as? CollectionScreenModel.State.Success
        if (successState == null) {
            // Error state - navigate back
            return
        }

        CollectionScreen(
            state = successState,
            onBackClick = navigator::pop,
            onMangaClick = { mangaId ->
                navigator.push(MangaScreen(mangaId))
            },
            onEditClick = screenModel::showEditDialog,
            onDeleteClick = {
                screenModel.showConfirmDeleteDialog()
            },
            onConfirmDeleteClick = {
                screenModel.deleteCollection()
                navigator.pop()
            },
            onRemoveItemClick = screenModel::removeItem,
            onBadgeClick = screenModel::showBadgeDialog,
            onUpdateBadgeClick = screenModel::updateItemBadge,
            onUpdateCollection = screenModel::updateCollection,
            onDismissDialog = screenModel::dismissDialog,
        )
    }
}
