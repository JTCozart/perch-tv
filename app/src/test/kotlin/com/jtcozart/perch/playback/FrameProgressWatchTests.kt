package com.jtcozart.perch.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FrameProgressWatchTests : FunSpec({
	val timeoutMs = 20_000L

	test("AStreamWhoseFramesKeepComingNeverStalls") {
		val watch = FrameProgressWatch(timeoutMs, startedAtMs = 0)
		(1..10).forEach { second -> watch.isStalled(renderedFrames = second * 15L, nowMs = second * 5_000L) shouldBe false }
	}

	test("NoNewFrameForLongerThanTheTimeoutIsAStall") {
		val watch = FrameProgressWatch(timeoutMs, startedAtMs = 0)
		watch.isStalled(renderedFrames = 100, nowMs = 5_000) shouldBe false
		watch.isStalled(renderedFrames = 100, nowMs = 24_000) shouldBe false
		watch.isStalled(renderedFrames = 100, nowMs = 25_001) shouldBe true
	}

	test("AStreamThatNeverShowsAFrameStallsOnceTheTimeoutHasPassedSinceTheStart") {
		val watch = FrameProgressWatch(timeoutMs, startedAtMs = 1_000)
		watch.isStalled(renderedFrames = null, nowMs = 20_000) shouldBe false
		watch.isStalled(renderedFrames = null, nowMs = 21_001) shouldBe true
	}

	test("FramesArrivingAgainAfterAQuietSpellRestartTheClock") {
		val watch = FrameProgressWatch(timeoutMs, startedAtMs = 0)
		watch.isStalled(renderedFrames = 10, nowMs = 1_000) shouldBe false
		watch.isStalled(renderedFrames = 10, nowMs = 19_000) shouldBe false
		watch.isStalled(renderedFrames = 11, nowMs = 20_000) shouldBe false
		watch.isStalled(renderedFrames = 11, nowMs = 39_000) shouldBe false
	}
})
