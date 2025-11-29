package eu.kanade.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.i18n.stringResource

object TabbedDialogPaddings {
    val Horizontal = 24.dp
    val Vertical = 8.dp
}

private val TabStateSaver: Saver<MutableIntState, Int> = Saver(
    save = { it.intValue },
    restore = { value -> mutableIntStateOf(value) },
)

@Composable
fun rememberTabbedDialogState(initialPage: Int = 0): MutableIntState {
    return rememberSaveable(saver = TabStateSaver) {
        mutableIntStateOf(initialPage.coerceAtLeast(0))
    }
}

@Composable
fun TabbedDialog(
    onDismissRequest: () -> Unit,
    tabTitles: ImmutableList<String>,
    modifier: Modifier = Modifier,
    tabOverflowMenuContent: (@Composable ColumnScope.(() -> Unit) -> Unit)? = null,
    content: @Composable (Int) -> Unit,
) {
    val tabState = rememberTabbedDialogState()
    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = tabTitles,
        tabState = tabState,
        modifier = modifier,
        tabOverflowMenuContent = tabOverflowMenuContent,
        content = content,
    )
}

@Composable
fun TabbedDialog(
    onDismissRequest: () -> Unit,
    tabTitles: ImmutableList<String>,
    tabState: MutableIntState,
    modifier: Modifier = Modifier,
    tabOverflowMenuContent: (@Composable ColumnScope.(() -> Unit) -> Unit)? = null,
    content: @Composable (Int) -> Unit,
) {
    AdaptiveSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        val maxIndex = (tabTitles.size - 1).coerceAtLeast(0)
        val coercedPage = tabState.intValue.coerceIn(0, maxIndex)

        LaunchedEffect(maxIndex) {
            if (tabState.intValue != coercedPage) {
                tabState.intValue = coercedPage
            }
        }

        Column {
            Row {
                PrimaryTabRow(
                    modifier = Modifier.weight(1f),
                    selectedTabIndex = coercedPage,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    divider = {},
                ) {
                    tabTitles.fastForEachIndexed { index, tab ->
                        Tab(
                            selected = coercedPage == index,
                            onClick = { tabState.intValue = index },
                            text = { TabText(text = tab) },
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                tabOverflowMenuContent?.let { MoreMenu(it) }
            }
            HorizontalDivider()

            AnimatedContent(
                targetState = coercedPage,
                modifier = Modifier.pointerInput(tabTitles.size, coercedPage) {
                    var totalDrag = 0f
                    var initialPage = coercedPage
                    detectHorizontalDragGestures(
                        onDragStart = {
                            totalDrag = 0f
                            initialPage = tabState.intValue
                        },
                        onDragEnd = {
                            val threshold = 100f
                            when {
                                totalDrag > threshold && initialPage > 0 -> {
                                    tabState.intValue = initialPage - 1
                                }
                                totalDrag < -threshold && initialPage < tabTitles.size - 1 -> {
                                    tabState.intValue = initialPage + 1
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        },
                    )
                },
                transitionSpec = {
                    val fadeSpec = tween<Float>(durationMillis = 250, easing = FastOutSlowInEasing)
                    fadeIn(animationSpec = fadeSpec) togetherWith
                        fadeOut(animationSpec = fadeSpec) using SizeTransform { _, _ ->
                            tween<IntSize>(durationMillis = 350, easing = FastOutSlowInEasing)
                        }
                },
                contentAlignment = Alignment.TopStart,
                label = "tabContent",
            ) { page ->
                content(page)
            }
        }
    }
}

@Composable
private fun MoreMenu(
    content: @Composable ColumnScope.(() -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(MR.strings.label_more),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            content { expanded = false }
        }
    }
}
