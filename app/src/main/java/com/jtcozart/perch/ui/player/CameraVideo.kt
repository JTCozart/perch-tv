@file:OptIn(UnstableApi::class)

package com.jtcozart.perch.ui.player

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.jtcozart.perch.camera.RedactedUrl
import kotlinx.coroutines.delay

private const val RETRY_DELAY_MS = 5_000L
private const val TAG = "Perch"

/**
 * Plays one camera while the app is in the foreground. The player is released when the app goes to the
 * background or the tile leaves the screen, and a dropped stream reconnects by itself.
 */
@Composable
fun CameraVideo(
	url: String,
	withSound: Boolean,
	onStatusChange: (VideoStatus) -> Unit,
	modifier: Modifier = Modifier,
	resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
) {
	val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
	Box(modifier = modifier.background(Color.Black)) {
		if (lifecycleState.isAtLeast(Lifecycle.State.STARTED)) {
			ConnectedVideo(url, withSound, onStatusChange, resizeMode)
		}
	}
}

@Composable
private fun ConnectedVideo(url: String, withSound: Boolean, onStatusChange: (VideoStatus) -> Unit, resizeMode: Int) {
	val context = LocalContext.current
	var attempt by remember(url) { mutableIntStateOf(0) }
	var status by remember(url) { mutableStateOf(VideoStatus.Connecting) }
	val player = remember(url, attempt) { CameraPlayerFactory.create(context, url, withSound) }

	DisposableEffect(player) {
		val listener = object : Player.Listener {
			override fun onPlaybackStateChanged(playbackState: Int) {
				if (playbackState == Player.STATE_READY) status = VideoStatus.Live
			}

			override fun onPlayerError(error: PlaybackException) {
				Log.w(TAG, "Stream failed url=${RedactedUrl.of(url)} code=${error.errorCodeName}")
				status = VideoStatus.Offline
			}
		}
		player.addListener(listener)
		player.prepare()
		onDispose {
			player.removeListener(listener)
			player.release()
		}
	}

	LaunchedEffect(status) {
		onStatusChange(status)
		if (status == VideoStatus.Offline) {
			delay(RETRY_DELAY_MS)
			status = VideoStatus.Connecting
			attempt++
		}
	}

	AndroidView(
		modifier = Modifier.fillMaxSize(),
		factory = { viewContext ->
			PlayerView(viewContext).apply {
				useController = false
				setShutterBackgroundColor(android.graphics.Color.BLACK)
				isFocusable = false
			}
		},
		update = { view ->
			view.player = player
			view.resizeMode = resizeMode
		},
		onRelease = { view -> view.player = null },
	)
}
