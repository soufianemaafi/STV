package com.example.soukitv.features.home.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.soukitv.R

@Composable
fun StvErrorDialog(
    message: String,
    installRequired: Boolean,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (installRequired) {
                        R.string.dialog_stv_required_title
                    } else {
                        R.string.dialog_error_title
                    }
                )
            )
        },
        text = { Text(text = message) },
        confirmButton = {
            Button(onClick = if (installRequired) onInstall else onRetry) {
                Text(
                    text = stringResource(
                        if (installRequired) {
                            R.string.action_install_stv
                        } else {
                            R.string.action_retry
                        }
                    )
                )
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}

