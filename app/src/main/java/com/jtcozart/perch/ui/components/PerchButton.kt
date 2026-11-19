package com.jtcozart.perch.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtcozart.perch.ui.theme.PerchColors

@Composable
fun PerchButton(
	label: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	isPrimary: Boolean = false,
) {
	FocusableSurface(
		onClick = onClick,
		modifier = modifier,
		color = if (isPrimary) PerchColors.Accent else PerchColors.SurfaceRaised,
		// An amber ring would vanish against an amber button
		focusRingColor = if (isPrimary) PerchColors.Text else PerchColors.Accent,
		focusedScale = 1.06f,
	) {
		Text(
			text = label,
			modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
			color = if (isPrimary) PerchColors.OnAccent else PerchColors.Text,
			style = MaterialTheme.typography.titleSmall,
			fontWeight = FontWeight.SemiBold,
		)
	}
}
