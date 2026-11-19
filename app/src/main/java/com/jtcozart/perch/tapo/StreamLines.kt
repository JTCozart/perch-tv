package com.jtcozart.perch.tapo

import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Reads header lines and fixed-length bodies byte by byte from one shared stream. The HTTP head and the multipart
 * parts that follow it arrive on the same connection, so nothing may read ahead into a buffer of its own.
 */
internal object StreamLines {
	private const val MAX_LINE_LENGTH = 16 * 1024

	/** The next line without its CRLF, or null at the end of the stream. */
	fun next(input: InputStream): String? {
		val bytes = ByteArrayOutputStream()
		while (true) {
			val next = input.read()
			if (next < 0) return if (bytes.size() == 0) null else bytes.toString(Charsets.ISO_8859_1.name())
			if (next == '\n'.code) return bytes.toString(Charsets.ISO_8859_1.name()).trimEnd('\r')
			if (bytes.size() >= MAX_LINE_LENGTH) throw TapoStreamException("camera sent a header line longer than $MAX_LINE_LENGTH bytes")
			bytes.write(next)
		}
	}

	/** Exactly [count] bytes, or null if the stream ends first. */
	fun exactly(input: InputStream, count: Int): ByteArray? {
		val bytes = ByteArray(count)
		var filled = 0
		while (filled < count) {
			val read = input.read(bytes, filled, count - filled)
			if (read < 0) return null
			filled += read
		}
		return bytes
	}

	/** Header lines up to the blank line, keyed by lowercase name. */
	fun headers(input: InputStream): Map<String, String>? {
		val headers = mutableMapOf<String, String>()
		while (true) {
			val line = next(input) ?: return null
			if (line.isEmpty()) return headers
			val separator = line.indexOf(':')
			if (separator > 0) headers[line.substring(0, separator).trim().lowercase()] = line.substring(separator + 1).trim()
		}
	}
}
