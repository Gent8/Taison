package eu.kanade.presentation.collection.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
 * Dialog for setting or editing a badge on a collection item.
 * Shows current badge value and allows modification or removal.
 */
@Composable
fun BadgeInputDialog(
    currentBadge: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var badge by remember { mutableStateOf(currentBadge ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(MR.strings.collection_set_badge)) },
        text = {
            Column {
                Text(
                    text = stringResource(MR.strings.collection_badge_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = badge,
                    onValueChange = { badge = it },
                    label = { Text(text = stringResource(MR.strings.collection_badge)) },
                    placeholder = { Text(text = stringResource(MR.strings.collection_badge_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(badge.ifBlank { null }) },
            ) {
                Text(text = stringResource(MR.strings.action_save))
            }
        },
        dismissButton = {
            Row {
                if (currentBadge != null) {
                    TextButton(onClick = { onConfirm(null) }) {
                        Text(text = stringResource(MR.strings.action_remove))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            }
        },
    )
}
