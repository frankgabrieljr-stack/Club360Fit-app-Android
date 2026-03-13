package com.club360fit.app.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.club360fit.app.data.MealPlanDto
import com.club360fit.app.data.ProgressCheckInDto
import com.club360fit.app.data.ProgressRepository
import com.club360fit.app.data.WorkoutPlanDto
import com.club360fit.app.ui.theme.BurgundyPrimary
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun ClientHomeScreen(
    onSignOut: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: ClientHomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    var showWorkoutDialog by remember { mutableStateOf(false) }
    var showMealDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var selectedWorkoutPlan by remember { mutableStateOf<WorkoutPlanDto?>(null) }
    var selectedMealPlan by remember { mutableStateOf<MealPlanDto?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Client Home",
                style = MaterialTheme.typography.headlineLarge,
                color = BurgundyPrimary
            )
            IconButton(onClick = onOpenProfile) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "My profile",
                    tint = BurgundyPrimary
                )
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BurgundyPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Next session", style = MaterialTheme.typography.titleMedium, color = BurgundyPrimary)
                        val next = state.nextSession
                        if (next == null) {
                            Text("No upcoming sessions scheduled.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("${next.date} at ${next.time}", style = MaterialTheme.typography.bodyLarge)
                            if (next.notes.isNotBlank()) {
                                Text(next.notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.workoutPlan != null) {
                            selectedWorkoutPlan = state.workoutPlan
                            showWorkoutDialog = true
                        }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("This week's workout", style = MaterialTheme.typography.titleMedium, color = BurgundyPrimary)
                        Text(
                            text = state.workoutPlan?.title ?: "No workout plan assigned yet.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (state.workoutPlan != null) {
                            Text(
                                text = "Tap to view full details",
                                style = MaterialTheme.typography.labelSmall,
                                color = BurgundyPrimary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.mealPlan != null) {
                            selectedMealPlan = state.mealPlan
                            showMealDialog = true
                        }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("This week's meals", style = MaterialTheme.typography.titleMedium, color = BurgundyPrimary)
                        Text(
                            text = state.mealPlan?.title ?: "No meal plan assigned yet.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (state.mealPlan != null) {
                            Text(
                                text = "Tap to view full details",
                                style = MaterialTheme.typography.labelSmall,
                                color = BurgundyPrimary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Plan history",
                    style = MaterialTheme.typography.titleMedium,
                    color = BurgundyPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (state.workoutPlans.isNotEmpty()) {
                    Text("Workouts", style = MaterialTheme.typography.labelLarge)
                    state.workoutPlans.forEach { plan ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedWorkoutPlan = plan
                                    showWorkoutDialog = true
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Week of ${plan.weekStart} – ${plan.title}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                if (state.mealPlans.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Meals", style = MaterialTheme.typography.labelLarge)
                    state.mealPlans.forEach { plan ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedMealPlan = plan
                                    showMealDialog = true
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Week of ${plan.weekStart} – ${plan.title}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                if (state.workoutPlans.isEmpty() && state.mealPlans.isEmpty()) {
                    Text(
                        "No past plans yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your progress",
                    style = MaterialTheme.typography.titleMedium,
                    color = BurgundyPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        if (state.progressCheckIns.isEmpty()) {
                            Text(
                                "No check-ins yet. Log your progress below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            state.progressCheckIns.forEach { checkIn ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${checkIn.checkInDate}", style = MaterialTheme.typography.labelLarge)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            checkIn.weightKg?.let { Text("${it} kg", style = MaterialTheme.typography.bodySmall) }
                                            if (checkIn.workoutDone) Text("Workout ✓", style = MaterialTheme.typography.bodySmall)
                                            if (checkIn.mealsFollowed) Text("Meals ✓", style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (checkIn.notes.isNotBlank()) {
                                            Text(checkIn.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { showProgressDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary.copy(alpha = 0.1f), contentColor = BurgundyPrimary)
                        ) { Text("Log progress") }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Text("Sign out")
                }
            }
        }
    }

    if (showWorkoutDialog && selectedWorkoutPlan != null) {
        val plan = selectedWorkoutPlan!!
        AlertDialog(
            onDismissRequest = { showWorkoutDialog = false; selectedWorkoutPlan = null },
            title = { Text(plan.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Week start: ${plan.weekStart}", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(plan.planText, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showWorkoutDialog = false; selectedWorkoutPlan = null }) {
                    Text("Close", color = BurgundyPrimary)
                }
            }
        )
    }

    if (showMealDialog && selectedMealPlan != null) {
        val plan = selectedMealPlan!!
        AlertDialog(
            onDismissRequest = { showMealDialog = false; selectedMealPlan = null },
            title = { Text(plan.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Week start: ${plan.weekStart}", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(plan.planText, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showMealDialog = false; selectedMealPlan = null }) {
                    Text("Close", color = BurgundyPrimary)
                }
            }
        )
    }

    if (showProgressDialog && state.clientId != null) {
        ClientLogProgressDialog(
            clientId = state.clientId!!,
            onDismiss = { showProgressDialog = false },
            onSaved = { viewModel.loadData() }
        )
    }
}

@Composable
private fun ClientLogProgressDialog(
    clientId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var weightText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var workoutDone by remember { mutableStateOf(false) }
    var mealsFollowed by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log progress") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg, optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    maxLines = 3
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Workout completed?", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = workoutDone, onCheckedChange = { workoutDone = it }, colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Meals followed?", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = mealsFollowed, onCheckedChange = { mealsFollowed = it }, colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary))
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
                    if (date == null) {
                        error = "Enter a valid date."
                        return@TextButton
                    }
                    val weight = weightText.toDoubleOrNull()
                    isSaving = true
                    error = null
                    scope.launch {
                        try {
                            ProgressRepository.addCheckIn(
                                ProgressCheckInDto(
                                    clientId = clientId,
                                    checkInDate = date,
                                    weightKg = weight,
                                    notes = notes,
                                    workoutDone = workoutDone,
                                    mealsFollowed = mealsFollowed
                                )
                            )
                            isSaving = false
                            onSaved()
                            onDismiss()
                        } catch (e: Exception) {
                            isSaving = false
                            error = e.message ?: "Failed to save"
                        }
                    }
                }
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(16.dp))
                else Text("Save", color = BurgundyPrimary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun Box(modifier: Modifier, contentAlignment: Alignment, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = modifier, contentAlignment = contentAlignment) {
        content()
    }
}
