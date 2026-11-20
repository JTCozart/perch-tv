package com.jtcozart.perch.update

import java.io.File

/** Where Perch finds and fetches new releases. */
interface UpdateSource {
	/** Looks up the newest stable release. */
	suspend fun latestRelease(): ReleaseLookup

	/**
	 * Downloads the release's APK to [target] and confirms it matches the published checksum.
	 * Throws [UpdateDownloadException] when the download fails or the file does not match.
	 */
	suspend fun downloadApk(release: ReleaseInfo, target: File, onProgress: (fraction: Float) -> Unit)
}
