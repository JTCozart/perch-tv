package com.jtcozart.perch.update

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

class UpdateProcessTests : FunSpec({
	fun version(text: String) = AppVersion.parse(text)!!
	fun releaseOf(text: String) = ReleaseInfo(version(text), "https://example.test/a.apk", 10, "https://example.test/s", "", "")

	class FakeUpdateSource(var lookup: ReleaseLookup, var failDownload: Boolean = false) : UpdateSource {
		var lookups = 0

		override suspend fun latestRelease(): ReleaseLookup {
			lookups++
			return lookup
		}

		override suspend fun downloadApk(release: ReleaseInfo, target: File, onProgress: (Float) -> Unit) {
			if (failDownload) throw UpdateDownloadException("no")
			onProgress(0.5f)
		}
	}

	fun processOf(source: UpdateSource, installed: String? = "0.2.0") =
		UpdateProcess(source, installed?.let(::version), File("unused"), CoroutineScope(Dispatchers.Unconfined))

	test("ANewerReleaseIsOfferedOnLaunch") {
		val process = processOf(FakeUpdateSource(ReleaseLookup.Found(releaseOf("0.3.0"))))
		process.checkOnLaunch()
		process.state.value.shouldBeInstanceOf<UpdateState.Offered>().release.version shouldBe version("0.3.0")
	}

	test("TheSameVersionStaysQuietOnLaunch") {
		val process = processOf(FakeUpdateSource(ReleaseLookup.Found(releaseOf("0.2.0"))))
		process.checkOnLaunch()
		process.state.value shouldBe UpdateState.Hidden
	}

	test("AnUnreachableGitHubStaysQuietOnLaunch") {
		val process = processOf(FakeUpdateSource(ReleaseLookup.Unreachable))
		process.checkOnLaunch()
		process.state.value shouldBe UpdateState.Hidden
	}

	test("ADevelopmentBuildNeverLooksForAnUpdate") {
		val source = FakeUpdateSource(ReleaseLookup.Found(releaseOf("9.0.0")))
		processOf(source, installed = "0.1.0-dev").checkOnLaunch()
		source.lookups shouldBe 0
	}

	test("ABuildWhoseVersionIsUnknownNeverLooksForAnUpdate") {
		val source = FakeUpdateSource(ReleaseLookup.Found(releaseOf("9.0.0")))
		processOf(source, installed = null).checkOnLaunch()
		source.lookups shouldBe 0
	}

	test("TheLaunchCheckRunsOnlyOncePerLaunch") {
		val source = FakeUpdateSource(ReleaseLookup.Found(releaseOf("0.3.0")))
		val process = processOf(source)
		process.checkOnLaunch()
		process.notNow()
		process.checkOnLaunch()
		source.lookups shouldBe 1
		process.state.value shouldBe UpdateState.Hidden
	}

	test("AManualCheckSaysWhenThereIsNothingNew") {
		val process = processOf(FakeUpdateSource(ReleaseLookup.Found(releaseOf("0.2.0"))))
		process.checkNow()
		process.state.value shouldBe UpdateState.UpToDate
	}

	test("AManualCheckSaysWhenGitHubCouldNotBeReached") {
		val process = processOf(FakeUpdateSource(ReleaseLookup.Unreachable))
		process.checkNow()
		process.state.value shouldBe UpdateState.CheckFailed
	}

	test("ADownloadThatSucceedsIsReadyToInstall") {
		val process = processOf(FakeUpdateSource(ReleaseLookup.Found(releaseOf("0.3.0"))))
		process.checkOnLaunch()
		process.startUpdate()
		process.state.value.shouldBeInstanceOf<UpdateState.ReadyToInstall>()
	}

	test("ADownloadThatFailsCanBeTriedAgain") {
		val source = FakeUpdateSource(ReleaseLookup.Found(releaseOf("0.3.0")), failDownload = true)
		val process = processOf(source)
		process.checkOnLaunch()
		process.startUpdate()
		process.state.value.shouldBeInstanceOf<UpdateState.Failed>()
		source.failDownload = false
		process.startUpdate()
		process.state.value.shouldBeInstanceOf<UpdateState.ReadyToInstall>()
	}

	test("BackingOutOfTheInstallerLeavesTheOfferStanding") {
		val process = processOf(FakeUpdateSource(ReleaseLookup.Found(releaseOf("0.3.0"))))
		process.checkOnLaunch()
		process.startUpdate()
		process.installerOpened()
		process.state.value.shouldBeInstanceOf<UpdateState.Offered>()
	}
})
