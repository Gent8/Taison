package eu.kanade.presentation.category

import android.content.Context
import androidx.compose.runtime.Composable
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

val Category.visualName: String
    @Composable
    get() = when {
        isSystemCategory && name.isBlank() -> stringResource(MR.strings.label_default)
        isSystemCategory -> name
        else -> name
    }

fun Category.visualName(context: Context): String =
    when {
        isSystemCategory && name.isBlank() -> context.stringResource(MR.strings.label_default)
        isSystemCategory -> name
        else -> name
    }
