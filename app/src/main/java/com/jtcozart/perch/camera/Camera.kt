package com.jtcozart.perch.camera

import kotlinx.serialization.Serializable

@Serializable
data class Camera(
	val id: String,
	val name: String,
	val source: CameraSource,
)
