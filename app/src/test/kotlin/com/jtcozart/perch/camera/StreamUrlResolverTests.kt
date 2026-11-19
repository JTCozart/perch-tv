package com.jtcozart.perch.camera

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class StreamUrlResolverTests : FunSpec({
	fun tapo(lens: Lens = Lens.Wide, port: Int = 554, username: String = "camuser", password: String = "secret99") =
		CameraSource.TapoRtsp(host = "192.168.1.40", port = port, username = username, password = password, lens = lens)

	test("TheSubStreamOfAWideLensIsStream2_SoGridTilesStayLight") {
		StreamUrlResolver.resolve(tapo(), StreamQuality.Sub) shouldBe "rtsp://camuser:secret99@192.168.1.40:554/stream2"
	}

	test("TheMainStreamOfAWideLensIsStream1") {
		StreamUrlResolver.resolve(tapo(), StreamQuality.Main) shouldBe "rtsp://camuser:secret99@192.168.1.40:554/stream1"
	}

	test("TheTelephotoLensOfADualLensCameraUsesStreams6And7") {
		StreamUrlResolver.resolve(tapo(Lens.Telephoto), StreamQuality.Main) shouldBe "rtsp://camuser:secret99@192.168.1.40:554/stream6"
		StreamUrlResolver.resolve(tapo(Lens.Telephoto), StreamQuality.Sub) shouldBe "rtsp://camuser:secret99@192.168.1.40:554/stream7"
	}

	test("ACustomPortIsKept") {
		StreamUrlResolver.resolve(tapo(port = 8554), StreamQuality.Sub) shouldBe "rtsp://camuser:secret99@192.168.1.40:8554/stream2"
	}

	test("ReservedCharactersInTheCredentialsArePercentEncoded_SoTheUrlStillParses") {
		val url = StreamUrlResolver.resolve(tapo(username = "front door", password = "p@ss:w/rd#1"), StreamQuality.Sub)
		url shouldBe "rtsp://front%20door:p%40ss%3Aw%2Frd%231@192.168.1.40:554/stream2"
	}

	test("AnIpv6HostIsWrappedInBrackets") {
		val source = CameraSource.TapoRtsp(host = "fd00::40", username = "camuser", password = "secret99")
		StreamUrlResolver.resolve(source, StreamQuality.Sub) shouldBe "rtsp://camuser:secret99@[fd00::40]:554/stream2"
	}

	test("ABridgeStreamIsPlayedAsGivenWhateverTheQuality") {
		val source = CameraSource.BridgeStream(url = "rtsp://10.0.0.5:8554/backyard")
		StreamUrlResolver.resolve(source, StreamQuality.Main) shouldBe "rtsp://10.0.0.5:8554/backyard"
		StreamUrlResolver.resolve(source, StreamQuality.Sub) shouldBe "rtsp://10.0.0.5:8554/backyard"
	}

	test("ABridgeWithASeparateSubStreamUsesItForTiles") {
		val source = CameraSource.BridgeStream(url = "rtsp://10.0.0.5:8554/yard", subStreamUrl = "rtsp://10.0.0.5:8554/yard_sub")
		StreamUrlResolver.resolve(source, StreamQuality.Sub) shouldBe "rtsp://10.0.0.5:8554/yard_sub"
		StreamUrlResolver.resolve(source, StreamQuality.Main) shouldBe "rtsp://10.0.0.5:8554/yard"
	}

	test("ATapoAppPasswordCameraPlaysOverTheTapoProtocol_VgaOnTilesAndHdFullScreen") {
		val source = CameraSource.TapoLocal(host = "192.168.1.50", tapoPassword = "tapo-pass1")
		StreamUrlResolver.resolve(source, StreamQuality.Sub) shouldBe "tapo://tapo-pass1@192.168.1.50:8800/?quality=sub"
		StreamUrlResolver.resolve(source, StreamQuality.Main) shouldBe "tapo://tapo-pass1@192.168.1.50:8800/?quality=main"
	}
})
