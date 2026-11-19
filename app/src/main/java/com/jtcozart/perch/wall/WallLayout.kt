package com.jtcozart.perch.wall

/** A page of the camera wall: its grid shape, and how many of its tiles play live (the rest wait to be selected). */
data class WallLayout(
	val columns: Int,
	val rows: Int,
	val liveTiles: Int,
)
