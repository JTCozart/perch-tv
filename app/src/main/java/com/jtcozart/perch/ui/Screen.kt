package com.jtcozart.perch.ui

sealed interface Screen {
	data object Wall : Screen
	data class FullScreen(val cameraId: String) : Screen
	data object ManageCameras : Screen

	/** [cameraId] is null when adding a new camera. */
	data class EditCamera(val cameraId: String?) : Screen

	/** Adds a new camera that starts as a copy of [sourceCameraId]. */
	data class DuplicateCamera(val sourceCameraId: String) : Screen
}
