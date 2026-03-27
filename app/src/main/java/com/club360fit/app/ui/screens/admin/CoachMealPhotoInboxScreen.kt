package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.club360fit.app.data.ClientDto
import com.club360fit.app.data.MealPhotoLogDto
import com.club360fit.app.data.MealPhotoRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.SubmitResultMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ClientMealGroup(
    val clientId: String,
    val displayName: String,
    val logs: List<MealPhotoLogDto>
)

/**
 * Coach-wide meal photo feed (newest activity first), aligned with iOS `CoachMealPhotoInboxView`.
 */
@Composable
fun CoachMealPhotoInboxScreen(
    clients: List<ClientDto>,
    onOpenClientHub: (clientId: String, displayTitle: String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var groups by remember { mutableStateOf<List<ClientMealGroup>>(emptyList()) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                val loaded = withContext(Dispatchers.IO) {
                    val out = mutableListOf<ClientMealGroup>()
                    for (c in clients) {
                        val id = c.id ?: continue
                        val logs = MealPhotoRepository.listForClient(id)
                        if (logs.isNotEmpty()) {
                            val name = c.fullName?.trim()?.takeIf { it.isNotEmpty() } ?: "(no name)"
                            out.add(ClientMealGroup(clientId = id, displayName = name, logs = logs))
                        }
                    }
                    out.sortedByDescending { g ->
                        g.logs.maxOfOrNull { it.createdAt ?: "" } ?: ""
                    }
                }
                groups = loaded
            } catch (e: Exception) {
                error = e.message ?: "Could not load meal inbox"
                groups = emptyList()
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(clients.map { it.id }.toString()) {
        reload()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            loading && groups.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BurgundyPrimary)
                }
            }

            error != null && groups.isEmpty() -> {
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

            groups.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No meal photos yet. When clients log meals, they appear here.",
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        Text(
                            "Review photos from all clients in one place.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    items(groups, key = { it.clientId }) { group ->
                        Text(
                            group.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = BurgundyPrimary
                        )
                        Button(
                            onClick = { onOpenClientHub(group.clientId, group.displayName) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text("Open client hub")
                        }
                        group.logs.forEach { log ->
                            MealPhotoReviewCard(
                                clientId = group.clientId,
                                item = log,
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
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}
