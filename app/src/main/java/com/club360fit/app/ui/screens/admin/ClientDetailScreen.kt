package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.toFeetInches
import com.club360fit.app.ui.utils.formatWeightLbsFromKg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: String,
    onBack: () -> Unit
) {
    val client = findClientDetails(clientId) ?: return
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(client.name) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Text("Client Profile", style = MaterialTheme.typography.headlineSmall, color = BurgundyPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            
            ProfileInfoRow("Email", client.email)
            client.phone?.let { ProfileInfoRow("Phone", it) }
            ProfileInfoRow("Age", client.age?.toString() ?: "N/A")
            
            // Height in Imperial
            val heightDisplay = client.heightCm?.let {
                val (ft, inc) = it.toFeetInches()
                "${ft} ft ${inc} in"
            } ?: "N/A"
            ProfileInfoRow("Height", heightDisplay)
            
            // Weight in Imperial (stored kg → display lbs)
            val weightDisplay = client.weightKg?.let { formatWeightLbsFromKg(it) } ?: "N/A"
            ProfileInfoRow("Weight", weightDisplay)
            
            Spacer(modifier = Modifier.height(16.dp))
            ProfileInfoRow("Overall Goal", client.overallGoal)
            client.medicalConditions?.let { ProfileInfoRow("Medical Conditions", it) }
            client.foodRestrictions?.let { ProfileInfoRow("Food Restrictions", it) }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
            
            Text(
                text = "Paleo Meal Plan",
                style = MaterialTheme.typography.titleLarge,
                color = BurgundyPrimary
            )
            Text(
                text = client.paleoMealPlan.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = client.paleoMealPlan.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            client.paleoMealPlan.mealsPerDay.forEach {
                Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
            
            Text(
                text = "Workout Plan",
                style = MaterialTheme.typography.titleLarge,
                color = BurgundyPrimary
            )
            Text(
                text = client.workoutPlan.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = client.workoutPlan.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            client.workoutPlan.sessionsPerWeek.forEach {
                Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
