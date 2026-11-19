package com.jtcozart.perch.playback

/**
 * Decides a live stream has stalled when no new frame has been rendered for [timeoutMs]. The player's own state cannot
 * be trusted for this: a progressive stream that stops sending can leave it READY on its last frame indefinitely.
 */
class FrameProgressWatch(private val timeoutMs: Long, startedAtMs: Long) {
	private var lastRenderedFrames: Long? = null
	private var lastProgressAtMs = startedAtMs

	/** [renderedFrames] is null until the decoder has started. */
	fun isStalled(renderedFrames: Long?, nowMs: Long): Boolean {
		if (renderedFrames != null && renderedFrames != lastRenderedFrames) {
			lastRenderedFrames = renderedFrames
			lastProgressAtMs = nowMs
		}
		return nowMs - lastProgressAtMs > timeoutMs
	}
}
