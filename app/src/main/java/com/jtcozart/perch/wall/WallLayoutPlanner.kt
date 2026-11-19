package com.jtcozart.perch.wall

import kotlin.math.ceil
import kotlin.math.sqrt

object WallLayoutPlanner {
	/** Beyond 4 by 4 a tile is too small to read from the couch, so more cameras go on another page. */
	const val MAX_COLUMNS = 4
	const val TILES_PER_PAGE = MAX_COLUMNS * MAX_COLUMNS

	fun plan(cameraCount: Int, liveTileLimit: Int): WallLayout {
		val tiles = cameraCount.coerceIn(0, TILES_PER_PAGE)
		if (tiles == 0) return WallLayout(columns = 0, rows = 0, liveTiles = 0)

		val columns = ceil(sqrt(tiles.toDouble())).toInt()
		val rows = ceil(tiles.toDouble() / columns).toInt()
		return WallLayout(columns = columns, rows = rows, liveTiles = minOf(tiles, liveTileLimit.coerceAtLeast(0)))
	}

	fun pageCount(cameraCount: Int): Int = (cameraCount + TILES_PER_PAGE - 1) / TILES_PER_PAGE
}
