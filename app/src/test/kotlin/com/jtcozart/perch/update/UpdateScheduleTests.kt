package com.jtcozart.perch.update

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UpdateScheduleTests : FunSpec({
	fun version(text: String) = AppVersion.parse(text)!!

	test("ABuildFromSourceNeverChecksOnLaunch") {
		UpdateSchedule.checksOnLaunch(version("0.1.0-dev")) shouldBe false
		UpdateSchedule.checksOnLaunch(version("0.2.0")) shouldBe true
	}

	test("OnlyAStrictlyNewerReleaseIsOffered") {
		UpdateSchedule.shouldOffer(latest = version("0.3.0"), installed = version("0.2.0")) shouldBe true
		UpdateSchedule.shouldOffer(latest = version("0.2.0"), installed = version("0.2.0")) shouldBe false
		UpdateSchedule.shouldOffer(latest = version("0.1.0"), installed = version("0.2.0")) shouldBe false
	}
})
