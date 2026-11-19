package com.jtcozart.perch.tapo

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Decrypts the MPEG-TS parts a Tapo camera sends. Each part is AES-128-CBC with PKCS#7 padding, and every part starts
 * again from the same IV, so parts decrypt independently. The key is md5(nonce:hash) and the IV md5(admin:nonce),
 * with the nonce from the `Key-Exchange` header of the stream response.
 */
class TapoSegmentCipher private constructor(private val key: SecretKeySpec, private val initialVector: IvParameterSpec) {
	fun decrypt(segment: ByteArray): ByteArray {
		if (segment.isEmpty() || segment.size % AES_BLOCK_SIZE != 0) throw TapoStreamException("camera sent a video part of ${segment.size} bytes, not whole AES blocks")
		val plain = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key, initialVector) }.doFinal(segment)
		val padding = plain.last().toInt() and 0xFF
		if (padding !in 1..AES_BLOCK_SIZE) throw TapoStreamException("video part did not decrypt; the Tapo password may be wrong")
		return plain.copyOf(plain.size - padding)
	}

	companion object {
		private const val AES_BLOCK_SIZE = 16
		private const val TRANSFORMATION = "AES/CBC/NoPadding"
		private const val USERNAME = "admin"

		fun from(keyExchange: String, hashedPassword: String): TapoSegmentCipher {
			val nonce = HeaderParameters.valueOf(keyExchange, "nonce") ?: throw TapoStreamException("camera sent no key exchange nonce")
			return TapoSegmentCipher(
				key = SecretKeySpec(HexText.md5("$nonce:$hashedPassword"), "AES"),
				initialVector = IvParameterSpec(HexText.md5("$USERNAME:$nonce")),
			)
		}
	}
}
