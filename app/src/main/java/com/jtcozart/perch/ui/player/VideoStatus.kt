package com.jtcozart.perch.ui.player

enum class VideoStatus(val label: String) {
	Connecting("Connecting"),
	Live("Live"),
	Offline("Offline, retrying"),
	PasswordRefused("Tapo password refused"),
}
