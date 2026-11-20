package com.jtcozart.perch

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jtcozart.perch.storage.EncryptedCameraRepository
import com.jtcozart.perch.ui.PerchApp
import com.jtcozart.perch.ui.update.UpdatePrompt
import com.jtcozart.perch.update.ApkInstaller
import com.jtcozart.perch.update.AppVersion
import com.jtcozart.perch.update.GitHubUpdateSource
import com.jtcozart.perch.update.UpdateViewModel
import com.jtcozart.perch.wall.DecoderBudget
import java.io.File

class MainActivity : ComponentActivity() {
	private val repository by lazy { EncryptedCameraRepository(applicationContext) }

	private val updates: UpdateViewModel by viewModels {
		object : ViewModelProvider.Factory {
			@Suppress("UNCHECKED_CAST")
			override fun <T : ViewModel> create(modelClass: Class<T>): T = UpdateViewModel(
				source = GitHubUpdateSource(log = { Log.i("PerchUpdate", it) }),
				installedVersion = installedVersion(),
				downloadFolder = File(cacheDir, "updates"),
			) as T
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		// A camera wall is meant to stay up, so the TV's screensaver should not cover it.
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

		val liveTileLimit = DecoderBudget.liveTileLimit()
		val installer = ApkInstaller(this)
		updates.checkOnLaunch()
		setContent {
			val updateState by updates.state.collectAsStateWithLifecycle()
			PerchApp(repository, liveTileLimit, onCheckForUpdates = updates::checkNow)
			UpdatePrompt(
				state = updateState,
				installApk = installer::install,
				onUpdate = updates::startUpdate,
				onNotNow = updates::notNow,
				onInstallerOpened = updates::installerOpened,
			)
		}
	}

	private fun installedVersion(): AppVersion? =
		packageManager.getPackageInfo(packageName, 0).versionName?.let(AppVersion::parse)
}
