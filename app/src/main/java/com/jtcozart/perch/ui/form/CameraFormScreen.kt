package com.jtcozart.perch.ui.form

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jtcozart.perch.camera.CameraForm
import com.jtcozart.perch.camera.CameraFormValidator
import com.jtcozart.perch.camera.CameraKind
import com.jtcozart.perch.camera.FormProblem
import com.jtcozart.perch.camera.Lens
import com.jtcozart.perch.ui.components.PerchButton
import com.jtcozart.perch.ui.theme.PerchColors

private val fieldWidth = 520.dp

@Composable
fun CameraFormScreen(
	initialForm: CameraForm,
	isNew: Boolean,
	onSave: (CameraForm) -> Unit,
	onCancel: () -> Unit,
) {
	var form by remember { mutableStateOf(initialForm) }
	var problems by remember { mutableStateOf(emptyList<FormProblem>()) }
	val firstField = remember { FocusRequester() }
	val saveButton = remember { FocusRequester() }
	LaunchedEffect(Unit) { runCatching { firstField.requestFocus() } }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState())
			.padding(horizontal = 48.dp, vertical = 28.dp),
		verticalArrangement = Arrangement.spacedBy(14.dp),
	) {
		Text(
			if (isNew) "Add a camera" else "Edit camera",
			style = MaterialTheme.typography.headlineMedium,
			fontWeight = FontWeight.Bold,
			color = PerchColors.Text,
		)
		Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
			PerchButton(
				label = "Tapo camera (RTSP)",
				onClick = { form = form.copy(kind = CameraKind.TapoRtsp) },
				modifier = Modifier.focusRequester(firstField),
				isPrimary = form.kind == CameraKind.TapoRtsp,
			)
			PerchButton(
				label = "Tapo app password",
				onClick = { form = form.copy(kind = CameraKind.TapoLocal) },
				isPrimary = form.kind == CameraKind.TapoLocal,
			)
			PerchButton(
				label = "Stream from a bridge",
				onClick = { form = form.copy(kind = CameraKind.BridgeStream) },
				isPrimary = form.kind == CameraKind.BridgeStream,
			)
		}
		TextInput("Name, such as Driveway", form.name) { form = form.copy(name = it) }

		when (form.kind) {
			CameraKind.TapoRtsp -> TapoFields(form) { form = it }
			CameraKind.TapoLocal -> TapoLocalFields(form) { form = it }
			CameraKind.BridgeStream -> BridgeFields(form) { form = it }
		}

		problems.forEach { Text(it.message, color = PerchColors.Problem) }

		// Arriving at the buttons from a full-width field lands on Save, not whichever button is geometrically nearest
		Row(
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			modifier = Modifier.padding(top = 8.dp).focusRestorer(saveButton).focusGroup(),
		) {
			PerchButton(
				label = "Save",
				modifier = Modifier.focusRequester(saveButton),
				isPrimary = true,
				onClick = {
					problems = CameraFormValidator.problemsWith(form)
					if (problems.isEmpty()) onSave(form)
				},
			)
			PerchButton("Cancel", onCancel)
		}
	}
}

@Composable
private fun TapoFields(form: CameraForm, onChange: (CameraForm) -> Unit) {
	Text(
		"Use the camera account, not your Tapo login. Create it in the Tapo app: open the camera, then " +
			"Settings, Advanced Settings, Camera Account. Battery and solar Tapo models have no RTSP; add those with Tapo app password instead.",
		color = PerchColors.TextMuted,
		modifier = Modifier.width(fieldWidth * 1.6f),
	)
	Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
		TextInput("IP address, such as 192.168.1.40", form.host, keyboardType = KeyboardType.Uri) { onChange(form.copy(host = it)) }
		TextInput("Port", form.port, keyboardType = KeyboardType.Number, width = 140.dp) { onChange(form.copy(port = it)) }
	}
	TextInput("Camera account username", form.username) { onChange(form.copy(username = it)) }
	PasswordInput("Camera account password", form.password) { onChange(form.copy(password = it)) }
	Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
		PerchButton("Main lens", { onChange(form.copy(lens = Lens.Wide)) }, isPrimary = form.lens == Lens.Wide)
		PerchButton("Telephoto lens (dual-lens models)", { onChange(form.copy(lens = Lens.Telephoto)) }, isPrimary = form.lens == Lens.Telephoto)
	}
}

@Composable
private fun TapoLocalFields(form: CameraForm, onChange: (CameraForm) -> Unit) {
	Text(
		"For battery and solar Tapo cameras, and any Tapo camera without a camera account. Perch talks to the camera " +
			"the way the Tapo app does at home, so it signs in with the password of your Tapo app login. The camera " +
			"must be on the same network as this TV, and Third-Party Compatibility must be on in the Tapo app (Me, Third-Party " +
			"Services). Experimental: a camera firmware update can stop it working.",
		color = PerchColors.TextMuted,
		modifier = Modifier.width(fieldWidth * 1.6f),
	)
	TextInput("Camera IP address, such as 192.168.1.50", form.host, keyboardType = KeyboardType.Uri) { onChange(form.copy(host = it)) }
	PasswordInput("Tapo app password", form.password) { onChange(form.copy(password = it)) }
	TextInput("Hub device ID (only for a camera paired to a hub; then enter the hub's address above)", form.hubDeviceId, width = fieldWidth * 1.6f) {
		onChange(form.copy(hubDeviceId = it))
	}
}

@Composable
private fun BridgeFields(form: CameraForm, onChange: (CameraForm) -> Unit) {
	Text(
		"Any RTSP or HLS address another box on your network publishes, for example go2rtc, Home Assistant or an NVR.",
		color = PerchColors.TextMuted,
		modifier = Modifier.width(fieldWidth * 1.6f),
	)
	TextInput("Stream address (full quality)", form.streamUrl, keyboardType = KeyboardType.Uri, width = fieldWidth * 1.6f) {
		onChange(form.copy(streamUrl = it))
	}
	TextInput("Tile stream address (optional, lower quality)", form.subStreamUrl, keyboardType = KeyboardType.Uri, width = fieldWidth * 1.6f) {
		onChange(form.copy(subStreamUrl = it))
	}
}

/** A TV keyboard can capitalize or add a space unseen, so the password can be shown to check what was typed. */
@Composable
private fun PasswordInput(label: String, value: String, onValueChange: (String) -> Unit) {
	var isShown by remember { mutableStateOf(false) }
	Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
		TextInput(label, value, isSecret = !isShown, keyboardType = KeyboardType.Password, onValueChange = onValueChange)
		PerchButton(if (isShown) "Hide password" else "Show password", { isShown = !isShown })
	}
}

@Composable
private fun TextInput(
	label: String,
	value: String,
	isSecret: Boolean = false,
	keyboardType: KeyboardType = KeyboardType.Text,
	width: Dp = fieldWidth,
	onValueChange: (String) -> Unit,
) {
	val focusManager = LocalFocusManager.current
	OutlinedTextField(
		value = value,
		onValueChange = onValueChange,
		label = { Text(label) },
		singleLine = true,
		visualTransformation = if (isSecret) PasswordVisualTransformation() else VisualTransformation.None,
		keyboardOptions = KeyboardOptions(
			keyboardType = if (isSecret) KeyboardType.Password else keyboardType,
			autoCorrectEnabled = !isSecret && keyboardType == KeyboardType.Text,
			imeAction = ImeAction.Next,
		),
		keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
		modifier = Modifier
			.width(width)
			.onPreviewKeyEvent { event -> movesFocusVertically(event, focusManager) },
	)
}

/**
 * A text field keeps the remote's up and down presses to itself, which traps a TV user inside the first field.
 * Single-line fields have no lines to move between, so up and down move to the neighbouring control instead.
 */
private fun movesFocusVertically(event: KeyEvent, focusManager: FocusManager): Boolean {
	if (event.type != KeyEventType.KeyDown) return false
	val direction = when (event.key) {
		Key.DirectionDown -> FocusDirection.Down
		Key.DirectionUp -> FocusDirection.Up
		else -> return false
	}
	return focusManager.moveFocus(direction)
}
