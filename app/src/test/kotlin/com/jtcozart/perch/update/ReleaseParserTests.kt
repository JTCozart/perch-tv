package com.jtcozart.perch.update

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ReleaseParserTests : FunSpec({
	val apkAndChecksums = """{"name":"perch-tv.apk","browser_download_url":"https://example.test/perch-tv.apk","size":1234},""" +
		"""{"name":"SHA256SUMS","browser_download_url":"https://example.test/SHA256SUMS","size":90}"""

	fun release(tag: String = "v0.3.0", assets: String = apkAndChecksums) =
		"""{"tag_name":"$tag","html_url":"https://example.test/r","body":"Notes","assets":[$assets],"extra":1}"""

	test("TheFixedNameApkAndChecksumFileAreFound") {
		val parsed = ReleaseParser.parse(release())!!
		parsed.version shouldBe AppVersion(0, 3, 0, null)
		parsed.apkUrl shouldBe "https://example.test/perch-tv.apk"
		parsed.apkSizeBytes shouldBe 1234
		parsed.checksumsUrl shouldBe "https://example.test/SHA256SUMS"
		parsed.notes shouldBe "Notes"
	}

	test("AReleaseWithoutTheApkIsNotAnUpdate") {
		ReleaseParser.parse(release(assets = """{"name":"other.zip","browser_download_url":"https://example.test/o","size":1}""")) shouldBe null
	}

	test("ATagThatIsNotAVersionIsNotAnUpdate") {
		ReleaseParser.parse(release(tag = "nightly")) shouldBe null
	}

	test("ABodyThatIsNotJsonIsNotAnUpdate") {
		ReleaseParser.parse("<html>rate limited</html>") shouldBe null
	}
})
