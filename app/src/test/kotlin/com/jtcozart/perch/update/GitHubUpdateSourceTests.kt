package com.jtcozart.perch.update

import com.sun.net.httpserver.HttpServer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import java.security.MessageDigest

class GitHubUpdateSourceTests : FunSpec({
	val apkBytes = ByteArray(300_000) { (it % 251).toByte() }
	val apkHash = MessageDigest.getInstance("SHA-256").digest(apkBytes).joinToString("") { "%02x".format(it) }

	fun withServer(checksumLine: String, body: (base: String) -> Unit) {
		val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
		val base = "http://127.0.0.1:${server.address.port}"
		fun reply(path: String, status: Int, bytes: ByteArray) = server.createContext(path) {
			it.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
			if (bytes.isNotEmpty()) it.responseBody.use { out -> out.write(bytes) }
			it.close()
		}
		val latest = """{"tag_name":"v0.3.0","assets":[""" +
			"""{"name":"perch-tv.apk","browser_download_url":"$base/apk","size":${apkBytes.size}},""" +
			"""{"name":"SHA256SUMS","browser_download_url":"$base/sums","size":1}]}"""
		reply("/latest", 200, latest.toByteArray())
		reply("/missing", 404, ByteArray(0))
		reply("/apk", 200, apkBytes)
		reply("/sums", 200, checksumLine.toByteArray())
		server.start()
		try {
			body(base)
		} finally {
			server.stop(0)
		}
	}

	fun tempApk(): File = File(Files.createTempDirectory("perch-update").toFile(), "perch-tv.apk")

	fun foundRelease(source: GitHubUpdateSource): ReleaseInfo =
		(runBlocking { source.latestRelease() } as ReleaseLookup.Found).release

	test("TheLatestReleaseIsReadFromTheApi") {
		withServer("$apkHash  perch-tv.apk") { base ->
			val lookup = runBlocking { GitHubUpdateSource("$base/latest").latestRelease() }
			lookup.shouldBeInstanceOf<ReleaseLookup.Found>().release.version shouldBe AppVersion(0, 3, 0, null)
		}
	}

	test("AnApiAnswerOfNotFoundMeansNoReleaseYet") {
		withServer("") { base ->
			runBlocking { GitHubUpdateSource("$base/missing").latestRelease() } shouldBe ReleaseLookup.NoRelease
		}
	}

	test("ASourceThatCannotBeReachedIsReportedAsUnreachable") {
		runBlocking { GitHubUpdateSource("http://127.0.0.1:1/latest").latestRelease() } shouldBe ReleaseLookup.Unreachable
	}

	test("ADownloadThatMatchesItsChecksumIsSaved_WithProgressReported") {
		withServer("$apkHash  perch-tv.apk") { base ->
			val source = GitHubUpdateSource("$base/latest")
			val target = tempApk()
			val progress = mutableListOf<Float>()
			runBlocking { source.downloadApk(foundRelease(source), target) { progress += it } }
			target.readBytes().contentEquals(apkBytes) shouldBe true
			progress.last() shouldBe 1f
		}
	}

	test("ADownloadThatDoesNotMatchItsChecksumIsDeleted_AndRefused") {
		withServer("${"0".repeat(64)}  perch-tv.apk") { base ->
			val source = GitHubUpdateSource("$base/latest")
			val target = tempApk()
			shouldThrow<UpdateDownloadException> { runBlocking { source.downloadApk(foundRelease(source), target) {} } }
			target.exists() shouldBe false
			File(target.parentFile, target.name + ".part").exists() shouldBe false
		}
	}
})
