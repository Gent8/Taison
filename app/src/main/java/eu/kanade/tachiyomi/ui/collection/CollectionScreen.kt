package eu.kanade.tachiyomi.ui.collection

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.collection.CollectionDetailScreen
import eu.kanade.presentation.collection.components.CollectionDeleteDialog
import eu.kanade.presentation.collection.components.CollectionRenameDialog
import eu.kanade.presentation.collection.components.EditDescriptionDialog
import eu.kanade.presentation.collection.components.RemoveEntryDialog
import eu.kanade.presentation.collection.components.SelectLabelDialog
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.screens.LoadingScreen

class CollectionScreen(
    private val collectionId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val screenModel = rememberScreenModel { CollectionScreenModel(collectionId) }

        val state by screenModel.state.collectAsState()

        if (state is CollectionScreenState.Loading) {
            LoadingScreen()
            return
        }

        val successState = state as CollectionScreenState.Success

        CollectionDetailScreen(
            state = successState,
            onClickReadEntry = { mangaId ->
                scope.launchIO {
                    val chapter = screenModel.getNextUnreadChapter(mangaId)
                    if (chapter != null) {
                        context.startActivity(
                            ReaderActivity.newIntent(context, chapter.mangaId, chapter.id),
                        )
                    } else {
                        context.toast(MR.strings.no_next_chapter)
                    }
                }
            },
            onClickNavigateToManga = { mangaId -> navigator.push(MangaScreen(mangaId)) },
            onClickRename = { screenModel.showDialog(CollectionDialog.Rename) },
            onClickDelete = { screenModel.showDialog(CollectionDialog.Delete) },
            onClickEditMode = screenModel::toggleEditMode,
            onClickChangeLabel = { entryId, currentLabel ->
                screenModel.showDialog(CollectionDialog.ChangeLabel(entryId, currentLabel))
            },
            onClickRemoveEntry = { entryId, mangaTitle ->
                screenModel.showDialog(CollectionDialog.RemoveEntry(entryId, mangaTitle))
            },
            onChangeOrder = screenModel::changeEntryOrder,
            onClickEditDescription = {
                screenModel.showDialog(
                    CollectionDialog.EditDescription(successState.collection.description),
                )
            },
            onClickContinueReading = {
                val firstEntry = successState.entries.firstOrNull() ?: return@CollectionDetailScreen
                scope.launchIO {
                    val chapter = screenModel.getNextUnreadChapter(firstEntry.manga.id)
                    if (chapter != null) {
                        context.startActivity(
                            ReaderActivity.newIntent(context, chapter.mangaId, chapter.id),
                        )
                    } else {
                        context.toast(MR.strings.no_next_chapter)
                    }
                }
            },
            navigateUp = navigator::pop,
        )

        when (val dialog = successState.dialog) {
            null -> {}
            CollectionDialog.Rename -> {
                CollectionRenameDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onRename = screenModel::renameCollection,
                    currentName = successState.collection.name,
                )
            }
            CollectionDialog.Delete -> {
                CollectionDeleteDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onDelete = screenModel::deleteCollection,
                    collectionName = successState.collection.name,
                )
            }
            is CollectionDialog.ChangeLabel -> {
                SelectLabelDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onSelectLabel = { label ->
                        screenModel.updateLabel(dialog.entryId, label)
                    },
                    currentLabel = dialog.currentLabel,
                )
            }
            is CollectionDialog.RemoveEntry -> {
                RemoveEntryDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onRemove = { screenModel.removeEntry(dialog.entryId) },
                    mangaTitle = dialog.mangaTitle,
                )
            }
            is CollectionDialog.EditDescription -> {
                EditDescriptionDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onSave = screenModel::updateDescription,
                    currentDescription = dialog.currentDescription,
                )
            }
        }

        LaunchedEffect(Unit) {
            screenModel.events.collectLatest { event ->
                when (event) {
                    is CollectionEvent.LocalizedMessage -> context.toast(event.stringRes)
                    is CollectionEvent.CollectionDeleted -> navigator.pop()
                }
            }
        }
    }
}
