package com.jtcozart.perch.tapo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TapoPasswordHashTests : FunSpec({
	test("OlderFirmwareAsksForMd5") {
		val challenge = """Digest realm="TP-LINK IP-Camera", nonce="abc", qop="auth""""
		TapoPasswordHash.preferredBy(challenge) shouldBe TapoPasswordHash.Method.Md5
	}

	test("FirmwareThatSaysEncryptType3AsksForSha256") {
		val challenge = """Digest realm="TP-LINK IP-Camera",algorithm="MD5",encrypt_type="3",qop="auth",nonce="abc""""
		TapoPasswordHash.preferredBy(challenge) shouldBe TapoPasswordHash.Method.Sha256
	}

	test("TheMd5HashIsUppercaseHex") {
		TapoPasswordHash.of("password", TapoPasswordHash.Method.Md5) shouldBe "5F4DCC3B5AA765D61D8327DEB882CF99"
	}

	test("TheSha256HashIsUppercaseHex") {
		TapoPasswordHash.of("password", TapoPasswordHash.Method.Sha256) shouldBe
			"5E884898DA28047151D0E56F8DC6292773603D0D6AABBDD62A11EF721D1542D8"
	}
})
