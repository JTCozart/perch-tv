package com.jtcozart.perch.tapo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class DigestAuthorizationTests : FunSpec({
	// The worked example from RFC 2617 section 3.5, so the response hash is checked against a published value
	val rfcChallenge = """Digest realm="testrealm@host.com", qop="auth,auth-int", nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093", opaque="5ccc069c403ebaf9f0171e9517f40e41""""

	fun rfcHeader(challenge: String = rfcChallenge) = DigestAuthorization.header(
		challenge = challenge,
		username = "Mufasa",
		password = "Circle Of Life",
		method = "GET",
		uri = "/dir/index.html",
		clientNonce = "0a4f113b",
	)

	test("TheResponseMatchesTheRfcExample") {
		rfcHeader() shouldContain """response="6629fae49393a05397450978507c4ef1""""
	}

	test("TheHeaderCarriesTheFieldsTheCameraChecks") {
		val header = rfcHeader()
		header shouldContain """Digest username="Mufasa""""
		header shouldContain """realm="testrealm@host.com""""
		header shouldContain """uri="/dir/index.html""""
		header shouldContain "nc=00000001"
		header shouldContain """cnonce="0a4f113b""""
	}

	test("TheOpaqueValueIsEchoedWhenTheCameraSendsOne") {
		rfcHeader() shouldContain """opaque="5ccc069c403ebaf9f0171e9517f40e41""""
	}

	test("NoOpaqueIsSentWhenTheCameraSentNone") {
		val challenge = """Digest realm="r", qop="auth", nonce="n""""
		rfcHeader(challenge) shouldNotContain "opaque"
	}
})
