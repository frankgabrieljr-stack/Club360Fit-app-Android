package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.ProgressCheckInDto
import com.club360fit.app.data.ProgressRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.toDisplayDate
import com.club360fit.app.ui.utils.formatWeightLbsFromKg
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProgressScreen(
    clientId: String,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var checkIns by remember { mutableStateOf<List<ProgressCheckInDto>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(clientId, refreshKey) {
        isLoading = true
        error = null
        try {
            checkIns = ProgressRepository.getForClient(clientId)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load progress"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BurgundyPrimary)
                }
            } else {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                if (checkIns.isEmpty()) {
                    Text(
                        text = "No check-ins yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    checkIns.forEach { checkIn ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = checkIn.checkInDate.toDisplayDate(),
                                style = MaterialTheme.typography.titleMedium,
                                color = BurgundyPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                formatWeightLbsFromKg(checkIn.weightKg)?.let { label ->
                                    Text(label, style = MaterialTheme.typography.bodySmall)
                                }
                                if (checkIn.workoutDone) Text("Workout ✓", style = MaterialTheme.typography.bodySmall)
                                if (checkIn.mealsFollowed) Text("Meals ✓", style = MaterialTheme.typography.bodySmall)
                            }
                            if (checkIn.notes.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = checkIn.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BurgundyPrimary.copy(alpha = 0.1f),
                        contentColor = BurgundyPrimary
                    )
                ) {
                    Text("Add progress check-in")
                }
            }
        }
    }

    if (showAddDialog) {
        AddProgressCheckInDialog(
            clientId = clientId,
            onDismiss = { showAddDialog = false },
            onSaved = {
                showAddDialog = false
                refreshKey++
            },
            onSubmitResult = { success, message ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message,
                        duration = if (success) SnackbarDuration.Short else SnackbarDuration.Long
                    )
                }
            }
        )
    }
}

