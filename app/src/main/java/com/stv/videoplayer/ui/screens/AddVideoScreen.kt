package com.stv.videoplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stv.videoplayer.R
import com.stv.videoplayer.core.security.IntentSecurityManager
import com.stv.videoplayer.core.security.IntentValidationResult
import com.stv.videoplayer.ui.theme.GreenSuccess
import kotlinx.coroutines.launch

/**
 * Ã‰cran formulaire ajout de flux â€” anciennement dans AddVideoActivity.kt
 *
 * @param onSave       Callback quand l'utilisateur sauvegarde (title, url)
 * @param onBackClick  Revenir en arriÃ¨re (navController.popBackStack)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVideoScreen(
    onSave: (String, String) -> Unit,
    onBackClick: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val intentSecurityManager = remember { IntentSecurityManager() }
    val invalidMessage = stringResource(R.string.add_video_invalid)
    val invalidUrlMessage = stringResource(R.string.add_video_invalid_url)

    val msgTitleEmpty = stringResource(R.string.validation_title_empty)
    val msgTitleShort = stringResource(R.string.validation_title_too_short)
    val msgTitleLong = stringResource(R.string.validation_title_too_long)

    fun validateTitle(value: String) {
        titleError = when {
            value.isBlank() -> msgTitleEmpty
            value.length < 2 -> msgTitleShort
            value.length > 100 -> msgTitleLong
            else -> ""
        }
    }

    fun validateUrl(value: String): IntentValidationResult {
        val result = intentSecurityManager.validateVideoUrl(value)
        urlError = when (result) {
            is IntentValidationResult.Valid -> ""
            is IntentValidationResult.Invalid -> invalidUrlMessage
        }
        return result
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_video_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.add_video_header_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.add_video_header_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Champ Titre
            OutlinedTextField(
                value = title,
                onValueChange = { newValue ->
                    title = newValue
                    validateTitle(newValue)
                },
                label = { Text(stringResource(R.string.add_video_label_title)) },
                placeholder = { Text(stringResource(R.string.title_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = titleError.isNotEmpty(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = if (titleError.isEmpty())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                supportingText = {
                    if (titleError.isNotEmpty()) {
                        Text(
                            text = titleError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )

            // Champ URL
            OutlinedTextField(
                value = url,
                onValueChange = { newValue ->
                    url = newValue
                    validateUrl(newValue)
                },
                label = { Text(stringResource(R.string.add_video_label_url)) },
                placeholder = { Text(stringResource(R.string.url_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = urlError.isNotEmpty(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = if (urlError.isEmpty())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                supportingText = {
                    if (urlError.isNotEmpty()) {
                        Text(
                            text = urlError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ã‰tat validation
            if (title.isNotEmpty()) {
                ValidationIndicator(
                    isValid = titleError.isEmpty(),
                    label = stringResource(R.string.add_video_title_valid)
                )
            }
            if (url.isNotEmpty()) {
                ValidationIndicator(
                    isValid = urlError.isEmpty(),
                    label = stringResource(R.string.add_video_url_valid)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bouton Sauvegarder
            Button(
                onClick = {
                    val cleanTitle = title.trim()
                    val cleanUrl = url.trim()

                    validateTitle(cleanTitle)
                    val urlValidationResult = validateUrl(cleanUrl)

                    val isTitleValid = cleanTitle.isNotEmpty() && cleanTitle.length >= 2 && cleanTitle.length <= 100
                    val validatedUrl = when (urlValidationResult) {
                        is IntentValidationResult.Valid -> urlValidationResult.videoUrl
                        is IntentValidationResult.Invalid -> null
                    }
                    val isUrlValid = validatedUrl != null

                    if (isTitleValid && isUrlValid) {
                        onSave(cleanTitle, validatedUrl)
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = invalidMessage
                            )
                        }
                    }
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = title.trim().isNotEmpty() && url.trim().isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.add_video_save))
            }

            // Bouton Annuler
            Button(
                onClick = onBackClick,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = stringResource(R.string.add_video_cancel))
            }
        }
    }
}

@Composable
private fun ValidationIndicator(
    isValid: Boolean,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isValid)
                Icons.Default.CheckCircle
            else
                Icons.Default.Cancel,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isValid) GreenSuccess else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isValid) GreenSuccess else MaterialTheme.colorScheme.error
        )
    }
}


