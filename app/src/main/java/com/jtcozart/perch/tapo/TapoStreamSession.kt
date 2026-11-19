package com.jtcozart.perch.tapo

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom

/**
 * One live stream from a Tapo camera over its local protocol on port 8800, the one the Tapo app itself uses on a home
 * network. Signs in with digest auth as `admin` and the hashed Tapo app password, asks for a preview, then hands out
 * the decrypted MPEG-TS parts one at a time.
 *
 * The protocol is TP-Link's own and undocumented. It is reimplemented here from go2rtc's MIT-licensed client
 * (see NOTICES.md), so a firmware update can change it under us.
 */
class TapoStreamSession private constructor(
	private val socket: Socket,
	private val cipher: TapoSegmentCipher,
	private val parts: MultipartPartReader,
) : Closeable {
	/** The next decrypted MPEG-TS chunk, or null when the camera ends the stream. Blocks until one arrives. */
	fun nextVideoSegment(): ByteArray? {
		while (true) {
			val part = parts.next() ?: return null
			if (part.contentType.equals(VIDEO_CONTENT_TYPE, ignoreCase = true)) return cipher.decrypt(part.body)
		}
	}

	override fun close() = socket.close()

	companion object {
		const val DEFAULT_PORT = 8800
		private const val CONNECT_TIMEOUT_MS = 8_000
		private const val READ_TIMEOUT_MS = 10_000
		private const val ADMIN_USERNAME = "admin"
		private const val VIDEO_CONTENT_TYPE = "video/mp2t"
		private const val HTTP_OK = 200
		private const val HTTP_UNAUTHORIZED = 401
		private const val CLIENT_NONCE_BYTES = 16
		private val random = SecureRandom()

		fun open(address: TapoStreamAddress): TapoStreamSession {
			val socket = Socket()
			try {
				socket.connect(InetSocketAddress(address.host, address.port), CONNECT_TIMEOUT_MS)
				socket.soTimeout = READ_TIMEOUT_MS
				socket.tcpNoDelay = true
				val input = BufferedInputStream(socket.getInputStream())
				val output = BufferedOutputStream(socket.getOutputStream())

				val cipher = signIn(address, input, output)
				output.write(TapoPreviewRequest.part(address))
				output.flush()
				return TapoStreamSession(socket, cipher, MultipartPartReader(input))
			} catch (error: IOException) {
				socket.close()
				throw error as? TapoStreamException ?: TapoStreamException("could not reach the camera at ${address.host}:${address.port}", error)
			}
		}

		private fun signIn(address: TapoStreamAddress, input: InputStream, output: OutputStream): TapoSegmentCipher {
			val challengeResponse = exchange(address, authorization = null, input, output)
			val challenge = challengeResponse.headers["www-authenticate"]
			if (challengeResponse.statusCode != HTTP_UNAUTHORIZED || challenge?.startsWith("Digest", ignoreCase = true) != true) {
				throw TapoStreamException("camera answered HTTP ${challengeResponse.statusCode} with no sign-in challenge; is this a Tapo camera on port ${address.port}?")
			}
			challengeResponse.discardBody(input)

			val authorization = DigestAuthorization.header(
				challenge = challenge,
				username = ADMIN_USERNAME,
				password = TapoPasswordHash.of(address.tapoPassword, challenge),
				method = "POST",
				uri = address.requestPath,
				clientNonce = clientNonce(),
			)
			val streamResponse = exchange(address, authorization, input, output)
			when (streamResponse.statusCode) {
				HTTP_OK -> Unit
				HTTP_UNAUTHORIZED -> throw TapoPasswordRefusedException()
				else -> throw TapoStreamException("camera refused the stream with HTTP ${streamResponse.statusCode}")
			}
			val keyExchange = streamResponse.headers["key-exchange"] ?: throw TapoStreamException("camera sent no Key-Exchange header")
			return TapoSegmentCipher.from(keyExchange, TapoPasswordHash.of(address.tapoPassword, keyExchange))
		}

		private fun exchange(address: TapoStreamAddress, authorization: String?, input: InputStream, output: OutputStream): HttpResponseHead {
			val request = buildString {
				append("POST ${address.requestPath} HTTP/1.1\r\n")
				append("Host: ${address.host}:${address.port}\r\n")
				append("User-Agent: Perch\r\n")
				append("Content-Type: multipart/mixed; boundary=--client-stream-boundary--\r\n")
				append("Content-Length: 0\r\n")
				if (authorization != null) append("Authorization: $authorization\r\n")
				append("\r\n")
			}
			output.write(request.toByteArray(Charsets.ISO_8859_1))
			output.flush()
			return HttpResponseHead.read(input)
		}

		private fun clientNonce(): String = HexText.lower(ByteArray(CLIENT_NONCE_BYTES).also(random::nextBytes))
	}
}
