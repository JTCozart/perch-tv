package com.jtcozart.perch.tapo

import com.jtcozart.perch.camera.RedactedUrl
import com.jtcozart.perch.camera.StreamQuality
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

class TapoStreamAddressTests : FunSpec({
	test("AnAddressSurvivesTheTripThroughItsUrl") {
		val address = TapoStreamAddress(host = "192.168.1.50", port = 8800, tapoPassword = "p@ss:w/rd#1 x", quality = StreamQuality.Sub)

		TapoStreamAddress.parse(address.toUrl()) shouldBe address
	}

	test("AHubDeviceIdIsKept") {
		val address = TapoStreamAddress(host = "192.168.1.9", port = 8800, tapoPassword = "secret", quality = StreamQuality.Main, deviceId = "80223AB1")

		TapoStreamAddress.parse(address.toUrl()) shouldBe address
	}

	test("TheUrlUsesTheTapoScheme_SoThePlayerKnowsToUseTheTapoProtocol") {
		TapoStreamAddress("10.0.0.4", 8800, "secret", StreamQuality.Main).toUrl() shouldBe "tapo://secret@10.0.0.4:8800/?quality=main"
	}

	test("RedactingTheUrlHidesThePassword") {
		RedactedUrl.of(TapoStreamAddress("10.0.0.4", 8800, "hunter22", StreamQuality.Main).toUrl()) shouldNotContain "hunter22"
	}

	test("AMalformedAddressFailsWithoutRepeatingThePassword_SoNoLogCanLeakIt") {
		val error = shouldThrow<TapoStreamException> { TapoStreamAddress.parse("tapo://hunter22@[192.168.1.50:8800]:8800/?quality=main") }
		error.message.orEmpty() shouldNotContain "hunter22"
		error.cause shouldBe null
	}

	test("TheMainStreamAsksForHdAndTheTileStreamForVga") {
		TapoStreamAddress("10.0.0.4", 8800, "s", StreamQuality.Main).resolution shouldBe "HD"
		TapoStreamAddress("10.0.0.4", 8800, "s", StreamQuality.Sub).resolution shouldBe "VGA"
	}

	test("TheRequestPathAddsTheHubDeviceIdOnlyWhenThereIsOne") {
		TapoStreamAddress("10.0.0.4", 8800, "s", StreamQuality.Main).requestPath shouldBe "/stream"
		TapoStreamAddress("10.0.0.4", 8800, "s", StreamQuality.Main, deviceId = "AB12").requestPath shouldBe "/stream?deviceId=AB12"
	}
})
