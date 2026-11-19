package com.jtcozart.perch.camera

enum class FormProblem(val message: String) {
	NameMissing("Give the camera a name."),
	HostMissing("Enter the camera's IP address."),
	HostInvalid("Enter just the address, such as 192.168.1.40, with no rtsp:// or spaces."),
	PortInvalid("The port is a number from 1 to 65535. Tapo cameras use 554."),
	UsernameLength("Tapo camera account usernames are 6 to 32 characters."),
	PasswordLength("Tapo camera account passwords are 6 to 32 characters."),
	StreamUrlInvalid("Enter a stream address starting with rtsp://, rtsps://, http:// or https://."),
	SubStreamUrlInvalid("The tile stream must also start with rtsp://, rtsps://, http:// or https://, or be left empty."),
}
