package com.jtcozart.perch.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Reads releases from the public GitHub repository. Only the latest stable release is considered, because GitHub's
 * "latest" leaves out pre-releases. [log] receives problems worth a log line, never a URL with credentials.
 */
class GitHubUpdateSource(
	private val latestReleaseUrl: String = LATEST_RELEASE_URL,
	private val log: (String) -> Unit = {},
) : UpdateSource {
	private companion object {
		const val LATEST_RELEASE_URL = "https://api.github.com/repos/JTCozart/perch-tv/releases/latest"
		const val USER_AGENT = "Perch-update-check"
		const val CONNECT_TIMEOUT_MS = 10_000
		const val READ_TIMEOUT_MS = 20_000
		const val BUFFER_SIZE = 64 * 1024
		const val HTTP_OK = 200
		const val HTTP_NOT_FOUND = 404
	}

	override suspend fun latestRelease(): ReleaseLookup = withContext(Dispatchers.IO) {
		try {
			open(latestReleaseUrl, accept = "application/vnd.github+json").useResponse { connection ->
				when (connection.responseCode) {
					HTTP_NOT_FOUND -> ReleaseLookup.NoRelease
					HTTP_OK -> connection.inputStream.bufferedReader().readText()
						.let(ReleaseParser::parse)?.let(ReleaseLookup::Found) ?: ReleaseLookup.NoRelease
					else -> {
						log("Update check refused status=${connection.responseCode}")
						ReleaseLookup.Unreachable
					}
				}
			}
		} catch (error: IOException) {
			// Best effort: being offline or rate limited only matters when the viewer asked for the check
			log("Update check failed reason=${error.javaClass.simpleName}")
			ReleaseLookup.Unreachable
		}
	}

	override suspend fun downloadApk(release: ReleaseInfo, target: File, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
		val checksumsUrl = release.checksumsUrl ?: throw UpdateDownloadException("The release has no checksum file")
		val expectedChecksum = ChecksumList.parse(fetchText(checksumsUrl))[ReleaseParser.APK_ASSET_NAME]
			?: throw UpdateDownloadException("The checksum file does not list ${ReleaseParser.APK_ASSET_NAME}")

		target.parentFile?.mkdirs()
		val partial = File(target.parentFile, target.name + ".part")
		val digest = MessageDigest.getInstance("SHA-256")

		try {
			open(release.apkUrl).useResponse { connection ->
				if (connection.responseCode != HTTP_OK) throw UpdateDownloadException("Download failed with status ${connection.responseCode}")
				val totalBytes = connection.getHeaderField("Content-Length")?.toLongOrNull()?.takeIf { it > 0 } ?: release.apkSizeBytes
				var readBytes = 0L

				connection.inputStream.use { input ->
					partial.outputStream().use { output ->
						val buffer = ByteArray(BUFFER_SIZE)
						while (true) {
							ensureActive()
							val count = input.read(buffer)
							if (count < 0) break
							output.write(buffer, 0, count)
							digest.update(buffer, 0, count)
							readBytes += count
							if (totalBytes > 0) onProgress((readBytes.toFloat() / totalBytes).coerceIn(0f, 1f))
						}
					}
				}
			}
		} catch (error: IOException) {
			partial.delete()
			throw UpdateDownloadException("Download failed", error)
		} catch (error: UpdateDownloadException) {
			partial.delete()
			throw error
		}

		val actualChecksum = digest.digest().joinToString("") { "%02x".format(it) }
		if (actualChecksum != expectedChecksum) {
			partial.delete()
			throw UpdateDownloadException("The download does not match the published checksum")
		}

		target.delete()
		if (!partial.renameTo(target)) throw UpdateDownloadException("The download could not be saved")
	}

	private fun fetchText(url: String): String = try {
		open(url).useResponse { connection ->
			if (connection.responseCode != HTTP_OK) throw UpdateDownloadException("Checksum download failed with status ${connection.responseCode}")
			connection.inputStream.bufferedReader().readText()
		}
	} catch (error: IOException) {
		throw UpdateDownloadException("Checksum download failed", error)
	}

	private fun open(url: String, accept: String? = null): HttpURLConnection =
		(URL(url).openConnection() as HttpURLConnection).apply {
			connectTimeout = CONNECT_TIMEOUT_MS
			readTimeout = READ_TIMEOUT_MS
			setRequestProperty("User-Agent", USER_AGENT)
			accept?.let { setRequestProperty("Accept", it) }
		}

	private inline fun <T> HttpURLConnection.useResponse(block: (HttpURLConnection) -> T): T =
		try {
			block(this)
		} finally {
			disconnect()
		}
}
