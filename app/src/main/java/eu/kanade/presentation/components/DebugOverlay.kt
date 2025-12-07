package eu.kanade.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.system.isDebugBuildType

/**
 * Overlay that visually flags debug builds and surfaces lightweight build metadata.
 * Guarded by [isDebugBuildType] so it never renders in release builds.
 */
@Composable
fun DebugOverlay() {
    if (!isDebugBuildType) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(Color(0x99FF0000))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = "DEBUG BUILD",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .background(Color(0x7F000000))
                .pointerInput(Unit) { }
                .semantics(mergeDescendants = true) { invisibleToUser() }
                .padding(8.dp),
        ) {
            Text(
                text = buildDebugInfo(),
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 12.sp,
            )
        }
    }
}

private fun buildDebugInfo(): String {
    val commitLabel = if (BuildConfig.COMMIT_SHA.isNotEmpty()) {
        BuildConfig.COMMIT_SHA.take(7)
    } else {
        "unknown"
    }
    return "Debug • ${BuildConfig.BUILD_TYPE} • v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • $commitLabel"
}

