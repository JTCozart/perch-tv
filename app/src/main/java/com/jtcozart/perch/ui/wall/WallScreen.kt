package com.jtcozart.perch.ui.wall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtcozart.perch.camera.Camera
import com.jtcozart.perch.ui.components.PerchButton
import com.jtcozart.perch.ui.theme.PerchColors
import com.jtcozart.perch.wall.WallLayoutPlanner

@Composable
fun WallScreen(
	cameras: List<Camera>,
	liveTileLimit: Int,
	onOpenCamera: (Camera) -> Unit,
	onManageCameras: () -> Unit,
	onAddCamera: () -> Unit,
) {
	val pageCount = WallLayoutPlanner.pageCount(cameras.size)
	var requestedPage by rememberSaveable { mutableIntStateOf(0) }
	// Removing cameras can leave the remembered page past the end
	val page = requestedPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
	val pageCameras = cameras.drop(page * WallLayoutPlanner.TILES_PER_PAGE).take(WallLayoutPlanner.TILES_PER_PAGE)

	Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 20.dp)) {
		WallHeader(
			page = page,
			pageCount = pageCount,
			onPreviousPage = { requestedPage = page - 1 },
			onNextPage = { requestedPage = page + 1 },
			onManageCameras = onManageCameras,
		)
		Spacer(Modifier.padding(top = 12.dp))
		if (cameras.isEmpty()) {
			EmptyWall(onAddCamera)
		} else {
			CameraGrid(pageCameras, liveTileLimit, onOpenCamera, Modifier.weight(1f))
		}
	}
}

@Composable
private fun WallHeader(page: Int, pageCount: Int, onPreviousPage: () -> Unit, onNextPage: () -> Unit, onManageCameras: () -> Unit) {
	Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
		Text("Perch", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = PerchColors.Accent)
		Spacer(Modifier.weight(1f))
		if (pageCount > 1) {
			if (page > 0) PerchButton("Previous page", onPreviousPage)
			Text(
				"Page ${page + 1} of $pageCount",
				modifier = Modifier.padding(horizontal = 16.dp),
				color = PerchColors.TextMuted,
			)
			if (page < pageCount - 1) PerchButton("Next page", onNextPage)
			Spacer(Modifier.padding(start = 16.dp))
		}
		PerchButton("Cameras", onManageCameras)
	}
}

@Composable
private fun CameraGrid(cameras: List<Camera>, liveTileLimit: Int, onOpenCamera: (Camera) -> Unit, modifier: Modifier) {
	val layout = WallLayoutPlanner.plan(cameras.size, liveTileLimit)
	val firstTile = remember { FocusRequester() }
	LaunchedEffect(cameras.firstOrNull()?.id) { runCatching { firstTile.requestFocus() } }

	Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
		for (row in 0 until layout.rows) {
			Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
				for (column in 0 until layout.columns) {
					val index = row * layout.columns + column
					val camera = cameras.getOrNull(index)
					if (camera == null) {
						Spacer(Modifier.weight(1f))
					} else {
						CameraTile(
							camera = camera,
							isLive = index < layout.liveTiles,
							onSelect = { onOpenCamera(camera) },
							modifier = Modifier
								.weight(1f)
								.fillMaxSize()
								.then(if (index == 0) Modifier.focusRequester(firstTile) else Modifier),
						)
					}
				}
			}
		}
	}
}

@Composable
private fun EmptyWall(onAddCamera: () -> Unit) {
	val addButton = remember { FocusRequester() }
	LaunchedEffect(Unit) { runCatching { addButton.requestFocus() } }

	Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
		Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
			Text("No cameras yet", style = MaterialTheme.typography.headlineMedium, color = PerchColors.Text)
			Text(
				"Add a wired Tapo camera with its camera account, or any RTSP or HLS stream from a bridge.",
				color = PerchColors.TextMuted,
			)
			PerchButton("Add a camera", onAddCamera, Modifier.focusRequester(addButton), isPrimary = true)
		}
	}
}
