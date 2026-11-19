package com.jtcozart.perch.camera

/** The changes a person can make to the wall's camera list, as pure functions over the list. */
object CameraListEdits {
	fun save(cameras: List<Camera>, camera: Camera): List<Camera> {
		val index = cameras.indexOfFirst { it.id == camera.id }
		return if (index < 0) cameras + camera else cameras.toMutableList().also { it[index] = camera }
	}

	fun delete(cameras: List<Camera>, cameraId: String): List<Camera> = cameras.filterNot { it.id == cameraId }

	fun move(cameras: List<Camera>, cameraId: String, offset: Int): List<Camera> {
		val from = cameras.indexOfFirst { it.id == cameraId }
		if (from < 0) return cameras
		val to = (from + offset).coerceIn(0, cameras.lastIndex)
		return if (from == to) cameras else cameras.toMutableList().apply { add(to, removeAt(from)) }
	}
}
