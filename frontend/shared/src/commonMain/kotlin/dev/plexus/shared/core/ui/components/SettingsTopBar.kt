package dev.plexus.shared.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.plexus.shared.core.ui.theme.PlexusThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsTopBar(
    title: String,
    onBack: () -> Unit,
) {
    val dimens = PlexusThemeTokens.dimens
    val shapes = PlexusThemeTokens.shapes

    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            OutlinedButton(
                onClick = onBack,
                shape = shapes.radiusSm,
                contentPadding = PaddingValues(horizontal = dimens.space12),
                modifier = Modifier.height(dimens.topBarButtonHeight).widthIn(min = dimens.minTapTargetWidth),
            ) {
                Text("Back")
            }
        },
    )
}
