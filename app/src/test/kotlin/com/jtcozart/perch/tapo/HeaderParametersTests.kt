package com.jtcozart.perch.tapo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HeaderParametersTests : FunSpec({
	val header = """Digest realm="TP-LINK IP-Camera", nonce="5a1b", qop="auth", opaque="xyz""""

	test("AQuotedParameterIsReadByName") {
		HeaderParameters.valueOf(header, "nonce") shouldBe "5a1b"
		HeaderParameters.valueOf(header, "realm") shouldBe "TP-LINK IP-Camera"
	}

	test("AMissingParameterIsNull") {
		HeaderParameters.valueOf(header, "algorithm") shouldBe null
	}

	test("ANameThatEndsAnotherNameDoesNotMatchIt") {
		HeaderParameters.valueOf("""cnonce="111", nonce="222"""", "nonce") shouldBe "222"
	}
})
