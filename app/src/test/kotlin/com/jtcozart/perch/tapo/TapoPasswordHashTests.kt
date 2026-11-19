package com.jtcozart.perch.tapo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TapoPasswordHashTests : FunSpec({
	test("OlderFirmwareGetsTheUppercaseMd5OfTheTapoPassword") {
		val challenge = """Digest realm="TP-LINK IP-Camera", nonce="abc", qop="auth""""
		TapoPasswordHash.of("password", challenge) shouldBe "5F4DCC3B5AA765D61D8327DEB882CF99"
	}

	test("FirmwareThatAsksForEncryptType3GetsTheUppercaseSha256") {
		val challenge = """Digest realm="TP-LINK IP-Camera", nonce="abc", qop="auth", encrypt_type="3""""
		TapoPasswordHash.of("password", challenge) shouldBe "5E884898DA28047151D0E56F8DC6292773603D0D6AABBDD62A11EF721D1542D8"
	}
})
