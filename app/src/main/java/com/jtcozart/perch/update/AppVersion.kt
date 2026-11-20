package com.jtcozart.perch.update

/** A semantic version such as `0.2.0` or `0.3.0-rc.1`, the shape of Perch's release tags and version names. */
data class AppVersion(
	val major: Int,
	val minor: Int,
	val patch: Int,
	val preRelease: String?,
) : Comparable<AppVersion> {
	/** Built from source rather than from a release tag; such a build must never be replaced by an update. */
	val isDevelopment: Boolean get() = preRelease?.startsWith(DEVELOPMENT_LABEL) == true

	override fun compareTo(other: AppVersion): Int = compareValuesBy(
		this,
		other,
		{ it.major },
		{ it.minor },
		{ it.patch },
		// A final release sorts after any pre-release of the same number
		{ it.preRelease == null },
		{ it.preReleaseNumber },
	)

	private val preReleaseNumber: Int get() = preRelease?.substringAfterLast('.')?.toIntOrNull() ?: 0

	override fun toString(): String = listOfNotNull("$major.$minor.$patch", preRelease).joinToString("-")

	companion object {
		private const val DEVELOPMENT_LABEL = "dev"
		private val pattern = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.]+))?$""")

		fun parse(text: String): AppVersion? {
			val match = pattern.matchEntire(text.trim()) ?: return null
			val (major, minor, patch, preRelease) = match.destructured
			return AppVersion(major.toInt(), minor.toInt(), patch.toInt(), preRelease.ifEmpty { null })
		}
	}
}
