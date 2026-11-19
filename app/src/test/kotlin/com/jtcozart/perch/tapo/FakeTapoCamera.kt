package com.jtcozart.perch.tapo

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread

/**
 * A stand-in camera on a loopback port that speaks the Tapo local stream protocol: digest sign-in with the hashed
 * Tapo password, then one encrypted MPEG-TS part per segment. Written from the protocol, not from the client, so a
 * test fails if the client and the camera disagree.
 */
class FakeTapoCamera(
	private val tapoPassword: String,
	private val segments: List<ByteArray>,
	private val usesSha256: Boolean = false,
) : AutoCloseable {
	private val server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
	val port: Int get() = server.localPort

	@Volatile var requestPath: String? = null
		private set

	@Volatile var previewRequest: String? = null
		private set

	init {
		thread(isDaemon = true) { runCatching { serveOneClient() } }
	}

	override fun close() = server.close()

	private fun serveOneClient() {
		server.accept().use { socket ->
			val input = BufferedInputStream(socket.getInputStream())
			val output = socket.getOutputStream()

			val firstRequest = readHead(input)
			requestPath = firstRequest.first().split(' ')[1]
			output.write(ascii("HTTP/1.1 401 Unauthorized\r\nWWW-Authenticate: $challenge\r\nContent-Length: 0\r\n\r\n"))

			val secondRequest = readHead(input)
			if (!isAuthorized(secondRequest)) {
				output.write(ascii("HTTP/1.1 401 Unauthorized\r\nContent-Length: 0\r\n\r\n"))
				return
			}
			output.write(
				ascii(
					"HTTP/1.1 200 OK\r\nContent-Type: multipart/mixed; boundary=--device-stream-boundary--\r\n" +
						"Key-Exchange: username=\"admin\" cipher=\"AES_128_CBC\" nonce=\"$KEY_NONCE\"$encryptTypeParameter\r\n\r\n",
				),
			)

			previewRequest = readClientPart(input)
			writePart(output, "application/json", ascii("""{"type":"response","seq":1,"params":{"error_code":0,"session_id":"7"}}"""))
			segments.forEach { writePart(output, "video/mp2t", encrypt(it, KEY_NONCE, hashedPassword)) }
			output.flush()
		}
	}

	private val hashedPassword: String
		get() = hexUpper(MessageDigest.getInstance(if (usesSha256) "SHA-256" else "MD5").digest(ascii(tapoPassword)))

	private val challenge: String
		get() = "Digest realm=\"$REALM\", nonce=\"$AUTH_NONCE\", qop=\"auth\"" + if (usesSha256) ",$encryptTypeParameter" else ""

	private val encryptTypeParameter: String
		get() = if (usesSha256) " encrypt_type=\"3\"" else ""

	private fun isAuthorized(head: List<String>): Boolean {
		val authorization = head.firstOrNull { it.startsWith("Authorization:", ignoreCase = true) } ?: return false
		fun field(name: String) = Regex("""\b$name="?([^",]*)"?""").find(authorization)?.groupValues?.get(1)
		val ha1 = md5Hex("${field("username")}:$REALM:$hashedPassword")
		val ha2 = md5Hex("POST:${field("uri")}")
		val expected = md5Hex("$ha1:$AUTH_NONCE:${field("nc")}:${field("cnonce")}:auth:$ha2")
		return field("username") == "admin" && field("response") == expected
	}

	private fun readClientPart(input: InputStream): String {
		val head = readHead(input)
		val length = head.first { it.startsWith("Content-Length:", ignoreCase = true) }.substringAfter(':').trim().toInt()
		return String(input.readNBytes(length), Charsets.UTF_8)
	}

	private fun writePart(output: OutputStream, contentType: String, body: ByteArray) {
		output.write(ascii("----device-stream-boundary--\r\nContent-Type: $contentType\r\nContent-Length: ${body.size}\r\n\r\n"))
		output.write(body)
		output.write(ascii("\r\n"))
	}

	/** Lines up to the blank line; the client's part starts with its boundary line, which is skipped. */
	private fun readHead(input: InputStream): List<String> {
		val lines = mutableListOf<String>()
		while (true) {
			val line = readLine(input)
			if (line.isEmpty()) {
				if (lines.isEmpty()) continue
				return lines
			}
			if (!line.startsWith("--")) lines += line
		}
	}

	private fun readLine(input: InputStream): String {
		val bytes = ByteArrayOutputStream()
		while (true) {
			val next = input.read()
			check(next >= 0) { "client closed the connection" }
			if (next == '\n'.code) return bytes.toString(Charsets.ISO_8859_1).trimEnd('\r')
			bytes.write(next)
		}
	}

	companion object {
		private const val REALM = "TP-LINK IP-Camera"
		private const val AUTH_NONCE = "5D3F9A"
		private const val KEY_NONCE = "C0FFEE42"

		/** AES-128-CBC with PKCS#7 padding, key md5(nonce:hash) and IV md5(admin:nonce), as the camera sends it. */
		fun encrypt(plain: ByteArray, nonce: String, hashedPassword: String): ByteArray {
			val key = MessageDigest.getInstance("MD5").digest(ascii("$nonce:$hashedPassword"))
			val iv = MessageDigest.getInstance("MD5").digest(ascii("admin:$nonce"))
			return Cipher.getInstance("AES/CBC/PKCS5Padding")
				.apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv)) }
				.doFinal(plain)
		}

		private fun ascii(text: String) = text.toByteArray(Charsets.ISO_8859_1)
		private fun md5Hex(text: String) = MessageDigest.getInstance("MD5").digest(ascii(text)).joinToString("") { "%02x".format(it) }
		private fun hexUpper(bytes: ByteArray) = bytes.joinToString("") { "%02X".format(it) }
	}
}
