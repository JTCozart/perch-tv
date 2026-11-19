package com.jtcozart.perch.wall

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class WallLayoutPlannerTests : FunSpec({
	data class Shape(val cameraCount: Int, val columns: Int, val rows: Int)

	context("TheGridGrowsToTheSmallestShapeThatFitsEveryCamera") {
		withData(
			nameFn = { "${it.cameraCount} cameras fill ${it.columns} by ${it.rows}" },
			Shape(1, 1, 1),
			Shape(2, 2, 1),
			Shape(3, 2, 2),
			Shape(4, 2, 2),
			Shape(5, 3, 2),
			Shape(6, 3, 2),
			Shape(7, 3, 3),
			Shape(9, 3, 3),
			Shape(10, 4, 3),
			Shape(16, 4, 4),
		) { shape ->
			val layout = WallLayoutPlanner.plan(cameraCount = shape.cameraCount, liveTileLimit = 16)
			layout.columns shouldBe shape.columns
			layout.rows shouldBe shape.rows
		}
	}

	test("NoCamerasMeansAnEmptyWall") {
		WallLayoutPlanner.plan(cameraCount = 0, liveTileLimit = 4) shouldBe WallLayout(columns = 0, rows = 0, liveTiles = 0)
	}

	test("OnlyAsManyTilesPlayLiveAsTheDecoderCanRun_TheRestWaitToBeSelected") {
		WallLayoutPlanner.plan(cameraCount = 6, liveTileLimit = 4).liveTiles shouldBe 4
	}

	test("EveryTilePlaysWhenTheDecoderHasRoom") {
		WallLayoutPlanner.plan(cameraCount = 3, liveTileLimit = 8).liveTiles shouldBe 3
	}

	test("MoreCamerasThanOnePageHoldsArePagedRatherThanShrunk") {
		val layout = WallLayoutPlanner.plan(cameraCount = 20, liveTileLimit = 16)
		layout.columns shouldBe 4
		layout.rows shouldBe 4
		WallLayoutPlanner.pageCount(cameraCount = 20) shouldBe 2
	}
})
