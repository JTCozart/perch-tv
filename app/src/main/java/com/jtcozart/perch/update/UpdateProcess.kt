package com.jtcozart.perch.update

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Steps an update through check, offer, download and install. It checks once per launch on its own, and again
 * whenever the viewer asks. [installedVersion] is null when the running build's version cannot be read, and then
 * nothing is ever offered.
 */
class UpdateProcess(
	private val source: UpdateSource,
	private val installedVersion: AppVersion?,
	private val downloadFolder: File,
	private val scope: CoroutineScope,
) {
	private companion object {
		const val DOWNLOAD_NAME = ReleaseParser.APK_ASSET_NAME
	}

	private val _state = MutableStateFlow<UpdateState>(UpdateState.Hidden)
	val state: StateFlow<UpdateState> = _state.asStateFlow()

	private var downloadJob: Job? = null
	private var hasCheckedThisLaunch = false

	fun checkOnLaunch() {
		val installed = installedVersion ?: return
		if (hasCheckedThisLaunch || !UpdateSchedule.checksOnLaunch(installed)) return
		hasCheckedThisLaunch = true

		scope.launch {
			val lookup = source.latestRelease()
			val isStillIdle = _state.value == UpdateState.Hidden
			if (lookup is ReleaseLookup.Found && isStillIdle && UpdateSchedule.shouldOffer(lookup.release.version, installed)) {
				_state.value = UpdateState.Offered(lookup.release)
			}
		}
	}

	/** The viewer asked, so the outcome is always reported, even when there is nothing new. */
	fun checkNow() {
		val installed = installedVersion ?: return
		if (_state.value is UpdateState.Downloading) return
		hasCheckedThisLaunch = true
		_state.value = UpdateState.Checking

		scope.launch {
			_state.value = when (val lookup = source.latestRelease()) {
				is ReleaseLookup.Found ->
					if (UpdateSchedule.shouldOffer(lookup.release.version, installed)) UpdateState.Offered(lookup.release)
					else UpdateState.UpToDate
				ReleaseLookup.NoRelease -> UpdateState.UpToDate
				ReleaseLookup.Unreachable -> UpdateState.CheckFailed
			}
		}
	}

	/** Hides the offer until the next launch or a manual check. */
	fun notNow() {
		downloadJob?.cancel()
		_state.value = UpdateState.Hidden
	}

	fun startUpdate() {
		val release = currentRelease() ?: return
		downloadJob?.cancel()
		downloadJob = scope.launch {
			_state.value = UpdateState.Downloading(release, 0f)
			val target = File(downloadFolder, DOWNLOAD_NAME)
			_state.value = try {
				source.downloadApk(release, target) { progress -> _state.value = UpdateState.Downloading(release, progress) }
				UpdateState.ReadyToInstall(release, target)
			} catch (_: UpdateDownloadException) {
				UpdateState.Failed(release)
			}
		}
	}

	/** The installer has taken over; if the viewer backs out of it, the offer is still there. */
	fun installerOpened() {
		val release = currentRelease() ?: return
		_state.value = UpdateState.Offered(release)
	}

	private fun currentRelease(): ReleaseInfo? = when (val state = _state.value) {
		UpdateState.Hidden, UpdateState.Checking, UpdateState.UpToDate, UpdateState.CheckFailed -> null
		is UpdateState.Offered -> state.release
		is UpdateState.Downloading -> state.release
		is UpdateState.ReadyToInstall -> state.release
		is UpdateState.Failed -> state.release
	}
}
