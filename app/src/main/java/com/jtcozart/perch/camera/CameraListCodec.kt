package com.jtcozart.perch.camera

import kotlinx.serialization.json.Json

object CameraListCodec {
	private val json = Json { ignoreUnknownKeys = true }

	fun encode(cameras: List<Camera>): String = json.encodeToString(cameras)

	fun decode(text: String): List<Camera> = if (text.isBlank()) emptyList() else json.decodeFromString(text)
}
