package com.jtcozart.perch.tapo

/** Reads `name="value"` parameters out of headers such as `WWW-Authenticate` and Tapo's `Key-Exchange`. */
object HeaderParameters {
	fun valueOf(header: String, name: String): String? =
		Regex("""(?:^|[\s,])${Regex.escape(name)}="([^"]*)"""").find(header)?.groupValues?.get(1)
}
