@file:OptIn(UnstableApi::class)

package com.jtcozart.perch.ui.player

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.atomic.AtomicLong
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
import com.jtcozart.perch.playback.FrameProgressWatch
import com.jtcozart.perch.tapo.TapoPasswordRefusedException
import com.jtcozart.perch.tapo.TapoStreamException
import kotlinx.coroutines.delay

private const val RETRY_DELAY_MS = 5_000L
private const val STALL_TIMEOUT_MS = 20_000L
private const val STALL_CHECK_INTERVAL_MS = 2_000L
private const val TAG = "Perch"

/** Messages of a TapoStreamException never contain the address or password, so they are safe to log. */
private fun Throwable.causes(): Sequence<Throwable> = generateSequence(this) { it.cause }

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
	// Counted on the playback thread as each frame is released to the screen; the decoder counters are not reliable here
	val renderedFrames = remember(player) { AtomicLong() }

	DisposableEffect(player) {
		val listener = object : Player.Listener {
			override fun onPlaybackStateChanged(playbackState: Int) {
				when (playbackState) {
					Player.STATE_READY -> status = VideoStatus.Live
					// A live camera has no end, so an ended stream means the camera hung up
					Player.STATE_ENDED -> status = VideoStatus.Offline
				}
			}

			override fun onPlayerError(error: PlaybackException) {
				val tapoFailure = error.causes().filterIsInstance<TapoStreamException>().firstOrNull()
				Log.w(TAG, "Stream failed url=${RedactedUrl.of(url)} code=${error.errorCodeName} reason=${tapoFailure?.message}")
				status = if (tapoFailure is TapoPasswordRefusedException) VideoStatus.PasswordRefused else VideoStatus.Offline
			}
		}
		player.addListener(listener)
		player.setVideoFrameMetadataListener { _, _, _, _ -> renderedFrames.incrementAndGet() }
		player.prepare()
		onDispose {
			player.removeListener(listener)
			player.release()
		}
	}

	// A stream that stops sending without an error (a camera that falls asleep or drops off Wi-Fi) can leave the
	// player showing its last frame indefinitely, so no new frame for too long counts as offline and reconnects.
	val isPlayingOrConnecting = status == VideoStatus.Live || status == VideoStatus.Connecting
	LaunchedEffect(player, isPlayingOrConnecting) {
		if (!isPlayingOrConnecting) return@LaunchedEffect
		val watch = FrameProgressWatch(STALL_TIMEOUT_MS, startedAtMs = SystemClock.elapsedRealtime())
		while (true) {
			delay(STALL_CHECK_INTERVAL_MS)
			val framesSoFar = renderedFrames.get().takeIf { it > 0 }
			if (watch.isStalled(framesSoFar, SystemClock.elapsedRealtime())) {
				Log.w(TAG, "Stream stalled url=${RedactedUrl.of(url)}")
				status = VideoStatus.Offline
				break
			}
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
