package com.jtcozart.perch.tapo

import java.security.MessageDigest

/**
 * The camera never sees the Tapo app password itself: it signs in user `admin` with the uppercase hex digest of it.
 * Older firmware uses MD5; firmware that adds `encrypt_type="3"` to its challenge asks for SHA-256.
 */
object TapoPasswordHash {
	enum class Method(val algorithm: String) { Md5("MD5"), Sha256("SHA-256") }

	private const val SHA256_ENCRYPT_TYPE = "3"

	fun preferredBy(cameraHeader: String): Method =
		if (HeaderParameters.valueOf(cameraHeader, "encrypt_type") == SHA256_ENCRYPT_TYPE) Method.Sha256 else Method.Md5

	fun of(tapoPassword: String, method: Method): String =
		HexText.upper(MessageDigest.getInstance(method.algorithm).digest(tapoPassword.toByteArray(Charsets.UTF_8)))
}
