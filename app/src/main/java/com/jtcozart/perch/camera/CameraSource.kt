package com.jtcozart.perch.camera

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Where a camera's video comes from. Each kind is resolved to a playable URL by [StreamUrlResolver],
 * so a new kind of camera is a new subtype here plus one branch there.
 */
@Serializable
sealed interface CameraSource {
	/** A mains-powered Tapo camera reached directly over RTSP with its camera account. */
	@Serializable
	@SerialName("tapo-rtsp")
	data class TapoRtsp(
		val host: String,
		val port: Int = DEFAULT_RTSP_PORT,
		val username: String,
		val password: String,
		val lens: Lens = Lens.Wide,
	) : CameraSource

	/**
	 * Any stream another box on the network re-publishes (go2rtc, Home Assistant, an NVR). This is the route for
	 * cameras that cannot be played directly, such as battery and solar models with no RTSP of their own.
	 */
	@Serializable
	@SerialName("bridge")
	data class BridgeStream(
		val url: String,
		val subStreamUrl: String? = null,
	) : CameraSource

	companion object {
		const val DEFAULT_RTSP_PORT = 554
	}
}
