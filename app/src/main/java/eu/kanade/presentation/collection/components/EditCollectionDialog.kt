package eu.kanade.presentation.collection.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Dialog for editing an existing collection.
 * Pre-fills the current name and description.
 */
@Composable
fun EditCollectionDialog(
    currentName: String,
    currentDescription: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    var description by remember { mutableStateOf(currentDescription ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(MR.strings.action_edit_collection)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = stringResource(MR.strings.collection_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(text = stringResource(MR.strings.collection_description_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description.ifBlank { null }) },
                enabled = name.isNotBlank(),
            ) {
                Text(text = stringResource(MR.strings.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}
