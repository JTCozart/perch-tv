package com.jtcozart.perch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val perchColorScheme = darkColorScheme(
	primary = PerchColors.Accent,
	onPrimary = PerchColors.OnAccent,
	background = PerchColors.Background,
	onBackground = PerchColors.Text,
	surface = PerchColors.Surface,
	onSurface = PerchColors.Text,
	onSurfaceVariant = PerchColors.TextMuted,
	error = PerchColors.Problem,
)

@Composable
fun PerchTheme(content: @Composable () -> Unit) {
	MaterialTheme(colorScheme = perchColorScheme, content = content)
}
