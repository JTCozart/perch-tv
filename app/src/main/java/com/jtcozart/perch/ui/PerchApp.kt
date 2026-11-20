package com.jtcozart.perch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jtcozart.perch.camera.CameraForm
import com.jtcozart.perch.camera.CameraFormValidator
import com.jtcozart.perch.storage.CameraRepository
import com.jtcozart.perch.ui.form.CameraFormScreen
import com.jtcozart.perch.ui.fullscreen.FullScreenCamera
import com.jtcozart.perch.ui.manage.ManageCamerasScreen
import com.jtcozart.perch.ui.theme.PerchColors
import com.jtcozart.perch.ui.theme.PerchTheme
import com.jtcozart.perch.ui.wall.WallScreen
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun PerchApp(repository: CameraRepository, liveTileLimit: Int, onCheckForUpdates: () -> Unit) {
	val cameras by repository.cameras.collectAsStateWithLifecycle()
	val scope = rememberCoroutineScope()
	var screen by remember { mutableStateOf<Screen>(Screen.Wall) }

	BackHandler(enabled = screen != Screen.Wall) {
		screen = when (screen) {
			is Screen.EditCamera -> Screen.ManageCameras
			else -> Screen.Wall
		}
	}

	PerchTheme {
		Box(modifier = Modifier.fillMaxSize().background(PerchColors.Background)) {
			when (val current = screen) {
				Screen.Wall -> WallScreen(
					cameras = cameras,
					liveTileLimit = liveTileLimit,
					onOpenCamera = { screen = Screen.FullScreen(it.id) },
					onManageCameras = { screen = Screen.ManageCameras },
					onAddCamera = { screen = Screen.EditCamera(cameraId = null) },
				)

				is Screen.FullScreen -> FullScreenCamera(
					cameras = cameras,
					cameraId = current.cameraId,
					onShowCamera = { screen = Screen.FullScreen(it.id) },
				)

				Screen.ManageCameras -> ManageCamerasScreen(
					cameras = cameras,
					onAddCamera = { screen = Screen.EditCamera(cameraId = null) },
					onEditCamera = { screen = Screen.EditCamera(it.id) },
					onMoveCamera = { camera, offset -> scope.launch { repository.move(camera.id, offset) } },
					onDeleteCamera = { camera -> scope.launch { repository.delete(camera.id) } },
					onCheckForUpdates = onCheckForUpdates,
					onDone = { screen = Screen.Wall },
				)

				is Screen.EditCamera -> {
					val existing = cameras.firstOrNull { it.id == current.cameraId }
					CameraFormScreen(
						initialForm = existing?.let(CameraForm::from) ?: CameraForm(),
						isNew = existing == null,
						onSave = { form ->
							val camera = CameraFormValidator.toCamera(form, id = existing?.id ?: UUID.randomUUID().toString())
							scope.launch {
								repository.save(camera)
								screen = Screen.ManageCameras
							}
						},
						onCancel = { screen = Screen.ManageCameras },
					)
				}
			}
		}
	}
}
