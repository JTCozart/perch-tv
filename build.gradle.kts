plugins {
	alias(libs.plugins.android.application) apply false
	alias(libs.plugins.kotlin.compose) apply false
	alias(libs.plugins.kotlin.serialization) apply false
}

buildscript {
	dependencies {
		// AGP 9 compiles Kotlin itself but still needs the Kotlin Gradle plugin on the classpath for the compose plugin
		classpath(libs.kotlin.gradle)
	}
}
