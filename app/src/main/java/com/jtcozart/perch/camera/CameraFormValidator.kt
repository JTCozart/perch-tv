package com.jtcozart.perch.camera

object CameraFormValidator {
	private val tapoCredentialLength = 6..32
	private val portRange = 1..65535
	private val deviceIdPattern = Regex("^[A-Za-z0-9]+$")
	private val streamSchemes = listOf("rtsp://", "rtsps://", "http://", "https://")
	private val hostPattern = Regex("^[A-Za-z0-9.:\\-]+$")

	fun problemsWith(form: CameraForm): List<FormProblem> = buildList {
		if (form.name.isBlank()) add(FormProblem.NameMissing)
		when (form.kind) {
			CameraKind.TapoRtsp -> addAll(tapoProblems(form))
			CameraKind.TapoLocal -> addAll(tapoLocalProblems(form))
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
			CameraKind.TapoLocal -> CameraSource.TapoLocal(
				host = form.host.trim(),
				tapoPassword = form.password,
				deviceId = form.hubDeviceId.trim().ifEmpty { null },
			)
			CameraKind.BridgeStream -> CameraSource.BridgeStream(
				url = form.streamUrl.trim(),
				subStreamUrl = form.subStreamUrl.trim().ifEmpty { null },
			)
		}
		return Camera(id = id, name = form.name.trim(), source = source)
	}

	private fun tapoProblems(form: CameraForm): List<FormProblem> = buildList {
		addAll(hostProblems(form))
		if (form.port.trim().toIntOrNull() !in portRange) add(FormProblem.PortInvalid)
		if (form.username.length !in tapoCredentialLength) add(FormProblem.UsernameLength)
		if (form.password.length !in tapoCredentialLength) add(FormProblem.PasswordLength)
	}

	private fun tapoLocalProblems(form: CameraForm): List<FormProblem> = buildList {
		addAll(hostProblems(form))
		if (form.password.isEmpty()) add(FormProblem.TapoPasswordMissing)
		val deviceId = form.hubDeviceId.trim()
		if (deviceId.isNotEmpty() && !deviceIdPattern.matches(deviceId)) add(FormProblem.DeviceIdInvalid)
	}

	private fun hostProblems(form: CameraForm): List<FormProblem> {
		val host = form.host.trim()
		return when {
			host.isEmpty() -> listOf(FormProblem.HostMissing)
			!hostPattern.matches(host) || hasPortTypedOn(host) -> listOf(FormProblem.HostInvalid)
			else -> emptyList()
		}
	}

	/** An IPv6 address has several colons; a single one means a port was typed after an IPv4 address or name. */
	private fun hasPortTypedOn(host: String): Boolean = host.count { it == ':' } == 1

	private fun bridgeProblems(form: CameraForm): List<FormProblem> = buildList {
		if (!isStreamUrl(form.streamUrl)) add(FormProblem.StreamUrlInvalid)
		if (form.subStreamUrl.isNotBlank() && !isStreamUrl(form.subStreamUrl)) add(FormProblem.SubStreamUrlInvalid)
	}

	private fun isStreamUrl(text: String): Boolean {
		val url = text.trim()
		return streamSchemes.any { url.startsWith(it, ignoreCase = true) && url.length > it.length } && url.none { it.isWhitespace() }
	}
}
