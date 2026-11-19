plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.kotlin.serialization)
}

// The release workflow passes the tag (v1.2.3 or v1.2.3-rc.1); local builds get a dev version
val releaseVersion: String = System.getenv("PERCH_VERSION")?.removePrefix("v") ?: "0.1.0-dev"

fun versionCodeOf(version: String): Int {
	val (major, minor, patch) = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }.plus(listOf(0, 0, 0)).take(3)
	return major * 1_000_000 + minor * 1_000 + patch
}

android {
	namespace = "com.jtcozart.perch"
	compileSdk = libs.versions.android.compileSdk.get().toInt()

	defaultConfig {
		applicationId = "com.jtcozart.perch"
		minSdk = libs.versions.android.minSdk.get().toInt()
		targetSdk = libs.versions.android.targetSdk.get().toInt()
		versionName = releaseVersion
		versionCode = versionCodeOf(releaseVersion).coerceAtLeast(1)
	}

	buildFeatures {
		compose = true
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	signingConfigs {
		val keystoreFile = System.getenv("KEYSTORE_FILE")
		val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
		val signingKeyAlias = System.getenv("SIGNING_KEY_ALIAS")
		val signingKeyPassword = System.getenv("SIGNING_KEY_PASSWORD")

		if (keystoreFile != null && keystorePassword != null && signingKeyAlias != null && signingKeyPassword != null) {
			create("release") {
				storeFile = file(keystoreFile)
				storePassword = keystorePassword
				keyAlias = signingKeyAlias
				keyPassword = signingKeyPassword
			}
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
			signingConfig = signingConfigs.findByName("release")
		}
		debug {
			applicationIdSuffix = ".debug"
		}
	}

	testOptions {
		unitTests.all { it.useJUnitPlatform() }
	}

	dependenciesInfo {
		includeInApk = false
		includeInBundle = false
	}
}

dependencies {
	implementation(libs.kotlinx.coroutines)
	implementation(libs.kotlinx.serialization.json)
	implementation(libs.androidx.core)
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.compose.foundation)
	implementation(libs.androidx.compose.ui)
	implementation(libs.androidx.compose.material3)
	implementation(libs.androidx.lifecycle.runtime.compose)
	implementation(libs.androidx.lifecycle.viewmodel.compose)
	implementation(libs.androidx.media3.exoplayer)
	implementation(libs.androidx.media3.exoplayer.rtsp)
	implementation(libs.androidx.media3.exoplayer.hls)
	implementation(libs.androidx.media3.ui)
	debugImplementation(libs.androidx.compose.ui.tooling)

	testImplementation(libs.kotest.runner.junit5)
	testImplementation(libs.kotest.assertions)
}
