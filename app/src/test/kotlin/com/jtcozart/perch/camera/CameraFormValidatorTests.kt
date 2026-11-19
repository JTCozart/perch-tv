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

	fun tapoLocalForm(host: String = "192.168.1.50", password: String = "tapo-pass1", deviceId: String = "") =
		CameraForm(kind = CameraKind.TapoLocal, name = "Porch", host = host, password = password, hubDeviceId = deviceId)

	test("ACompleteTapoAppPasswordFormHasNoProblems") {
		CameraFormValidator.problemsWith(tapoLocalForm()).shouldBeEmpty()
	}

	test("ATapoAppPasswordCameraNeedsAnAddressAndThePassword") {
		CameraFormValidator.problemsWith(tapoLocalForm(host = "", password = "")) shouldContainExactly
			listOf(FormProblem.HostMissing, FormProblem.TapoPasswordMissing)
	}

	test("AnAddressWithAPortTypedOnIsRefused") {
		CameraFormValidator.problemsWith(tapoLocalForm(host = "192.168.1.50:8800")) shouldContainExactly listOf(FormProblem.HostInvalid)
		CameraFormValidator.problemsWith(tapoForm(host = "192.168.1.40:554")) shouldContainExactly listOf(FormProblem.HostInvalid)
	}

	test("AnIpv6AddressIsStillAccepted") {
		CameraFormValidator.problemsWith(tapoLocalForm(host = "fd00::40")).shouldBeEmpty()
	}

	test("AHubDeviceIdIsLettersAndDigitsOnly") {
		CameraFormValidator.problemsWith(tapoLocalForm(deviceId = "80 22/AB")) shouldContainExactly listOf(FormProblem.DeviceIdInvalid)
	}

	test("ATapoAppPasswordFormBecomesACameraOnPort8800_WithNoDeviceIdWhenLeftEmpty") {
		CameraFormValidator.toCamera(tapoLocalForm(), id = "p") shouldBe Camera(
			id = "p",
			name = "Porch",
			source = CameraSource.TapoLocal(host = "192.168.1.50", tapoPassword = "tapo-pass1"),
		)
	}

	test("ATapoAppPasswordCameraRoundTripsThroughTheForm") {
		val camera = Camera(id = "p", name = "Porch", source = CameraSource.TapoLocal(host = "192.168.1.50", tapoPassword = "tapo-pass1", deviceId = "80223AB1"))
		CameraFormValidator.toCamera(CameraForm.from(camera), id = "p") shouldBe camera
	}
})
