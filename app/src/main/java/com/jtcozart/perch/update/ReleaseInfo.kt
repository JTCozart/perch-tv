package com.jtcozart.perch.update

/** A published Perch release, as far as updating needs to know it. */
data class ReleaseInfo(
	val version: AppVersion,
	val apkUrl: String,
	val apkSizeBytes: Long,
	val checksumsUrl: String?,
	val notes: String,
	val pageUrl: String,
)
