package com.jtcozart.perch.update

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AppVersionTests : FunSpec({
	test("ATagWithALeadingV_ParsesLikeTheBareVersion") {
		AppVersion.parse("v0.2.0") shouldBe AppVersion(0, 2, 0, null)
		AppVersion.parse("0.2.0") shouldBe AppVersion(0, 2, 0, null)
	}

	test("APreReleaseTagKeepsItsLabel") {
		AppVersion.parse("v0.3.0-rc.1") shouldBe AppVersion(0, 3, 0, "rc.1")
	}

	test("TextThatIsNotAVersionIsRefused") {
		AppVersion.parse("latest") shouldBe null
		AppVersion.parse("v1.2") shouldBe null
		AppVersion.parse("") shouldBe null
	}

	test("ANewerNumberWins_ComparedPartByPart") {
		(AppVersion.parse("0.10.0")!! > AppVersion.parse("0.9.9")!!) shouldBe true
		(AppVersion.parse("1.0.0")!! > AppVersion.parse("0.99.99")!!) shouldBe true
	}

	test("AFinalReleaseIsNewerThanItsOwnPreRelease") {
		(AppVersion.parse("0.3.0")!! > AppVersion.parse("0.3.0-rc.2")!!) shouldBe true
		(AppVersion.parse("0.3.0-rc.2")!! > AppVersion.parse("0.3.0-rc.1")!!) shouldBe true
	}

	test("ABuildFromSourceIsADevelopmentBuild") {
		AppVersion.parse("0.1.0-dev")!!.isDevelopment shouldBe true
		AppVersion.parse("0.2.0")!!.isDevelopment shouldBe false
		AppVersion.parse("0.2.0-rc.1")!!.isDevelopment shouldBe false
	}

	test("ItPrintsAsTheVersionName") {
		AppVersion.parse("v0.3.0-rc.1").toString() shouldBe "0.3.0-rc.1"
	}
})
