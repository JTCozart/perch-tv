package com.jtcozart.perch.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/** Hands a downloaded APK to Android's package installer, which shows its own confirmation. */
class ApkInstaller(private val context: Context) {
	enum class Outcome { InstallerOpened, PermissionNeeded, Unavailable }

	private companion object {
		const val TAG = "PerchUpdate"
		const val APK_MIME_TYPE = "application/vnd.android.package-archive"
		const val PROVIDER_SUFFIX = ".updates"
	}

	fun install(apk: File): Outcome {
		// Before Android 8 the permission is one global setting that the system installer handles itself
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
			openInstallPermissionSettings()
			return Outcome.PermissionNeeded
		}

		val uri = FileProvider.getUriForFile(context, context.packageName + PROVIDER_SUFFIX, apk)
		val intent = Intent(Intent.ACTION_VIEW)
			.setDataAndType(uri, APK_MIME_TYPE)
			.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

		return try {
			context.startActivity(intent)
			Outcome.InstallerOpened
		} catch (error: ActivityNotFoundException) {
			Log.w(TAG, "No package installer available", error)
			Outcome.Unavailable
		}
	}

	private fun openInstallPermissionSettings() {
		val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
			.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		try {
			context.startActivity(intent)
		} catch (error: ActivityNotFoundException) {
			// Some TVs hide this screen; the prompt tells the viewer where to find the setting instead
			Log.w(TAG, "Install permission screen not available", error)
		}
	}
}
