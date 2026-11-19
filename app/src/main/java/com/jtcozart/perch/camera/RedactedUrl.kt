package com.jtcozart.perch.camera

/** Hides the credentials in a stream URL so it can be logged or shown on screen. */
object RedactedUrl {
	private const val SCHEME_SEPARATOR = "://"

	fun of(url: String): String {
		val authorityStart = url.indexOf(SCHEME_SEPARATOR).takeIf { it >= 0 }?.plus(SCHEME_SEPARATOR.length) ?: return url
		val authorityEnd = url.indexOfAny(charArrayOf('/', '?', '#'), authorityStart).takeIf { it >= 0 } ?: url.length
		val credentialsEnd = url.lastIndexOf('@', authorityEnd - 1)
		if (credentialsEnd < authorityStart) return url
		return url.substring(0, authorityStart) + "***" + url.substring(credentialsEnd)
	}
}
