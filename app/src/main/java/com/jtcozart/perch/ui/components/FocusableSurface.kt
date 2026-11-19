package com.jtcozart.perch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jtcozart.perch.ui.theme.PerchColors

/**
 * A remote-friendly clickable box. Material's focus state layer is too faint to see from across a room, so focus
 * here is an amber ring and a slight lift.
 */
@Composable
fun FocusableSurface(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	color: Color = PerchColors.Surface,
	focusRingColor: Color = PerchColors.Accent,
	focusedScale: Float = 1.03f,
	content: @Composable (isFocused: Boolean) -> Unit,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()
	val shape = RoundedCornerShape(10.dp)

	Box(
		modifier = modifier
			.scale(if (isFocused) focusedScale else 1f)
			.clip(shape)
			.background(color)
			.border(width = 3.dp, color = if (isFocused) focusRingColor else Color.Transparent, shape = shape)
			.clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
	) {
		content(isFocused)
	}
}
