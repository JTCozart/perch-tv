package com.jtcozart.perch.update

/** When Perch looks for a new release, and when it offers one. */
object UpdateSchedule {
	fun checksOnLaunch(installed: AppVersion): Boolean = !installed.isDevelopment

	fun shouldOffer(latest: AppVersion, installed: AppVersion): Boolean = latest > installed
}
