package com.jtcozart.perch.camera

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class CameraListCodecTests : FunSpec({
	val cameras = listOf(
		Camera(id = "a", name = "Driveway", source = CameraSource.TapoRtsp(host = "192.168.1.40", username = "camuser", password = "secret99")),
		Camera(id = "b", name = "Garden", source = CameraSource.BridgeStream(url = "rtsp://10.0.0.5:8554/garden")),
		Camera(id = "c", name = "Porch", source = CameraSource.TapoLocal(host = "192.168.1.50", tapoPassword = "tapo-pass1", deviceId = "80223AB1")),
	)

	test("CamerasSurviveARoundTrip") {
		CameraListCodec.decode(CameraListCodec.encode(cameras)) shouldBe cameras
	}

	test("NothingSavedYetMeansNoCameras") {
		CameraListCodec.decode("").shouldBeEmpty()
	}
})
