package eu.kanade.tachiyomi.ui.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.collection.CollectionManagementContent
import eu.kanade.presentation.collection.components.CollectionCreateDialog
import eu.kanade.presentation.collection.components.CollectionDeleteDialog
import eu.kanade.presentation.collection.components.CollectionRenameDialog
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.presentation.core.screens.LoadingScreen

class CollectionManagementScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { CollectionManagementScreenModel() }

        val state by screenModel.state.collectAsState()

        if (state is CollectionManagementState.Loading) {
            LoadingScreen()
            return
        }

        val successState = state as CollectionManagementState.Success

        CollectionManagementContent(
            state = successState,
            onClickCreate = { screenModel.showDialog(CollectionManagementDialog.Create) },
            onClickRename = { screenModel.showDialog(CollectionManagementDialog.Rename(it)) },
            onClickDelete = { screenModel.showDialog(CollectionManagementDialog.Delete(it)) },
            onClickCollection = { navigator.push(CollectionScreen(it.id)) },
            onChangeOrder = screenModel::changeOrder,
            navigateUp = navigator::pop,
        )

        when (val dialog = successState.dialog) {
            null -> {}
            CollectionManagementDialog.Create -> {
                CollectionCreateDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onCreate = screenModel::createCollection,
                )
            }
            is CollectionManagementDialog.Rename -> {
                CollectionRenameDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onRename = { screenModel.renameCollection(dialog.collection.id, it) },
                    currentName = dialog.collection.name,
                )
            }
            is CollectionManagementDialog.Delete -> {
                CollectionDeleteDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onDelete = { screenModel.deleteCollection(dialog.collection.id) },
                    collectionName = dialog.collection.name,
                )
            }
        }

        LaunchedEffect(Unit) {
            screenModel.events.collectLatest { event ->
                if (event is CollectionManagementEvent.LocalizedMessage) {
                    context.toast(event.stringRes)
                }
            }
        }
    }
}
