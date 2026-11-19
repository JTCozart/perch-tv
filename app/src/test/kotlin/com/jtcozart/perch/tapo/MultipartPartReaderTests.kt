package com.jtcozart.perch.tapo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream

class MultipartPartReaderTests : FunSpec({
	fun readerOf(text: String) = MultipartPartReader(ByteArrayInputStream(text.toByteArray(Charsets.ISO_8859_1)))

	test("APartWithALengthIsReadByThatLength_EvenWhenTheBodyContainsLineBreaks") {
		val body = "ab\r\ncd"
		val reader = readerOf("----device-stream-boundary--\r\nContent-Type: video/mp2t\r\nContent-Length: ${body.length}\r\n\r\n$body\r\n")

		val part = reader.next()!!
		part.contentType shouldBe "video/mp2t"
		String(part.body, Charsets.ISO_8859_1) shouldBe body
	}

	test("APartWithNoLengthRunsToTheNextBoundary") {
		val reader = readerOf(
			"----device-stream-boundary--\r\nContent-Type: application/json\r\n\r\n{\"a\":1}\r\n" +
				"----device-stream-boundary--\r\nContent-Type: video/mp2t\r\nContent-Length: 2\r\n\r\nxy\r\n",
		)

		String(reader.next()!!.body) shouldBe "{\"a\":1}"
		String(reader.next()!!.body) shouldBe "xy"
	}

	test("PartsFollowOneAnotherAndTheEndOfTheStreamIsNull") {
		val reader = readerOf(
			"----device-stream-boundary--\r\nContent-Type: video/mp2t\r\nContent-Length: 1\r\n\r\nA\r\n" +
				"----device-stream-boundary--\r\nContent-Type: video/mp2t\r\nContent-Length: 1\r\n\r\nB\r\n",
		)

		String(reader.next()!!.body) shouldBe "A"
		String(reader.next()!!.body) shouldBe "B"
		reader.next() shouldBe null
	}

	test("HeaderNamesAreMatchedWithoutRegardToCase") {
		val reader = readerOf("----device-stream-boundary--\r\ncontent-type: video/mp2t\r\ncontent-length: 1\r\n\r\nZ\r\n")

		reader.next()!!.contentType shouldBe "video/mp2t"
	}
})
