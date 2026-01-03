package eu.kanade.presentation.collection.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover as MangaCoverModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A row item displaying a manga within a collection.
 * Shows the manga cover, title, optional badge, and a remove button.
 */
@Composable
fun CollectionItemRow(
    manga: Manga,
    badge: String?,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onBadgeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Manga cover
        MangaCover.Square(
            modifier = Modifier
                .size(48.dp, 68.dp)
                .clip(MaterialTheme.shapes.small),
            data = MangaCoverModel(
                mangaId = manga.id,
                sourceId = manga.source,
                isMangaFavorite = manga.favorite,
                url = manga.thumbnailUrl,
                lastModified = manga.coverLastModified,
            ),
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title and badge
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = manga.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (badge != null) {
                Spacer(modifier = Modifier.height(4.dp))
                AssistChip(
                    onClick = onBadgeClick,
                    label = {
                        Text(
                            text = badge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Label,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            } else {
                TextButton(
                    onClick = onBadgeClick,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.collection_add_badge),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        // Remove button
        IconButton(onClick = onRemoveClick) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(MR.strings.collection_remove_manga),
            )
        }
    }
}
