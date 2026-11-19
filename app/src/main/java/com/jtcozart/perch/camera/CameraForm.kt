package com.jtcozart.perch.camera

/** What the person typed on the add or edit screen, kept as text until [CameraFormValidator] accepts it. */
data class CameraForm(
	val kind: CameraKind = CameraKind.TapoRtsp,
	val name: String = "",
	val host: String = "",
	val port: String = CameraSource.DEFAULT_RTSP_PORT.toString(),
	val username: String = "",
	val password: String = "",
	val lens: Lens = Lens.Wide,
	val streamUrl: String = "",
	val subStreamUrl: String = "",
) {
	companion object {
		fun from(camera: Camera): CameraForm = when (val source = camera.source) {
			is CameraSource.TapoRtsp -> CameraForm(
				kind = CameraKind.TapoRtsp,
				name = camera.name,
				host = source.host,
				port = source.port.toString(),
				username = source.username,
				password = source.password,
				lens = source.lens,
			)
			is CameraSource.BridgeStream -> CameraForm(
				kind = CameraKind.BridgeStream,
				name = camera.name,
				streamUrl = source.url,
				subStreamUrl = source.subStreamUrl.orEmpty(),
			)
		}
	}
}
