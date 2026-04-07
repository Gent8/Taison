package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import tachiyomi.domain.collection.model.CollectionCoverData
import tachiyomi.domain.manga.model.MangaCover as MangaCoverModel

private val StackPeek = 5.dp

@Composable
fun CollectionCompactGridItem(
    title: String,
    coverData: List<CollectionCoverData>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    GridItemSelectable(
        isSelected = false,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    ) {
        MangaGridCover(
            cover = { StackedCollectionCover(coverData) },
            content = {
                if (showTitle) {
                    CoverTextOverlay(title = title)
                }
            },
        )
    }
}

@Composable
fun CollectionComfortableGridItem(
    title: String,
    coverData: List<CollectionCoverData>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GridItemSelectable(
        isSelected = false,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    ) {
        Column {
            MangaGridCover(
                cover = { StackedCollectionCover(coverData) },
            )
            GridItemTitle(
                modifier = Modifier.padding(4.dp),
                title = title,
                style = MaterialTheme.typography.titleSmall,
                minLines = 2,
            )
        }
    }
}

@Composable
fun CollectionListItem(
    title: String,
    coverData: List<CollectionCoverData>,
    entryCount: Long,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MangaListItem(
        title = title,
        coverData = coverData.firstOrNull()?.let { cover ->
            MangaCoverModel(
                mangaId = cover.mangaId,
                sourceId = cover.sourceId,
                isMangaFavorite = cover.isFavorite,
                url = cover.thumbnailUrl,
                lastModified = cover.coverLastModified,
            )
        } ?: MangaCoverModel(
            mangaId = 0,
            sourceId = 0,
            isMangaFavorite = false,
            url = null,
            lastModified = 0,
        ),
        badge = {},
        onClick = onClick,
        onLongClick = onLongClick,
        onClickContinueReading = null,
    )
}

/**
 * The first entry's cover sits at top-left while two subtle surface-tinted cards peek out
 * from the bottom-right, creating a layered stack effect. The front cover is sized to
 * fit fully within its slot so no part of the artwork is clipped.
 */
@Composable
private fun BoxScope.StackedCollectionCover(
    coverData: List<CollectionCoverData>,
) {
    val shape = MaterialTheme.shapes.extraSmall
    Box(modifier = Modifier.matchParentSize()) {
        // Far back card — peeks the most from bottom-right
        BackCard(
            modifier = Modifier
                .matchParentSize()
                .padding(start = StackPeek * 2, top = StackPeek * 2),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            elevation = 1.dp,
            shape = shape,
        )
        // Near back card — sits between front and far back
        BackCard(
            modifier = Modifier
                .matchParentSize()
                .padding(
                    start = StackPeek,
                    top = StackPeek,
                    end = StackPeek,
                    bottom = StackPeek,
                ),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            elevation = 2.dp,
            shape = shape,
        )
        // Front cover — sized to fit; anchored top-left
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = StackPeek * 2, bottom = StackPeek * 2)
                .shadow(elevation = 3.dp, shape = shape, clip = false)
                .clip(shape),
        ) {
            val firstCover = coverData.firstOrNull()
            if (firstCover != null) {
                MangaCover.Book(
                    modifier = Modifier.fillMaxSize(),
                    data = MangaCoverModel(
                        mangaId = firstCover.mangaId,
                        sourceId = firstCover.sourceId,
                        isMangaFavorite = firstCover.isFavorite,
                        url = firstCover.thumbnailUrl,
                        lastModified = firstCover.coverLastModified,
                    ),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "📁",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun BackCard(
    modifier: Modifier,
    color: Color,
    elevation: Dp,
    shape: Shape,
) {
    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(color)
            .border(
                width = Dp.Hairline,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            ),
    )
}
