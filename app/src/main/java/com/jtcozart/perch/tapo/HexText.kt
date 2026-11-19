package com.jtcozart.perch.tapo

import java.security.MessageDigest

internal object HexText {
	fun lower(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

	fun upper(bytes: ByteArray): String = bytes.joinToString("") { "%02X".format(it) }

	fun md5(text: String): ByteArray = MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
}
