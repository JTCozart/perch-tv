package com.jtcozart.perch.tapo

import java.io.InputStream

/**
 * Splits the camera's endless `multipart/mixed` reply into parts. Video parts carry a Content-Length and are read by
 * it, because encrypted bytes can contain anything, including a line that looks like a boundary.
 */
class MultipartPartReader(private val input: InputStream, private val boundaryMarker: String = DEVICE_BOUNDARY) {
	private var isAtPartHeaders = false

	fun next(): MultipartPart? {
		if (!isAtPartHeaders && !skipToBoundary()) return null
		isAtPartHeaders = false

		val headers = StreamLines.headers(input) ?: return null
		val contentType = headers["content-type"]
		val length = headers["content-length"]?.toIntOrNull()
		if (length != null) {
			if (length !in 0..MAX_PART_SIZE) throw TapoStreamException("camera sent a part of $length bytes")
			return StreamLines.exactly(input, length)?.let { MultipartPart(contentType, it) }
		}
		return MultipartPart(contentType, readUntilBoundary())
	}

	private fun skipToBoundary(): Boolean {
		while (true) {
			val line = StreamLines.next(input) ?: return false
			if (isBoundary(line)) return true
		}
	}

	private fun readUntilBoundary(): ByteArray {
		val lines = mutableListOf<String>()
		while (true) {
			val line = StreamLines.next(input) ?: break
			if (isBoundary(line)) {
				isAtPartHeaders = true
				break
			}
			lines += line
		}
		return lines.joinToString("\r\n").toByteArray(Charsets.ISO_8859_1)
	}

	private fun isBoundary(line: String): Boolean = line.startsWith("--") && line.contains(boundaryMarker)

	companion object {
		const val DEVICE_BOUNDARY = "device-stream-boundary"
		private const val MAX_PART_SIZE = 8 * 1024 * 1024
	}
}
