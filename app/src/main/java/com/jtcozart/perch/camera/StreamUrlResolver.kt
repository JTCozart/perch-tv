package com.jtcozart.perch.camera

object StreamUrlResolver {
	fun resolve(source: CameraSource, quality: StreamQuality): String = when (source) {
		is CameraSource.TapoRtsp -> tapoUrl(source, quality)
		is CameraSource.BridgeStream -> bridgeUrl(source, quality)
	}

	private fun tapoUrl(source: CameraSource.TapoRtsp, quality: StreamQuality): String {
		val credentials = "${UserInfoEncoder.encode(source.username)}:${UserInfoEncoder.encode(source.password)}"
		return "rtsp://$credentials@${hostForUrl(source.host)}:${source.port}/${TapoStreamPath.of(source.lens, quality)}"
	}

	private fun bridgeUrl(source: CameraSource.BridgeStream, quality: StreamQuality): String =
		if (quality == StreamQuality.Sub && !source.subStreamUrl.isNullOrBlank()) source.subStreamUrl else source.url

	private fun hostForUrl(host: String): String = if (host.contains(':')) "[$host]" else host
}
