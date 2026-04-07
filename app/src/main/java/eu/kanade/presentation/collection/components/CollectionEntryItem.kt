package eu.kanade.presentation.collection.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

private val EntryItemHeight = 96.dp

@Composable
fun ReorderableCollectionItemScope.CollectionEntryItem(
    entry: CollectionEntryWithManga,
    onClickManga: () -> Unit,
    onClickChangeLabel: () -> Unit,
    onClickRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClickManga)
                .height(EntryItemHeight)
                .padding(
                    start = MaterialTheme.padding.small,
                    end = MaterialTheme.padding.small,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.DragHandle,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = MaterialTheme.padding.small)
                    .draggableHandle(),
            )
            MangaCover.Book(
                modifier = Modifier.fillMaxHeight().padding(vertical = MaterialTheme.padding.small),
                data = entry.manga,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = MaterialTheme.padding.medium),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = entry.manga.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (entry.entry.label.isNotEmpty()) {
                    Text(
                        text = entry.entry.label.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
            }
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
}
