package com.jtcozart.perch.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jtcozart.perch.ui.components.PerchButton
import com.jtcozart.perch.ui.theme.PerchColors
import com.jtcozart.perch.update.ApkInstaller
import com.jtcozart.perch.update.UpdateState
import java.io.File

/** A dialog over the wall that follows an update from the offer to the system installer. It shows nothing when idle. */
@Composable
fun UpdatePrompt(
	state: UpdateState,
	installApk: (File) -> ApkInstaller.Outcome,
	onUpdate: () -> Unit,
	onNotNow: () -> Unit,
	onInstallerOpened: () -> Unit,
) {
	var needsInstallPermission by remember { mutableStateOf(false) }

	LaunchedEffect(state) {
		if (state is UpdateState.ReadyToInstall) {
			when (installApk(state.apk)) {
				ApkInstaller.Outcome.InstallerOpened -> needsInstallPermission = false
				ApkInstaller.Outcome.PermissionNeeded -> needsInstallPermission = true
				ApkInstaller.Outcome.Unavailable -> needsInstallPermission = false
			}
			onInstallerOpened()
		}
	}

	if (state == UpdateState.Hidden) return

	Dialog(onDismissRequest = onNotNow) {
		Column(
			modifier = Modifier
				.width(560.dp)
				.background(PerchColors.Surface, RoundedCornerShape(16.dp))
				.padding(28.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			val primary = remember { FocusRequester() }
			LaunchedEffect(state::class) { runCatching { primary.requestFocus() } }

			when (state) {
				UpdateState.Hidden -> Unit
				UpdateState.Checking -> Message("Checking for updates", "Looking for a newer version of Perch.")
				UpdateState.UpToDate -> {
					Message("Perch is up to date", "You have the newest version.")
					PerchButton("OK", onNotNow, Modifier.focusRequester(primary), isPrimary = true)
				}
				UpdateState.CheckFailed -> {
					Message("Could not check for updates", "GitHub could not be reached. Try again later.")
					PerchButton("OK", onNotNow, Modifier.focusRequester(primary), isPrimary = true)
				}
				is UpdateState.Offered -> {
					Message(
						"Perch ${state.release.version} is available",
						if (needsInstallPermission) INSTALL_PERMISSION_HINT else "Download it now and install it over this version. Your cameras are kept.",
					)
					Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
						PerchButton("Update now", onUpdate, Modifier.focusRequester(primary), isPrimary = true)
						PerchButton("Not now", onNotNow)
					}
				}
				is UpdateState.Downloading -> {
					Message("Downloading Perch ${state.release.version}", "${(state.progress * 100).toInt()}%")
					LinearProgressIndicator(
						progress = { state.progress },
						modifier = Modifier.fillMaxWidth(),
						color = PerchColors.Accent,
						trackColor = PerchColors.SurfaceRaised,
					)
					PerchButton("Cancel", onNotNow, Modifier.focusRequester(primary))
				}
				is UpdateState.ReadyToInstall -> Message("Opening the installer", "Confirm the install on the next screen.")
				is UpdateState.Failed -> {
					Message("The update could not be downloaded", "Check the connection and try again.")
					Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
						PerchButton("Try again", onUpdate, Modifier.focusRequester(primary), isPrimary = true)
						PerchButton("Not now", onNotNow)
					}
				}
			}
		}
	}
}

@Composable
private fun Message(title: String, detail: String) {
	Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = PerchColors.Text)
	Text(detail, style = MaterialTheme.typography.bodyLarge, color = PerchColors.TextMuted)
}

private const val INSTALL_PERMISSION_HINT =
	"Allow Perch to install apps in the settings screen that opened (Settings, Apps, Special app access, " +
		"Install unknown apps), then choose Update now again."
