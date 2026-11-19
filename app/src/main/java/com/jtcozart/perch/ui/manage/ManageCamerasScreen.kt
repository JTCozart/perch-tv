package com.jtcozart.perch.ui.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtcozart.perch.camera.Camera
import com.jtcozart.perch.camera.CameraSource
import com.jtcozart.perch.camera.RedactedUrl
import com.jtcozart.perch.ui.components.PerchButton
import com.jtcozart.perch.ui.theme.PerchColors

@Composable
fun ManageCamerasScreen(
	cameras: List<Camera>,
	onAddCamera: () -> Unit,
	onEditCamera: (Camera) -> Unit,
	onMoveCamera: (Camera, Int) -> Unit,
	onDeleteCamera: (Camera) -> Unit,
	onDone: () -> Unit,
) {
	val addButton = remember { FocusRequester() }
	LaunchedEffect(Unit) { runCatching { addButton.requestFocus() } }

	Column(modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 28.dp)) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Text("Cameras", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = PerchColors.Text)
			Spacer(Modifier.weight(1f))
			PerchButton("Add a camera", onAddCamera, Modifier.focusRequester(addButton), isPrimary = true)
			Spacer(Modifier.padding(start = 12.dp))
			PerchButton("Back to the wall", onDone)
		}
		Text(
			"The wall shows cameras in this order, 16 to a page.",
			modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
			color = PerchColors.TextMuted,
		)
		LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
			items(cameras, key = { it.id }) { camera ->
				CameraRow(camera, onEditCamera, onMoveCamera, onDeleteCamera)
			}
		}
	}
}

@Composable
private fun CameraRow(
	camera: Camera,
	onEditCamera: (Camera) -> Unit,
	onMoveCamera: (Camera, Int) -> Unit,
	onDeleteCamera: (Camera) -> Unit,
) {
	var isConfirmingDelete by remember(camera.id) { mutableStateOf(false) }

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.background(PerchColors.Surface, RoundedCornerShape(10.dp))
			.padding(horizontal = 18.dp, vertical = 12.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(10.dp),
	) {
		Column(modifier = Modifier.weight(1f)) {
			Text(camera.name, style = MaterialTheme.typography.titleMedium, color = PerchColors.Text)
			Text(describe(camera.source), style = MaterialTheme.typography.bodySmall, color = PerchColors.TextMuted)
		}
		PerchButton("Edit", { onEditCamera(camera) })
		PerchButton("Move up", { onMoveCamera(camera, -1) })
		PerchButton("Move down", { onMoveCamera(camera, 1) })
		PerchButton(
			label = if (isConfirmingDelete) "Press again to remove" else "Remove",
			onClick = { if (isConfirmingDelete) onDeleteCamera(camera) else isConfirmingDelete = true },
		)
	}
}

private fun describe(source: CameraSource): String = when (source) {
	is CameraSource.TapoRtsp -> "Tapo RTSP at ${source.host}:${source.port}, ${source.lens.name.lowercase()} lens"
	is CameraSource.TapoLocal -> "Tapo app password at ${source.host}" + (source.deviceId?.let { ", hub camera $it" } ?: "")
	is CameraSource.BridgeStream -> "Bridge stream ${RedactedUrl.of(source.url)}"
}
