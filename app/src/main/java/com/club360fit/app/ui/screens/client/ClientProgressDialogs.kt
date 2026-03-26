package com.club360fit.app.ui.screens.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.ProgressCheckInDto
import com.club360fit.app.data.ProgressRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.SubmitResultMessages
import com.club360fit.app.ui.utils.poundsToKg
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun ClientLogProgressDialog(
    clientId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onSubmitResult: (success: Boolean, message: String) -> Unit = { _, _ -> }
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
                    label = { Text("Weight (lbs, optional)") },
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
                    Switch(
                        checked = workoutDone,
                        onCheckedChange = { workoutDone = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Meals followed?", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = mealsFollowed,
                        onCheckedChange = { mealsFollowed = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary)
                    )
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
                    val weightLbs = weightText.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                    if (weightText.trim().isNotEmpty() && weightLbs == null) {
                        error = "Enter a valid weight in pounds."
                        return@TextButton
                    }
                    val weightKg = weightLbs?.let { it.poundsToKg() }
                    isSaving = true
                    error = null
                    scope.launch {
                        try {
                            ProgressRepository.addCheckIn(
                                ProgressCheckInDto(
                                    clientId = clientId,
                                    checkInDate = date,
                                    weightKg = weightKg,
                                    notes = notes,
                                    workoutDone = workoutDone,
                                    mealsFollowed = mealsFollowed
                                )
                            )
                            isSaving = false
                            onSubmitResult(true, SubmitResultMessages.SAVED_SUCCESS)
                            onSaved()
                            onDismiss()
                        } catch (e: Exception) {
                            isSaving = false
                            val msg = e.message ?: "Failed to save"
                            error = msg
                            onSubmitResult(false, SubmitResultMessages.failure(e))
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

