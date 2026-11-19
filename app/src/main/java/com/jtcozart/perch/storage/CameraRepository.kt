package com.jtcozart.perch.storage

import com.jtcozart.perch.camera.Camera
import kotlinx.coroutines.flow.StateFlow

interface CameraRepository {
	val cameras: StateFlow<List<Camera>>

	/** Adds the camera, or replaces the saved one with the same id. */
	suspend fun save(camera: Camera)

	suspend fun delete(cameraId: String)

	/** Moves a camera one place earlier or later on the wall. */
	suspend fun move(cameraId: String, offset: Int)
}
