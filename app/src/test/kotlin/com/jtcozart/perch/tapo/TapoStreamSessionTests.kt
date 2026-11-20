package com.jtcozart.perch.tapo

import com.jtcozart.perch.camera.StreamQuality
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class TapoStreamSessionTests : FunSpec({
	val segments = listOf(ByteArray(188 * 7) { 0x47 }, ByteArray(188 * 2) { (it % 7).toByte() })

	fun addressOf(camera: FakeTapoCamera, password: String = "tapo-pass1", quality: StreamQuality = StreamQuality.Main, deviceId: String? = null) =
		TapoStreamAddress(host = "127.0.0.1", port = camera.port, tapoPassword = password, quality = quality, deviceId = deviceId)

	fun readAll(session: TapoStreamSession): List<List<Byte>> =
		generateSequence { session.nextVideoSegment() }.map { it.toList() }.toList()

	test("TheCameraIsOpenedWithTheTapoPassword_AndItsVideoComesOutDecrypted") {
		FakeTapoCamera("tapo-pass1", segments).use { camera ->
			TapoStreamSession.open(addressOf(camera)).use { session ->
				readAll(session) shouldBe segments.map { it.toList() }
			}
		}
	}

	test("NewerFirmwareThatHashesWithSha256IsAlsoOpened") {
		FakeTapoCamera("tapo-pass1", segments, hashing = FakeTapoCamera.Hashing.Sha256).use { camera ->
			TapoStreamSession.open(addressOf(camera)).use { session ->
				readAll(session) shouldBe segments.map { it.toList() }
			}
			camera.connections shouldBe 1
		}
	}

	test("FirmwareThatAsksForSha256ButOnlyAcceptsMd5IsOpenedOnASecondTryWithMd5") {
		FakeTapoCamera("tapo-pass1", segments, hashing = FakeTapoCamera.Hashing.Sha256AdvertisedMd5Accepted).use { camera ->
			TapoStreamSession.open(addressOf(camera)).use { session ->
				readAll(session) shouldBe segments.map { it.toList() }
			}
			camera.connections shouldBe 2
		}
	}

	test("AWrongPasswordFailsWithItsOwnError_SoThePlayerStopsRetryingIt") {
		FakeTapoCamera("tapo-pass1", segments).use { camera ->
			val error = shouldThrow<TapoPasswordRefusedException> { TapoStreamSession.open(addressOf(camera, password = "wrong-one")) }
			error.message!! shouldContain "password"
			camera.connections shouldBe 1
		}
	}

	test("AWrongPasswordOnSha256FirmwareIsRefusedAfterTheMd5RetryToo") {
		FakeTapoCamera("tapo-pass1", segments, hashing = FakeTapoCamera.Hashing.Sha256).use { camera ->
			shouldThrow<TapoPasswordRefusedException> { TapoStreamSession.open(addressOf(camera, password = "wrong-one")) }
			camera.connections shouldBe 2
		}
	}

	test("TheWallTileAsksTheCameraForItsVgaStream") {
		FakeTapoCamera("tapo-pass1", segments).use { camera ->
			TapoStreamSession.open(addressOf(camera, quality = StreamQuality.Sub)).use { readAll(it) }
			camera.previewRequest!! shouldContain "\"resolutions\":[\"VGA\"]"
		}
	}

	test("ACameraBehindAHubIsAskedForByItsDeviceId") {
		FakeTapoCamera("tapo-pass1", segments).use { camera ->
			TapoStreamSession.open(addressOf(camera, deviceId = "80223AB1")).use { readAll(it) }
			camera.requestPath shouldBe "/stream?deviceId=80223AB1"
		}
	}
})
