package eu.kanade.presentation.collection.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.delay
import tachiyomi.domain.collection.model.RelationshipLabel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.seconds

@Composable
fun CollectionCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = name.isNotEmpty(),
                onClick = {
                    onCreate(name)
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.action_create_collection))
        },
        text = {
            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequester),
                value = name,
                onValueChange = { name = it },
                label = {
                    Text(text = stringResource(MR.strings.collection_name_hint))
                },
                singleLine = true,
            )
        },
    )

    LaunchedEffect(focusRequester) {
        delay(0.1.seconds)
        focusRequester.requestFocus()
    }
}

@Composable
fun CollectionRenameDialog(
    onDismissRequest: () -> Unit,
    onRename: (String) -> Unit,
    currentName: String,
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(currentName, selection = TextRange(currentName.length)))
    }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = textFieldValue.text.isNotEmpty(),
                onClick = {
                    onRename(textFieldValue.text)
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.action_rename_collection))
        },
        text = {
            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequester),
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                label = {
                    Text(text = stringResource(MR.strings.collection_name_hint))
                },
                singleLine = true,
            )
        },
    )

    LaunchedEffect(focusRequester) {
        delay(0.1.seconds)
        focusRequester.requestFocus()
    }
}

@Composable
fun CollectionDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    collectionName: String,
) {
    AlertDialog(
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
        title = {
            Text(text = stringResource(MR.strings.action_delete_collection))
        },
        text = {
            Text(text = stringResource(MR.strings.action_delete_collection_confirm))
        },
    )
}

@Composable
fun RemoveEntryDialog(
    onDismissRequest: () -> Unit,
    onRemove: () -> Unit,
    mangaTitle: String,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onRemove()
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
        title = {
            Text(text = stringResource(MR.strings.action_remove_from_collection))
        },
        text = {
            Text(text = mangaTitle)
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectLabelDialog(
    onDismissRequest: () -> Unit,
    onSelectLabel: (String) -> Unit,
    currentLabel: String,
) {
    var labelText by remember {
        mutableStateOf(TextFieldValue(currentLabel, selection = TextRange(currentLabel.length)))
    }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onSelectLabel(labelText.text)
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
        title = {
            Text(text = stringResource(MR.strings.collection_select_label))
        },
        text = {
            Column {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    value = labelText,
                    onValueChange = { labelText = it },
                    singleLine = true,
                    label = {
                        Text(text = stringResource(MR.strings.action_change_label))
                    },
                )

                // Suggestion chips
                FlowRow(
                    modifier = Modifier.padding(top = MaterialTheme.padding.small),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                ) {
                    val suggestions = listOf(
                        RelationshipLabel.PREQUEL to stringResource(MR.strings.label_prequel),
                        RelationshipLabel.SEQUEL to stringResource(MR.strings.label_sequel),
                        RelationshipLabel.SPIN_OFF to stringResource(MR.strings.label_spin_off),
                        RelationshipLabel.SIDE_STORY to stringResource(MR.strings.label_side_story),
                        RelationshipLabel.ALTERNATE to stringResource(MR.strings.label_alternate),
                    )

                    suggestions.forEach { (value, displayName) ->
                        SuggestionChip(
                            onClick = {
                                labelText = TextFieldValue(value, selection = TextRange(value.length))
                            },
                            label = { Text(text = displayName) },
                        )
                    }
                }
            }
        },
    )

    LaunchedEffect(focusRequester) {
        delay(0.1.seconds)
        focusRequester.requestFocus()
    }
}

@Composable
fun EditDescriptionDialog(
    onDismissRequest: () -> Unit,
    onSave: (String) -> Unit,
    currentDescription: String,
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(currentDescription, selection = TextRange(currentDescription.length)))
    }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onSave(textFieldValue.text)
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
        title = {
            Text(text = stringResource(MR.strings.collection_edit_description))
        },
        text = {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                label = {
                    Text(text = stringResource(MR.strings.collection_description_hint))
                },
                maxLines = 5,
            )
        },
    )

    LaunchedEffect(focusRequester) {
        delay(0.1.seconds)
        focusRequester.requestFocus()
    }
}
