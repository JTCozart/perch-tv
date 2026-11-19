package com.jtcozart.perch.camera

object CameraFormValidator {
	private val tapoCredentialLength = 6..32
	private val portRange = 1..65535
	private val streamSchemes = listOf("rtsp://", "rtsps://", "http://", "https://")
	private val hostPattern = Regex("^[A-Za-z0-9.:\\-]+$")

	fun problemsWith(form: CameraForm): List<FormProblem> = buildList {
		if (form.name.isBlank()) add(FormProblem.NameMissing)
		when (form.kind) {
			CameraKind.TapoRtsp -> addAll(tapoProblems(form))
			CameraKind.BridgeStream -> addAll(bridgeProblems(form))
		}
	}

	/** Call only for a form with no [problemsWith]. */
	fun toCamera(form: CameraForm, id: String): Camera {
		val source = when (form.kind) {
			CameraKind.TapoRtsp -> CameraSource.TapoRtsp(
				host = form.host.trim(),
				port = form.port.trim().toInt(),
				username = form.username,
				password = form.password,
				lens = form.lens,
			)
			CameraKind.BridgeStream -> CameraSource.BridgeStream(
				url = form.streamUrl.trim(),
				subStreamUrl = form.subStreamUrl.trim().ifEmpty { null },
			)
		}
		return Camera(id = id, name = form.name.trim(), source = source)
	}

	private fun tapoProblems(form: CameraForm): List<FormProblem> = buildList {
		val host = form.host.trim()
		when {
			host.isEmpty() -> add(FormProblem.HostMissing)
			!hostPattern.matches(host) -> add(FormProblem.HostInvalid)
		}
		if (form.port.trim().toIntOrNull() !in portRange) add(FormProblem.PortInvalid)
		if (form.username.length !in tapoCredentialLength) add(FormProblem.UsernameLength)
		if (form.password.length !in tapoCredentialLength) add(FormProblem.PasswordLength)
	}

	private fun bridgeProblems(form: CameraForm): List<FormProblem> = buildList {
		if (!isStreamUrl(form.streamUrl)) add(FormProblem.StreamUrlInvalid)
		if (form.subStreamUrl.isNotBlank() && !isStreamUrl(form.subStreamUrl)) add(FormProblem.SubStreamUrlInvalid)
	}

	private fun isStreamUrl(text: String): Boolean {
		val url = text.trim()
		return streamSchemes.any { url.startsWith(it, ignoreCase = true) && url.length > it.length } && url.none { it.isWhitespace() }
	}
}
