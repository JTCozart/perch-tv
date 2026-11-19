package com.jtcozart.perch.camera

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class CameraFormValidatorTests : FunSpec({
	fun tapoForm(
		name: String = "Driveway",
		host: String = "192.168.1.40",
		port: String = "554",
		username: String = "camuser",
		password: String = "secret99",
	) = CameraForm(kind = CameraKind.TapoRtsp, name = name, host = host, port = port, username = username, password = password)

	test("ACompleteTapoFormHasNoProblems") {
		CameraFormValidator.problemsWith(tapoForm()).shouldBeEmpty()
	}

	test("EveryCameraNeedsAName") {
		CameraFormValidator.problemsWith(tapoForm(name = "  ")) shouldContainExactly listOf(FormProblem.NameMissing)
	}

	test("ATapoCameraNeedsAnAddress") {
		CameraFormValidator.problemsWith(tapoForm(host = "")) shouldContainExactly listOf(FormProblem.HostMissing)
	}

	test("AnAddressWithSpacesOrASchemeIsRefused") {
		CameraFormValidator.problemsWith(tapoForm(host = "rtsp://192.168.1.40")) shouldContainExactly listOf(FormProblem.HostInvalid)
		CameraFormValidator.problemsWith(tapoForm(host = "192.168 .1.40")) shouldContainExactly listOf(FormProblem.HostInvalid)
	}

	test("ThePortMustBeANumberFrom1To65535") {
		CameraFormValidator.problemsWith(tapoForm(port = "0")) shouldContainExactly listOf(FormProblem.PortInvalid)
		CameraFormValidator.problemsWith(tapoForm(port = "70000")) shouldContainExactly listOf(FormProblem.PortInvalid)
		CameraFormValidator.problemsWith(tapoForm(port = "abc")) shouldContainExactly listOf(FormProblem.PortInvalid)
	}

	test("TapoCameraAccountsAre6To32Characters_SoShorterOrLongerCredentialsCannotBeRight") {
		CameraFormValidator.problemsWith(tapoForm(username = "cam")) shouldContainExactly listOf(FormProblem.UsernameLength)
		CameraFormValidator.problemsWith(tapoForm(password = "x".repeat(33))) shouldContainExactly listOf(FormProblem.PasswordLength)
	}

	test("ABridgeNeedsAnRtspOrHttpUrl") {
		val valid = CameraForm(kind = CameraKind.BridgeStream, name = "Garden", streamUrl = "rtsp://10.0.0.5:8554/garden")
		CameraFormValidator.problemsWith(valid).shouldBeEmpty()

		val wrongScheme = valid.copy(streamUrl = "ftp://10.0.0.5/garden")
		CameraFormValidator.problemsWith(wrongScheme) shouldContainExactly listOf(FormProblem.StreamUrlInvalid)
	}

	test("ABridgeSubStreamIsOptionalButMustBeAUrlWhenGiven") {
		val form = CameraForm(kind = CameraKind.BridgeStream, name = "Garden", streamUrl = "rtsp://h/garden", subStreamUrl = "garden_sub")
		CameraFormValidator.problemsWith(form) shouldContainExactly listOf(FormProblem.SubStreamUrlInvalid)
	}

	test("AValidFormBecomesTheCameraItDescribes_WithStrayWhitespaceTrimmed") {
		val camera = CameraFormValidator.toCamera(tapoForm(name = " Driveway ", host = " 192.168.1.40 "), id = "a")
		camera shouldBe Camera(
			id = "a",
			name = "Driveway",
			source = CameraSource.TapoRtsp(host = "192.168.1.40", port = 554, username = "camuser", password = "secret99"),
		)
	}

	test("AFormMadeFromACameraFillsEveryField_SoEditingStartsFromWhatIsSaved") {
		val camera = Camera(id = "a", name = "Driveway", source = CameraSource.TapoRtsp(host = "192.168.1.40", port = 8554, username = "camuser", password = "secret99", lens = Lens.Telephoto))
		CameraForm.from(camera) shouldBe tapoForm(port = "8554").copy(lens = Lens.Telephoto)
	}
})
