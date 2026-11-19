package com.jtcozart.perch.camera

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RedactedUrlTests : FunSpec({
	test("TheCredentialsAreHiddenBeforeAUrlIsLoggedOrShown") {
		RedactedUrl.of("rtsp://camuser:secret99@192.168.1.40:554/stream2") shouldBe "rtsp://***@192.168.1.40:554/stream2"
	}

	test("AUrlWithoutCredentialsIsUnchanged") {
		RedactedUrl.of("http://10.0.0.5:1984/api/stream.m3u8?src=yard") shouldBe "http://10.0.0.5:1984/api/stream.m3u8?src=yard"
	}

	test("AnAtSignInTheQueryIsNotMistakenForCredentials") {
		RedactedUrl.of("http://host/path?mail=a@b") shouldBe "http://host/path?mail=a@b"
	}
})
