package com.jtcozart.perch.camera

/** Tapo's fixed RTSP paths: stream1 and stream2 for the main lens, stream6 and stream7 for a telephoto lens. */
object TapoStreamPath {
	fun of(lens: Lens, quality: StreamQuality): String = when (lens) {
		Lens.Wide -> if (quality == StreamQuality.Main) "stream1" else "stream2"
		Lens.Telephoto -> if (quality == StreamQuality.Main) "stream6" else "stream7"
	}
}
