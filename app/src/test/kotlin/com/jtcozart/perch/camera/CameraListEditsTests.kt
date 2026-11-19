package com.jtcozart.perch.camera

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CameraListEditsTests : FunSpec({
	fun camera(id: String) = Camera(id = id, name = id, source = CameraSource.BridgeStream(url = "rtsp://h/$id"))
	val wall = listOf(camera("a"), camera("b"), camera("c"))
	fun List<Camera>.ids() = map { it.id }

	test("ANewCameraGoesAtTheEndOfTheWall") {
		CameraListEdits.save(wall, camera("d")).ids() shouldBe listOf("a", "b", "c", "d")
	}

	test("SavingAnExistingCameraReplacesItInPlace") {
		val renamed = camera("b").copy(name = "Porch")
		CameraListEdits.save(wall, renamed) shouldBe listOf(camera("a"), renamed, camera("c"))
	}

	test("DeletingRemovesOnlyThatCamera") {
		CameraListEdits.delete(wall, "b").ids() shouldBe listOf("a", "c")
	}

	test("MovingEarlierSwapsWithTheCameraBefore") {
		CameraListEdits.move(wall, "c", -1).ids() shouldBe listOf("a", "c", "b")
	}

	test("MovingPastEitherEndLeavesTheOrderAlone") {
		CameraListEdits.move(wall, "a", -1).ids() shouldBe listOf("a", "b", "c")
		CameraListEdits.move(wall, "c", 1).ids() shouldBe listOf("a", "b", "c")
	}

	test("MovingAnUnknownCameraChangesNothing") {
		CameraListEdits.move(wall, "z", 1).ids() shouldBe listOf("a", "b", "c")
	}
})
