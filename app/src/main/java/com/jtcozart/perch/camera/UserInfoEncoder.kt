package com.jtcozart.perch.camera

/**
 * Percent-encodes a username or password for the user-info part of a URL. URLEncoder is not used because it is
 * for form bodies and turns a space into '+', which an RTSP client would send as a literal plus.
 */
object UserInfoEncoder {
	private const val UNRESERVED_SYMBOLS = "-._~"

	fun encode(text: String): String = buildString {
		for (byte in text.toByteArray(Charsets.UTF_8)) {
			val char = (byte.toInt() and 0xFF).toChar()
			if (isUnreserved(char)) append(char) else append("%%%02X".format(byte.toInt() and 0xFF))
		}
	}

	private fun isUnreserved(char: Char): Boolean =
		char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in UNRESERVED_SYMBOLS
}
