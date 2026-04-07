package eu.kanade.presentation.collection.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import sh.calvin.reorderable.ReorderableCollectionItemScope
import tachiyomi.domain.collection.model.CollectionEntryWithManga
import tachiyomi.presentation.core.components.material.padding

private val EntryRowHeight = 72.dp
private val LeadingSlotSize = 48.dp
private val LabelSlotHeight = 24.dp

@Composable
fun CollectionEntryRow(
    entry: CollectionEntryWithManga,
    onClickRead: () -> Unit,
    onClickNavigateToManga: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(EntryRowHeight)
            .clickable(onClick = onClickRead)
            .padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaCover.Square(
            modifier = Modifier.size(40.dp),
            data = entry.manga,
        )
        EntryTextColumn(
            title = entry.manga.title,
            label = entry.entry.label,
            useChip = true,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = MaterialTheme.padding.medium),
        )
        IconButton(onClick = onClickNavigateToManga) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
            )
        }
    }
}

@Composable
fun ReorderableCollectionItemScope.CollectionEntryEditRow(
    entry: CollectionEntryWithManga,
    onClickChangeLabel: () -> Unit,
    onClickRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(EntryRowHeight)
            .padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(LeadingSlotSize)
                .draggableHandle(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.DragHandle,
                contentDescription = null,
            )
        }
        MangaCover.Square(
            modifier = Modifier.size(40.dp),
            data = entry.manga,
        )
        EntryTextColumn(
            title = entry.manga.title,
            label = entry.entry.label,
            useChip = false,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = MaterialTheme.padding.medium),
        )
        IconButton(onClick = onClickChangeLabel) {
            Icon(
                imageVector = Icons.Outlined.Label,
                contentDescription = null,
            )
        }
        IconButton(onClick = onClickRemove) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun EntryTextColumn(
    title: String,
    label: String,
    useChip: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        Box(
            modifier = Modifier.defaultMinSize(minHeight = LabelSlotHeight),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (label.isNotEmpty()) {
                if (useChip) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = label.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                } else {
                    Text(
                        text = label.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
