package com.example.soukitv.features.home.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.soukitv.R
import com.example.soukitv.features.home.presentation.StvPromptType

@Composable
fun StvErrorDialog(
    message: String?,
    promptType: StvPromptType?,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    onRetry: () -> Unit
) {
    val titleRes = when (promptType) {
        StvPromptType.Install -> R.string.dialog_stv_install_title
        StvPromptType.Update -> R.string.dialog_stv_update_title
        null -> R.string.dialog_error_title
    }
    val messageText = when (promptType) {
        StvPromptType.Install -> stringResource(R.string.dialog_stv_install_message)
        StvPromptType.Update -> stringResource(R.string.dialog_stv_update_message)
        null -> message.orEmpty()
    }
    val confirmRes = when (promptType) {
        StvPromptType.Install -> R.string.action_install
        StvPromptType.Update -> R.string.action_update
        null -> R.string.action_retry
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(titleRes))
        },
        text = { Text(text = messageText) },
        confirmButton = {
            Button(onClick = if (promptType != null) onInstall else onRetry) {
                Text(text = stringResource(confirmRes))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}

