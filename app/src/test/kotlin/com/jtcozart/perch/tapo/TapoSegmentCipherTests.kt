package com.jtcozart.perch.tapo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TapoSegmentCipherTests : FunSpec({
	val hashedPassword = "5F4DCC3B5AA765D61D8327DEB882CF99"
	val keyExchange = """username="admin" padding="0" algorithm="MD5" cipher="AES_128_CBC" nonce="7C3A1F02""""

	test("ASegmentEncryptedTheWayTheCameraDoesItDecryptsToTheOriginalBytes") {
		val segment = ByteArray(188 * 3) { (it % 251).toByte() }
		val cipher = TapoSegmentCipher.from(keyExchange, hashedPassword)

		cipher.decrypt(FakeTapoCamera.encrypt(segment, "7C3A1F02", hashedPassword)).toList() shouldBe segment.toList()
	}

	test("EverySegmentStartsFromTheSameIv_SoTheyDecryptIndependently") {
		val first = ByteArray(100) { 1 }
		val second = ByteArray(37) { 2 }
		val cipher = TapoSegmentCipher.from(keyExchange, hashedPassword)

		cipher.decrypt(FakeTapoCamera.encrypt(first, "7C3A1F02", hashedPassword)).toList() shouldBe first.toList()
		cipher.decrypt(FakeTapoCamera.encrypt(second, "7C3A1F02", hashedPassword)).toList() shouldBe second.toList()
	}
})
