package eu.kanade.presentation.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun HistoryDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: (Boolean) -> Unit,
) {
    var removeEverything by remember { mutableStateOf(false) }

    AlertDialog(
        title = {
            Text(text = stringResource(MR.strings.action_remove))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                Text(text = stringResource(MR.strings.dialog_with_checkbox_remove_description))

                LabeledCheckbox(
                    label = stringResource(MR.strings.dialog_with_checkbox_reset),
                    checked = removeEverything,
                    onCheckedChange = { removeEverything = it },
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onDelete(removeEverything)
                onDismissRequest()
            }) {
                Text(text = stringResource(MR.strings.action_remove))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
fun HistoryDeleteAllDialog(
    canTargetActiveScope: Boolean,
    activeScopeLabel: String,
    includeNonLibraryLabel: Boolean,
    selection: HistoryScreenModel.HistoryDeletionScope,
    onSelectionChange: (HistoryScreenModel.HistoryDeletionScope) -> Unit,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = stringResource(MR.strings.history_delete_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                Text(text = stringResource(MR.strings.history_delete_scope_message))
                if (canTargetActiveScope) {
                    val activeDescription = if (includeNonLibraryLabel) {
                        stringResource(
                            MR.strings.history_delete_scope_active_with_nonlibrary,
                            activeScopeLabel,
                        )
                    } else {
                        stringResource(
                            MR.strings.history_delete_scope_active,
                            activeScopeLabel,
                        )
                    }
                    HistoryDeleteScopeRow(
                        title = stringResource(MR.strings.history_delete_scope_active_title),
                        description = activeDescription,
                        selected = selection == HistoryScreenModel.HistoryDeletionScope.ACTIVE_SCOPE,
                        onClick = { onSelectionChange(HistoryScreenModel.HistoryDeletionScope.ACTIVE_SCOPE) },
                    )
                }
                HistoryDeleteScopeRow(
                    title = stringResource(MR.strings.history_delete_scope_everything),
                    description = stringResource(MR.strings.history_delete_scope_everything_description),
                    selected = selection == HistoryScreenModel.HistoryDeletionScope.EVERYTHING,
                    onClick = { onSelectionChange(HistoryScreenModel.HistoryDeletionScope.EVERYTHING) },
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onDelete()
                onDismissRequest()
            }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun HistoryDeleteDialogPreview() {
    TachiyomiPreviewTheme {
        HistoryDeleteDialog(
            onDismissRequest = {},
            onDelete = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HistoryDeleteAllDialogPreview() {
    TachiyomiPreviewTheme {
        HistoryDeleteAllDialog(
            canTargetActiveScope = true,
            activeScopeLabel = "Action",
            includeNonLibraryLabel = true,
            selection = HistoryScreenModel.HistoryDeletionScope.ACTIVE_SCOPE,
            onSelectionChange = {},
            onDismissRequest = {},
            onDelete = {},
        )
    }
}

@Composable
private fun HistoryDeleteScopeRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = MaterialTheme.padding.small)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
