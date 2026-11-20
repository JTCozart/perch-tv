package com.jtcozart.perch.camera

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CameraFormTests : FunSpec({
	test("ACopyKeepsEverySettingIncludingThePassword_SoOnlyTheAddressNeedsChanging") {
		val solar = Camera(id = "a", name = "Back yard", source = CameraSource.TapoLocal(host = "10.0.0.177", tapoPassword = "secret"))

		val copy = CameraForm.copyOf(solar)

		copy shouldBe CameraForm.from(solar).copy(name = "Back yard copy")
	}

	test("ACopyOfAnRtspCameraKeepsItsAccountAndLens") {
		val wired = Camera(
			id = "b",
			name = "Front door",
			source = CameraSource.TapoRtsp(host = "10.0.0.103", username = "cam", password = "pw", lens = Lens.Telephoto),
		)

		val copy = CameraForm.copyOf(wired)

		copy.username shouldBe "cam"
		copy.password shouldBe "pw"
		copy.lens shouldBe Lens.Telephoto
		copy.name shouldBe "Front door copy"
	}
})
