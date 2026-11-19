package com.jtcozart.perch.storage

import android.content.Context
import android.util.Log
import com.jtcozart.perch.camera.Camera
import com.jtcozart.perch.camera.CameraListCodec
import com.jtcozart.perch.camera.CameraListEdits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.GeneralSecurityException

/** Keeps the camera list in one encrypted file outside backups (the manifest also turns backup off). */
class EncryptedCameraRepository(
	context: Context,
	private val cipher: KeystoreCipher = KeystoreCipher(),
) : CameraRepository {
	private val file = File(context.noBackupFilesDir, FILE_NAME)
	private val writeLock = Mutex()
	private val state = MutableStateFlow(readSaved())

	override val cameras: StateFlow<List<Camera>> = state.asStateFlow()

	override suspend fun save(camera: Camera) = update { CameraListEdits.save(it, camera) }

	override suspend fun delete(cameraId: String) = update { CameraListEdits.delete(it, cameraId) }

	override suspend fun move(cameraId: String, offset: Int) = update { CameraListEdits.move(it, cameraId, offset) }

	private suspend fun update(change: (List<Camera>) -> List<Camera>) = writeLock.withLock {
		val updated = change(state.value)
		withContext(Dispatchers.IO) { write(updated) }
		state.value = updated
	}

	private fun write(cameras: List<Camera>) {
		val temporary = File(file.parentFile, "$FILE_NAME.tmp")
		temporary.writeBytes(cipher.encrypt(CameraListCodec.encode(cameras).toByteArray(Charsets.UTF_8)))
		if (!temporary.renameTo(file)) {
			file.delete()
			temporary.renameTo(file)
		}
	}

	// Read once at start-up on the caller's thread: the file is a few kilobytes at most.
	private fun readSaved(): List<Camera> {
		if (!file.exists()) return emptyList()
		return try {
			CameraListCodec.decode(cipher.decrypt(file.readBytes()).toString(Charsets.UTF_8))
		} catch (exception: GeneralSecurityException) {
			// The Keystore key is gone (app data cleared or restored to another device), so the list cannot be read.
			Log.w(TAG, "Saved cameras could not be decrypted, starting with none", exception)
			emptyList()
		}
	}

	private companion object {
		const val FILE_NAME = "cameras.bin"
		const val TAG = "Perch"
	}
}
