package com.jtcozart.perch.ui.wall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jtcozart.perch.camera.Camera
import com.jtcozart.perch.camera.StreamQuality
import com.jtcozart.perch.camera.StreamUrlResolver
import com.jtcozart.perch.ui.components.FocusableSurface
import com.jtcozart.perch.ui.player.CameraVideo
import com.jtcozart.perch.ui.player.VideoStatus
import com.jtcozart.perch.ui.theme.PerchColors

@Composable
fun CameraTile(
	camera: Camera,
	isLive: Boolean,
	onSelect: () -> Unit,
	modifier: Modifier = Modifier,
) {
	var status by remember(camera) { mutableStateOf(VideoStatus.Connecting) }

	FocusableSurface(onClick = onSelect, modifier = modifier, focusedScale = 1.02f) {
		Box(modifier = Modifier.fillMaxSize()) {
			if (isLive) {
				CameraVideo(
					url = StreamUrlResolver.resolve(camera.source, StreamQuality.Sub),
					withSound = false,
					onStatusChange = { status = it },
					modifier = Modifier.fillMaxSize(),
				)
				StatusBadge(status, Modifier.align(Alignment.TopEnd).padding(8.dp))
			} else {
				WaitingTile(Modifier.align(Alignment.Center))
			}
			NameLabel(camera.name, Modifier.align(Alignment.BottomStart).padding(8.dp))
		}
	}
}

@Composable
private fun WaitingTile(modifier: Modifier) {
	Column(modifier = modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
		Text("Paused to spare the TV's decoder", color = PerchColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
		Text("Select to watch", color = PerchColors.Text, style = MaterialTheme.typography.bodyMedium)
	}
}

@Composable
private fun NameLabel(name: String, modifier: Modifier) {
	Text(
		text = name,
		modifier = modifier
			.background(PerchColors.Scrim, RoundedCornerShape(6.dp))
			.padding(horizontal = 10.dp, vertical = 4.dp),
		color = PerchColors.Text,
		style = MaterialTheme.typography.titleSmall,
		fontWeight = FontWeight.SemiBold,
		maxLines = 1,
		overflow = TextOverflow.Ellipsis,
	)
}

@Composable
fun StatusBadge(status: VideoStatus, modifier: Modifier = Modifier) {
	val color = when (status) {
		VideoStatus.Live -> PerchColors.Live
		VideoStatus.Connecting -> PerchColors.TextMuted
		VideoStatus.Offline -> PerchColors.Problem
	}
	Text(
		text = status.label,
		modifier = modifier
			.background(PerchColors.Scrim, RoundedCornerShape(6.dp))
			.padding(horizontal = 8.dp, vertical = 2.dp),
		color = color,
		style = MaterialTheme.typography.labelMedium,
	)
}
