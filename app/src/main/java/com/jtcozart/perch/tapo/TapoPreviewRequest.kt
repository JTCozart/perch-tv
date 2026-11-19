package com.jtcozart.perch.tapo

/** The one request the client sends after signing in: start a live preview at the chosen resolution. */
object TapoPreviewRequest {
	private const val CLIENT_BOUNDARY_LINE = "----client-stream-boundary--"
	private const val MAIN_CHANNEL = 0

	fun json(address: TapoStreamAddress): String =
		"""{"params":{"preview":{"audio":["default"],"channels":[$MAIN_CHANNEL],"resolutions":["${address.resolution}"]},"method":"get"},"seq":1,"type":"request"}"""

	fun part(address: TapoStreamAddress): ByteArray {
		val body = json(address).toByteArray(Charsets.UTF_8)
		val head = "$CLIENT_BOUNDARY_LINE\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\n\r\n"
		return head.toByteArray(Charsets.ISO_8859_1) + body + "\r\n".toByteArray(Charsets.ISO_8859_1)
	}
}
