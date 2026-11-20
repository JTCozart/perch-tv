package com.jtcozart.perch.update

/** What asking for the newest release found. */
sealed interface ReleaseLookup {
	data class Found(val release: ReleaseInfo) : ReleaseLookup
	data object NoRelease : ReleaseLookup
	data object Unreachable : ReleaseLookup
}
