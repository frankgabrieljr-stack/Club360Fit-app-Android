package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.fromFeetInches
import com.club360fit.app.ui.utils.fromPounds
import com.club360fit.app.ui.utils.toFeetInches
import com.club360fit.app.ui.utils.toPounds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProfileScreen(
    clientId: String?,
    onBack: () -> Unit
) {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ClientProfileViewModel(clientId) as T
        }
    }
    val viewModel: ClientProfileViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (clientId == null) "New Client" else "Client Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = BurgundyPrimary,
                    navigationIconContentColor = BurgundyPrimary
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BurgundyPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                OutlinedTextField(
                    value = state.client.fullName ?: "",
                    onValueChange = viewModel::updateName,
                    label = { Text("Full name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.client.age?.toString() ?: "",
                        onValueChange = { viewModel.updateAge(it.toIntOrNull()) },
                        label = { Text("Age") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    // Imperial Height
                    val heightCm = state.client.heightCm ?: 0
                    val (feet, inches) = heightCm.toFeetInches()
                    var feetText by remember(heightCm) { mutableStateOf(if (feet > 0) feet.toString() else "") }
                    var inchesText by remember(heightCm) { mutableStateOf(if (inches > 0) inches.toString() else "") }

                    OutlinedTextField(
                        value = feetText,
                        onValueChange = {
                            feetText = it.filter(Char::isDigit)
                            val f = feetText.toIntOrNull() ?: 0
                            val i = inchesText.toIntOrNull() ?: 0
                            viewModel.updateHeight(fromFeetInches(f, i))
                        },
                        label = { Text("Ht (ft)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inchesText,
                        onValueChange = {
                            inchesText = it.filter(Char::isDigit)
                            val f = feetText.toIntOrNull() ?: 0
                            val i = inchesText.toIntOrNull() ?: 0
                            viewModel.updateHeight(fromFeetInches(f, i))
                        },
                        label = { Text("Ht (in)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Imperial Weight
                val weightKg = state.client.weightKg ?: 0
                var weightLbsText by remember(weightKg) {
                    mutableStateOf(if (weightKg > 0) weightKg.toPounds().toString() else "")
                }
                
                OutlinedTextField(
                    value = weightLbsText,
                    onValueChange = {
                        weightLbsText = it.filter(Char::isDigit)
                        val lbs = weightLbsText.toIntOrNull()
                        viewModel.updateWeight(lbs?.let { fromPounds(it) })
                    },
                    label = { Text("Weight (lbs)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.client.goal ?: "",
                    onValueChange = viewModel::updateGoal,
                    label = { Text("Goal") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "View privileges",
                    style = MaterialTheme.typography.titleMedium,
                    color = BurgundyPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                PrivSwitchRow(
                    label = "Nutrition plans",
                    checked = state.client.canViewNutrition,
                    onChecked = { viewModel.updatePrivilege(nutrition = it) }
                )
                PrivSwitchRow(
                    label = "Workout programs",
                    checked = state.client.canViewWorkouts,
                    onChecked = { viewModel.updatePrivilege(workouts = it) }
                )
                PrivSwitchRow(
                    label = "Payments",
                    checked = state.client.canViewPayments,
                    onChecked = { viewModel.updatePrivilege(payments = it) }
                )
                PrivSwitchRow(
                    label = "Run club events",
                    checked = state.client.canViewEvents,
                    onChecked = { viewModel.updatePrivilege(events = it) }
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Admin access", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = state.isAdmin,
                        onCheckedChange = viewModel::setAdmin,
                        colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary)
                    )
                }
                state.error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BurgundyPrimary)
                    ) { Text("Cancel") }
                    Button(
                        onClick = { viewModel.save(onDone = onBack) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BurgundyPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun PrivSwitchRow(
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked, 
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary)
        )
    }
}
