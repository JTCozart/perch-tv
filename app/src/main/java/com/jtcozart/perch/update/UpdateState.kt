package com.jtcozart.perch.update

import java.io.File

/** Where an update stands, from nothing to offer to an APK ready for the installer. */
sealed interface UpdateState {
	data object Hidden : UpdateState
	data object Checking : UpdateState
	data object UpToDate : UpdateState
	data object CheckFailed : UpdateState
	data class Offered(val release: ReleaseInfo) : UpdateState
	data class Downloading(val release: ReleaseInfo, val progress: Float) : UpdateState
	data class ReadyToInstall(val release: ReleaseInfo, val apk: File) : UpdateState
	data class Failed(val release: ReleaseInfo) : UpdateState
}
