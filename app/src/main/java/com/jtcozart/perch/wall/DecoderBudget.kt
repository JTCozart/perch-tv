package com.jtcozart.perch.wall

import android.media.MediaCodecList
import android.media.MediaFormat

/**
 * How many video streams this device can decode at once. TV boxes differ a lot here, so the wall asks the
 * decoders rather than assuming a number, and tiles beyond the budget wait to be selected instead of failing.
 */
object DecoderBudget {
	private val cameraVideoTypes = listOf(MediaFormat.MIMETYPE_VIDEO_AVC, MediaFormat.MIMETYPE_VIDEO_HEVC)

	/** Keeps one decoder free so opening a camera full screen never has to wait for a tile to let go. */
	private const val RESERVED_FOR_FULL_SCREEN = 1

	fun liveTileLimit(): Int {
		val instancesPerType = cameraVideoTypes.mapNotNull(::maxInstancesFor)
		val fewest = instancesPerType.minOrNull() ?: return 1
		return (fewest - RESERVED_FOR_FULL_SCREEN).coerceIn(1, WallLayoutPlanner.TILES_PER_PAGE)
	}

	private fun maxInstancesFor(mimeType: String): Int? =
		MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
			.filter { !it.isEncoder && mimeType in it.supportedTypes }
			.maxOfOrNull { it.getCapabilitiesForType(mimeType).maxSupportedInstances }
}
