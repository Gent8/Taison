package eu.kanade.presentation.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.R
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun LogoHeader() {
    val uiPreferences = remember { Injekt.get<UiPreferences>() }
    val appTheme by uiPreferences.appTheme().changes()
        .collectAsState(initial = uiPreferences.appTheme().get())
    val isTaisonTheme = appTheme == AppTheme.DEFAULT

    if (isTaisonTheme) {
        TaisonLogoHeader()
    } else {
        DefaultLogoHeader()
    }
}

@Composable
private fun DefaultLogoHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_mihon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .padding(vertical = 56.dp)
                .size(64.dp),
        )

        HorizontalDivider()
    }
}

@Composable
private fun TaisonLogoHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center,
        ) {
            FeatureGraphicBackground()
            Icon(
                painter = painterResource(R.drawable.ic_mihon),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(64.dp),
            )
        }
    }
}

@Composable
private fun BoxScope.FeatureGraphicBackground() {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Image(
        painter = painterResource(R.drawable.feature_graphic),
        contentDescription = null,
        modifier = Modifier
            .matchParentSize()
            .blur(3.dp)
            .alpha(0.9f),
        contentScale = ContentScale.Crop,
    )

    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                Brush.verticalGradient(
                    0.0f to surfaceColor.copy(alpha = 0.3f),
                    0.2f to surfaceColor.copy(alpha = 0.05f),
                    0.5f to surfaceColor.copy(alpha = 0.0f),
                    0.7f to surfaceColor.copy(alpha = 0.15f),
                    0.85f to surfaceColor.copy(alpha = 0.5f),
                    1.0f to surfaceColor,
                ),
            ),
    )

    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                Brush.horizontalGradient(
                    0.0f to surfaceColor.copy(alpha = 0.25f),
                    0.15f to Color.Transparent,
                    0.85f to Color.Transparent,
                    1.0f to surfaceColor.copy(alpha = 0.25f),
                ),
            ),
    )
}
