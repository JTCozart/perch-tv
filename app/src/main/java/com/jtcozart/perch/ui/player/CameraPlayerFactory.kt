@file:OptIn(UnstableApi::class)

package com.jtcozart.perch.ui.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource

/** Builds a player tuned for a live camera: short buffers so the picture stays close to real time. */
object CameraPlayerFactory {
	private const val MIN_BUFFER_MS = 1_000
	private const val MAX_BUFFER_MS = 3_000
	private const val BUFFER_FOR_PLAYBACK_MS = 300
	private const val BUFFER_AFTER_REBUFFER_MS = 1_000
	private const val RTSP_TIMEOUT_MS = 8_000L

	fun create(context: Context, url: String, withSound: Boolean): ExoPlayer {
		val loadControl = DefaultLoadControl.Builder()
			.setBufferDurationsMs(MIN_BUFFER_MS, MAX_BUFFER_MS, BUFFER_FOR_PLAYBACK_MS, BUFFER_AFTER_REBUFFER_MS)
			.build()

		return ExoPlayer.Builder(context)
			.setLoadControl(loadControl)
			.build()
			.apply {
				// Wall tiles skip audio entirely, which saves a decoder per tile as well as the noise.
				trackSelectionParameters = trackSelectionParameters.buildUpon()
					.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, !withSound)
					.build()
				setMediaSource(mediaSourceFor(context, url))
				playWhenReady = true
			}
	}

	private fun mediaSourceFor(context: Context, url: String): MediaSource {
		val item = MediaItem.fromUri(url)
		return if (url.startsWith("rtsp", ignoreCase = true)) {
			// Interleaved TCP: UDP RTP is often dropped by Wi-Fi access points and mesh systems.
			RtspMediaSource.Factory()
				.setForceUseRtpTcp(true)
				.setTimeoutMs(RTSP_TIMEOUT_MS)
				.createMediaSource(item)
		} else {
			DefaultMediaSourceFactory(context).createMediaSource(item)
		}
	}
}
