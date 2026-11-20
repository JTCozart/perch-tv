package com.jtcozart.perch.update

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ChecksumListTests : FunSpec({
	test("EachLineMapsAFileNameToItsLowercaseHash") {
		val text = "ABC123  perch-tv-v0.2.0.apk\nDEF456 *perch-tv.apk\n\n"
		ChecksumList.parse(text) shouldBe mapOf("perch-tv-v0.2.0.apk" to "abc123", "perch-tv.apk" to "def456")
	}

	test("LinesWithoutAFileNameAreIgnored") {
		ChecksumList.parse("justonetoken\n") shouldBe emptyMap()
	}
})
