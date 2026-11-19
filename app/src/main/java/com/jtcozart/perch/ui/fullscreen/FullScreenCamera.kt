package com.jtcozart.perch.ui.fullscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtcozart.perch.camera.Camera
import com.jtcozart.perch.camera.StreamQuality
import com.jtcozart.perch.camera.StreamUrlResolver
import com.jtcozart.perch.ui.player.CameraVideo
import com.jtcozart.perch.ui.player.VideoStatus
import com.jtcozart.perch.ui.theme.PerchColors
import com.jtcozart.perch.ui.wall.StatusBadge

/** One camera at full quality with sound. Left and right step through the other cameras. */
@Composable
fun FullScreenCamera(
	cameras: List<Camera>,
	cameraId: String,
	onShowCamera: (Camera) -> Unit,
) {
	val index = cameras.indexOfFirst { it.id == cameraId }.coerceAtLeast(0)
	val camera = cameras.getOrNull(index) ?: return
	var status by remember(camera.id) { mutableStateOf(VideoStatus.Connecting) }
	val focus = remember { FocusRequester() }
	LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

	Box(
		modifier = Modifier
			.fillMaxSize()
			.focusRequester(focus)
			.focusable()
			.onKeyEvent { event ->
				if (event.type != KeyEventType.KeyDown || cameras.size < 2) return@onKeyEvent false
				when (event.key) {
					Key.DirectionLeft -> { onShowCamera(cameras[(index - 1 + cameras.size) % cameras.size]); true }
					Key.DirectionRight -> { onShowCamera(cameras[(index + 1) % cameras.size]); true }
					else -> false
				}
			},
	) {
		// A fresh player per camera, so its status never carries over from the camera shown before
		key(camera.id) {
			CameraVideo(
				url = StreamUrlResolver.resolve(camera.source, StreamQuality.Main),
				withSound = true,
				onStatusChange = { status = it },
				modifier = Modifier.fillMaxSize(),
			)
		}
		Column(
			modifier = Modifier
				.align(Alignment.TopStart)
				.padding(24.dp)
				.background(PerchColors.Scrim, RoundedCornerShape(8.dp))
				.padding(horizontal = 14.dp, vertical = 8.dp),
		) {
			Text(camera.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PerchColors.Text)
			if (cameras.size > 1) {
				Text("Left and right switch cameras. Back returns to the wall.", color = PerchColors.TextMuted, style = MaterialTheme.typography.bodySmall)
			}
		}
		StatusBadge(status, Modifier.align(Alignment.TopEnd).padding(24.dp))
	}
}
