package com.jtcozart.perch.tapo

import java.security.MessageDigest

/**
 * The camera never sees the Tapo app password itself: it signs in user `admin` with the uppercase hex digest of it.
 * Older firmware uses MD5; firmware that adds `encrypt_type="3"` to its header uses SHA-256.
 */
object TapoPasswordHash {
	private const val SHA256_ENCRYPT_TYPE = "3"

	fun of(tapoPassword: String, cameraHeader: String): String {
		val usesSha256 = HeaderParameters.valueOf(cameraHeader, "encrypt_type") == SHA256_ENCRYPT_TYPE
		val digest = MessageDigest.getInstance(if (usesSha256) "SHA-256" else "MD5")
		return HexText.upper(digest.digest(tapoPassword.toByteArray(Charsets.UTF_8)))
	}
}
