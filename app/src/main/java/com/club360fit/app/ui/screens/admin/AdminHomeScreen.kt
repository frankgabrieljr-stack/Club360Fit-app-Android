package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.club360fit.app.data.ClientDto
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.theme.Club360FitTheme
import java.time.LocalDate
import kotlin.math.ceil

@Composable
fun AdminHomeScreen(
    onSignOut: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenClientDetails: (String) -> Unit,
    onOpenClientProfile: (String?) -> Unit,
    viewModel: AdminHomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val tabs = listOf("Overview", "Clients", "Schedule")
    var selectedTab by remember { mutableIntStateOf(1) } // default to Clients

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { onOpenClientProfile(null) },
                    containerColor = BurgundyPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Client")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "Admin Dashboard",
                        style = MaterialTheme.typography.headlineLarge,
                        color = BurgundyPrimary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenProfile) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "My profile",
                            tint = BurgundyPrimary
                        )
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign out",
                            tint = BurgundyPrimary
                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = BurgundyPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BurgundyPrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        selectedContentColor = BurgundyPrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (selectedTab) {
                0 -> OverviewTab()
                1 -> ClientsTab(
                    clients = state.clients,
                    isLoading = state.isLoading,
                    onOpenDetails = onOpenClientDetails,
                    onOpenProfile = onOpenClientProfile
                )
                2 -> ScheduleTab()
            }
        }
    }
}

@Composable
fun OverviewTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Overview",
            style = MaterialTheme.typography.headlineSmall,
            color = BurgundyPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "High-level view of your coaching business. Later we can add metrics like active clients, sessions this week, and overdue payments.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ClientsTab(
    clients: List<ClientDto>,
    isLoading: Boolean,
    onOpenDetails: (String) -> Unit,
    onOpenProfile: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Clients",
            style = MaterialTheme.typography.titleLarge,
            color = BurgundyPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BurgundyPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // First show demo clients
                items(demoClientSummaries, key = { "demo_${it.id}" }) { client ->
                    ClientCard(
                        name = client.name,
                        goal = client.goal,
                        lastActive = client.lastActive,
                        onClick = { onOpenDetails(client.id) }
                    )
                }
                
                // Then show real clients from Supabase
                items(clients, key = { it.id ?: it.userId }) { client ->
                    ClientCard(
                        name = client.fullName ?: "(no name)",
                        goal = client.goal ?: "",
                        lastActive = client.lastActive ?: "Never",
                        onClick = { client.id?.let { onOpenProfile(it) } }
                    )
                }
            }
        }
    }
}

@Composable
fun ClientCard(
    name: String,
    goal: String,
    lastActive: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = goal,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = lastActive,
                style = MaterialTheme.typography.labelSmall,
                color = BurgundyPrimary
            )
        }
    }
}

@Composable
fun ScheduleTab() {
    val today = LocalDate.now()
    val startOfMonth = today.withDayOfMonth(1)
    val daysInMonth = startOfMonth.lengthOfMonth()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Schedules",
            style = MaterialTheme.typography.titleLarge,
            color = BurgundyPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = today.month.name.lowercase().replaceFirstChar { it.uppercase() } +
                " " + today.year,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { d ->
                Text(d, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        val firstDayOfWeekIndex = startOfMonth.dayOfWeek.value - 1
        val totalCells = firstDayOfWeekIndex + daysInMonth
        val rows = ceil(totalCells / 7f).toInt()
        
        Column {
            var dayCounter = 1
            repeat(rows) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(7) { cellIndex ->
                        val currentIndex = rowIndex * 7 + cellIndex
                        val showDay = currentIndex >= firstDayOfWeekIndex && dayCounter <= daysInMonth
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (showDay) {
                                Text(dayCounter.toString(), style = MaterialTheme.typography.bodyMedium)
                                dayCounter++
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Later we’ll attach sessions to dates and show them as markers here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AdminHomeScreenPreview() {
    Club360FitTheme {
        AdminHomeScreen(onSignOut = {}, onOpenProfile = {}, onOpenClientDetails = {}, onOpenClientProfile = {})
    }
}
