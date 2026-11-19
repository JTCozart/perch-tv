package com.jtcozart.perch.tapo

/** Builds an HTTP digest `Authorization` header (RFC 2617, MD5) answering a camera's `WWW-Authenticate` challenge. */
object DigestAuthorization {
	private const val NONCE_COUNT = "00000001"
	private const val PREFERRED_QOP = "auth"

	fun header(challenge: String, username: String, password: String, method: String, uri: String, clientNonce: String): String {
		val realm = HeaderParameters.valueOf(challenge, "realm").orEmpty()
		val nonce = HeaderParameters.valueOf(challenge, "nonce").orEmpty()
		val qop = chooseQop(HeaderParameters.valueOf(challenge, "qop"))
		val opaque = HeaderParameters.valueOf(challenge, "opaque")

		val ha1 = md5Hex("$username:$realm:$password")
		val ha2 = md5Hex("$method:$uri")
		val response = if (qop == null) md5Hex("$ha1:$nonce:$ha2") else md5Hex("$ha1:$nonce:$NONCE_COUNT:$clientNonce:$qop:$ha2")

		return buildString {
			append("""Digest username="$username", realm="$realm", nonce="$nonce", uri="$uri"""")
			if (qop != null) append(""", qop=$qop, nc=$NONCE_COUNT, cnonce="$clientNonce"""")
			append(""", response="$response"""")
			if (opaque != null) append(""", opaque="$opaque", algorithm=MD5""")
		}
	}

	private fun chooseQop(offered: String?): String? {
		val options = offered?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: return null
		return options.firstOrNull { it == PREFERRED_QOP } ?: options.firstOrNull()
	}

	private fun md5Hex(text: String): String = HexText.lower(HexText.md5(text))
}
