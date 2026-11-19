@file:OptIn(UnstableApi::class)

package com.jtcozart.perch.ui.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.jtcozart.perch.tapo.TapoStreamAddress
import com.jtcozart.perch.tapo.TapoStreamException
import com.jtcozart.perch.tapo.TapoStreamSession

/** Feeds the decrypted MPEG-TS of a `tapo://` address to ExoPlayer as one endless, unseekable stream. */
class TapoDataSource : BaseDataSource(true) {
	private var session: TapoStreamSession? = null
	private var openedUri: Uri? = null
	private var segment = ByteArray(0)
	private var positionInSegment = 0

	override fun open(dataSpec: DataSpec): Long {
		transferInitializing(dataSpec)
		session = TapoStreamSession.open(TapoStreamAddress.parse(dataSpec.uri.toString()))
		openedUri = dataSpec.uri
		transferStarted(dataSpec)
		return C.LENGTH_UNSET.toLong()
	}

	override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
		if (length == 0) return 0
		val stream = session ?: return C.RESULT_END_OF_INPUT
		while (positionInSegment >= segment.size) {
			// A live camera never ends its stream on purpose, so an end is an error the player reconnects from.
			segment = stream.nextVideoSegment() ?: throw TapoStreamException("camera ended the stream")
			positionInSegment = 0
		}
		val count = minOf(length, segment.size - positionInSegment)
		System.arraycopy(segment, positionInSegment, buffer, offset, count)
		positionInSegment += count
		bytesTransferred(count)
		return count
	}

	override fun getUri(): Uri? = openedUri

	override fun close() {
		val wasOpen = openedUri != null
		session?.close()
		session = null
		openedUri = null
		segment = ByteArray(0)
		positionInSegment = 0
		if (wasOpen) transferEnded()
	}

	class Factory : DataSource.Factory {
		override fun createDataSource(): DataSource = TapoDataSource()
	}
}
