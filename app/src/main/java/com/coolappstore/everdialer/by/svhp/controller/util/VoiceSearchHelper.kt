package com.coolappstore.everdialer.by.svhp.controller.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

object VoiceSearchHelper {
    fun createSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search...")
        }
    }

    fun launchVoiceSearch(
        context: Context,
        launcher: androidx.activity.result.ActivityResultLauncher<Intent>
    ) {
        try {
            launcher.launch(createSpeechIntent())
        } catch (_: Exception) {
            Toast.makeText(context, "Voice search not available on this device", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun rememberVoiceSearchLauncher(
    onResult: (String) -> Unit
): androidx.activity.result.ActivityResultLauncher<Intent> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onResult(spokenText.trim())
            }
        }
    }
}
