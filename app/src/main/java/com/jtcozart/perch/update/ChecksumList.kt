package com.jtcozart.perch.update

/** Reads a `sha256sum` output file: one `<hash>  <file name>` line per file, where a `*` before the name marks binary mode. */
object ChecksumList {
	private val whitespace = Regex("""\s+""")

	fun parse(text: String): Map<String, String> = text.lineSequence()
		.map { it.trim() }
		.filter { it.isNotEmpty() }
		.mapNotNull { line ->
			val parts = line.split(whitespace, limit = 2)
			if (parts.size < 2) null else parts[1].removePrefix("*") to parts[0].lowercase()
		}
		.toMap()
}
