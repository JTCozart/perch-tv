package com.jtcozart.perch.tapo

import com.jtcozart.perch.camera.StreamQuality
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Runs Perch's own client against a real camera. Skipped unless the person running it sets PERCH_LIVE_TAPO_HOST and
 * PERCH_LIVE_TAPO_PASSWORD in their own shell; the password is never printed.
 */
class LiveTapoCameraTests : FunSpec({
	val host = System.getenv("PERCH_LIVE_TAPO_HOST")
	val password = System.getenv("PERCH_LIVE_TAPO_PASSWORD")
	val isConfigured = !host.isNullOrBlank() && !password.isNullOrEmpty()

	StreamQuality.entries.forEach { quality ->
		test("ARealCameraStreamsDecryptedMpegTs_$quality").config(enabled = isConfigured) {
			val address = TapoStreamAddress.parse(TapoStreamAddress(host!!, TapoStreamSession.DEFAULT_PORT, password!!, quality).toUrl())
			TapoStreamSession.open(address).use { session ->
				val segments = List(SEGMENTS_TO_READ) { session.nextVideoSegment() }
				println("live $quality: ${segments.count { it != null }} segments, ${segments.sumOf { it?.size ?: 0 }} bytes")
				segments.all { it != null && it.first() == MPEG_TS_SYNC_BYTE } shouldBe true
			}
		}
	}
}) {
	private companion object {
		const val SEGMENTS_TO_READ = 20
		const val MPEG_TS_SYNC_BYTE: Byte = 0x47
	}
}
