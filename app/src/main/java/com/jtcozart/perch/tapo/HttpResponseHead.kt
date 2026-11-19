package com.jtcozart.perch.tapo

import java.io.InputStream

/** The status line and headers of one HTTP response, read off a connection that stays open afterwards. */
class HttpResponseHead(val statusCode: Int, val headers: Map<String, String>) {
	fun discardBody(input: InputStream) {
		val length = headers["content-length"]?.toIntOrNull() ?: return
		if (length > 0) StreamLines.exactly(input, length) ?: throw TapoStreamException("camera closed the connection")
	}

	companion object {
		fun read(input: InputStream): HttpResponseHead {
			val statusLine = StreamLines.next(input) ?: throw TapoStreamException("camera closed the connection")
			val statusCode = statusLine.split(' ').getOrNull(1)?.toIntOrNull() ?: throw TapoStreamException("camera sent an unexpected reply")
			val headers = StreamLines.headers(input) ?: throw TapoStreamException("camera closed the connection")
			return HttpResponseHead(statusCode, headers)
		}
	}
}
