package com.jtcozart.perch.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-GCM with a key that never leaves the Android Keystore, so the saved camera passwords are unreadable
 * without this app on this device. Output is the 12-byte IV followed by the ciphertext.
 */
class KeystoreCipher(private val keyAlias: String = DEFAULT_KEY_ALIAS) {
	fun encrypt(plain: ByteArray): ByteArray {
		val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
		return cipher.iv + cipher.doFinal(plain)
	}

	fun decrypt(sealed: ByteArray): ByteArray {
		val iv = sealed.copyOfRange(0, IV_LENGTH_BYTES)
		val cipher = Cipher.getInstance(TRANSFORMATION).apply {
			init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
		}
		return cipher.doFinal(sealed, IV_LENGTH_BYTES, sealed.size - IV_LENGTH_BYTES)
	}

	private fun secretKey(): SecretKey {
		val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
		(keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

		val spec = KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
			.setBlockModes(KeyProperties.BLOCK_MODE_GCM)
			.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
			.setKeySize(KEY_SIZE_BITS)
			.build()
		return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
			init(spec)
			generateKey()
		}
	}

	private companion object {
		const val ANDROID_KEYSTORE = "AndroidKeyStore"
		const val DEFAULT_KEY_ALIAS = "perch-cameras"
		const val TRANSFORMATION = "AES/GCM/NoPadding"
		const val IV_LENGTH_BYTES = 12
		const val TAG_LENGTH_BITS = 128
		const val KEY_SIZE_BITS = 256
	}
}
