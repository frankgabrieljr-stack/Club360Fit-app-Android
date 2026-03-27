package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.MealPhotoLogDto
import com.club360fit.app.data.MealPhotoRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.SubmitResultMessages
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientMealPhotosScreen(
    clientId: String,
    onBack: () -> Unit
) {
    var logs by remember { mutableStateOf<List<MealPhotoLogDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                logs = MealPhotoRepository.listForClient(clientId)
            } catch (e: Exception) {
                error = e.message ?: "Could not load meal photos"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(clientId) {
        reload()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Client meal photos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BurgundyPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = BurgundyPrimary
                )
            )
        }
    ) { padding ->
        when {
            loading && logs.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BurgundyPrimary)
                }
            }

            error != null && logs.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }

            logs.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No meal photos from this client yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Text(
                            "Add quick feedback so your client knows if portions are too much, too little, or on track.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(logs, key = { it.id ?: it.storagePath }) { item ->
                        MealPhotoReviewCard(
                            clientId = clientId,
                            item = item,
                            onSaved = {
                                reload()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        SubmitResultMessages.SAVED_SUCCESS,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onError = { msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        SubmitResultMessages.failure(msg),
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
