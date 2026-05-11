package eu.kanade.tachiyomi.ui.deeplink

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen

class DeepLinkScreen(
    val query: String = "",
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel {
            DeepLinkScreenModel(query = query)
        }
        val state by screenModel.state.collectAsState()
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.action_search_hint),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            when (val s = state) {
                is DeepLinkScreenModel.State.Loading -> {
                    LoadingScreen(Modifier.padding(contentPadding))
                }
                is DeepLinkScreenModel.State.NoResults -> {
                    navigator.replace(GlobalSearchScreen(query))
                }
                is DeepLinkScreenModel.State.SourceNotInstalled -> {
                    val sourceLabel = s.sourceName ?: s.sourceId.toString()
                    val message = stringResource(MR.strings.deep_link_source_not_installed, sourceLabel, s.title)
                    val scope = rememberCoroutineScope()
                    EmptyScreen(
                        message = message,
                        modifier = Modifier.padding(contentPadding),
                        actions = persistentListOf(
                            EmptyScreenAction(
                                stringRes = MR.strings.label_extensions,
                                icon = Icons.Outlined.Extension,
                                onClick = {
                                    scope.launch { HomeScreen.openTab(HomeScreen.Tab.Browse(toExtensions = true)) }
                                },
                            ),
                            EmptyScreenAction(
                                stringRes = MR.strings.action_global_search,
                                icon = Icons.Outlined.Search,
                                onClick = {
                                    navigator.replace(GlobalSearchScreen(s.title))
                                },
                            ),
                        ),
                    )
                }
                is DeepLinkScreenModel.State.Result -> {
                    if (s.chapterId == null) {
                        navigator.replace(
                            MangaScreen(
                                s.manga.id,
                                true,
                            ),
                        )
                    } else {
                        navigator.pop()
                        ReaderActivity.newIntent(
                            context,
                            s.manga.id,
                            s.chapterId,
                        ).also(context::startActivity)
                    }
                }
            }
        }
    }
}

