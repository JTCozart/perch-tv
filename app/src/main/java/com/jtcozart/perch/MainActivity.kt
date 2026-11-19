package com.jtcozart.perch

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jtcozart.perch.storage.EncryptedCameraRepository
import com.jtcozart.perch.ui.PerchApp
import com.jtcozart.perch.wall.DecoderBudget

class MainActivity : ComponentActivity() {
	private val repository by lazy { EncryptedCameraRepository(applicationContext) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		// A camera wall is meant to stay up, so the TV's screensaver should not cover it.
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

		val liveTileLimit = DecoderBudget.liveTileLimit()
		setContent { PerchApp(repository, liveTileLimit) }
	}
}
