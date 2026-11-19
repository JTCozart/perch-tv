package com.jtcozart.perch.tapo

import com.jtcozart.perch.camera.StreamQuality
import com.jtcozart.perch.camera.UserInfoEncoder
import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder

/**
 * Everything needed to open one Tapo stream, carried to the player as a `tapo://password@host:port/?quality=main`
 * URL so it travels the same path as RTSP and HLS addresses and is redacted the same way.
 */
data class TapoStreamAddress(
	val host: String,
	val port: Int,
	val tapoPassword: String,
	val quality: StreamQuality,
	val deviceId: String? = null,
) {
	/** Tapo names its full stream HD and its light sub stream VGA, whatever the real resolution is. */
	val resolution: String get() = if (quality == StreamQuality.Main) "HD" else "VGA"

	val requestPath: String get() = if (deviceId == null) STREAM_PATH else "$STREAM_PATH?deviceId=$deviceId"

	fun toUrl(): String = buildString {
		append("$SCHEME://${UserInfoEncoder.encode(tapoPassword)}@${hostForUrl()}:$port/?quality=${quality.name.lowercase()}")
		if (deviceId != null) append("&deviceId=${UserInfoEncoder.encode(deviceId)}")
	}

	private fun hostForUrl(): String = if (host.contains(':')) "[$host]" else host

	companion object {
		const val SCHEME = "tapo"
		private const val STREAM_PATH = "/stream"

		/** Fails with a message that never repeats [url], because the URL holds the password. */
		fun parse(url: String): TapoStreamAddress = try {
			parseUnchecked(url)
		} catch (_: URISyntaxException) {
			throw TapoStreamException("the camera address is not valid")
		} catch (_: IllegalArgumentException) {
			throw TapoStreamException("the camera address is not valid")
		} catch (_: NullPointerException) {
			throw TapoStreamException("the camera address is not valid")
		}

		private fun parseUnchecked(url: String): TapoStreamAddress {
			val uri = URI(url)
			if (!uri.scheme.equals(SCHEME, ignoreCase = true)) throw TapoStreamException("not a $SCHEME:// address")
			val query = uri.rawQuery.orEmpty().split('&').filter { it.contains('=') }.associate {
				it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), "UTF-8")
			}
			return TapoStreamAddress(
				host = requireNotNull(uri.host).removePrefix("[").removeSuffix("]"),
				port = uri.port,
				tapoPassword = uri.userInfo.orEmpty(),
				quality = if (query["quality"] == "sub") StreamQuality.Sub else StreamQuality.Main,
				deviceId = query["deviceId"],
			)
		}
	}
}
