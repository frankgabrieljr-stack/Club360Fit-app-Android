package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.club360fit.app.data.AdherenceMetricsRepository
import com.club360fit.app.data.ClientDto
import com.club360fit.app.data.PushRegistrationRepository
import com.club360fit.app.data.ScheduleEvent
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.theme.Club360FitTheme
import com.club360fit.app.ui.screens.gallery.TransformationGalleryScreen
import com.club360fit.app.ui.screens.profile.UserProfileScreen
import com.club360fit.app.ui.utils.buildClientMemberSummaryLine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import com.club360fit.app.ui.utils.toDisplayDate
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    onOpenCoachNotifications: () -> Unit,
    onOpenClientProfile: (String?) -> Unit,
    onOpenClientHub: (String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: AdminHomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val coachUnread by viewModel.coachUnreadCount.collectAsState()
    val scheduleViewModel: ScheduleViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var hubShowSchedule by remember { mutableStateOf(false) }
    var showCoachDirectory by remember { mutableStateOf(false) }
    val assignedClients = state.clients.filter { it.coachId != null }
    val newClients = state.clients.filter { it.coachId == null }
    var moreDestination by remember { mutableStateOf<AdminMoreDestination?>(null) }

    LaunchedEffect(selectedTab) {
        if (selectedTab != 0) hubShowSchedule = false
        if (selectedTab != 5) moreDestination = null
    }

    LaunchedEffect(Unit) {
        PushRegistrationRepository.syncAndroidFcmTokenIfPossible(context)
    }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) viewModel.refreshCoachUnread()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = BurgundyPrimary,
        selectedTextColor = BurgundyPrimary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        floatingActionButton = {
            if (selectedTab == 3) {
                FloatingActionButton(
                    onClick = { onOpenClientProfile(null) },
                    containerColor = BurgundyPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Client")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BurgundyPrimary
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Hub") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("Schedule") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    label = { Text("New clients") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    label = { Text("My clients") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                    label = { Text("Meal inbox") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    icon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                    label = { Text("More") },
                    colors = navItemColors
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> {
                    if (hubShowSchedule) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { hubShowSchedule = false }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to hub",
                                    tint = BurgundyPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Hub", color = BurgundyPrimary)
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "Schedule",
                                style = MaterialTheme.typography.titleLarge,
                                color = BurgundyPrimary,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Coach hub",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = BurgundyPrimary
                                )
                                Text(
                                    text = "Assignments & schedule",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BurgundyPrimary.copy(alpha = 0.85f)
                                )
                            }
                            BadgedBox(
                                badge = {
                                    if (coachUnread > 0) {
                                        Badge { Text("${coachUnread.coerceAtMost(99)}") }
                                    }
                                }
                            ) {
                                IconButton(onClick = onOpenCoachNotifications) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Coach updates",
                                        tint = BurgundyPrimary
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Schedule",
                                style = MaterialTheme.typography.headlineLarge,
                                color = BurgundyPrimary
                            )
                            Text(
                                text = "Day and week views",
                                style = MaterialTheme.typography.titleMedium,
                                color = BurgundyPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
                2 -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "New clients",
                                style = MaterialTheme.typography.headlineLarge,
                                color = BurgundyPrimary
                            )
                            Text(
                                text = "Review intake and claim members",
                                style = MaterialTheme.typography.titleMedium,
                                color = BurgundyPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
                3 -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "My clients",
                                style = MaterialTheme.typography.headlineLarge,
                                color = BurgundyPrimary
                            )
                            Text(
                                text = "Plans, meals, progress",
                                style = MaterialTheme.typography.titleMedium,
                                color = BurgundyPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
                4 -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Meal inbox",
                                style = MaterialTheme.typography.headlineLarge,
                                color = BurgundyPrimary
                            )
                            Text(
                                text = "Review meal photos from your assigned clients",
                                style = MaterialTheme.typography.titleMedium,
                                color = BurgundyPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
                5 -> {
                    if (moreDestination == null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "More",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = BurgundyPrimary
                                )
                                Text(
                                    text = "Gallery and profile",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BurgundyPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        if (hubShowSchedule) {
                            ScheduleTab(clients = assignedClients, viewModel = scheduleViewModel)
                        } else {
                            OverviewTab(
                                clients = assignedClients,
                                scheduleViewModel = scheduleViewModel,
                                onGoToClients = { selectedTab = 3 },
                                onGoToSchedule = { focusDate ->
                                    focusDate?.let { scheduleViewModel.jumpToDate(it) }
                                    hubShowSchedule = true
                                }
                            )
                        }
                    }
                    1 -> AdminScheduleOptionsTab(
                        clients = assignedClients,
                        viewModel = scheduleViewModel
                    )
                    2 -> NewClientsTab(
                        clients = newClients,
                        profileRolesByUserId = state.profileRolesByUserId,
                        onClaim = viewModel::claimClient
                    )
                    3 -> ClientsTab(
                        viewModel = viewModel,
                        onOpenProfile = onOpenClientProfile
                    )
                    4 -> CoachMealPhotoInboxScreen(
                        clients = assignedClients,
                        onOpenClientHub = { clientId, _ -> onOpenClientHub(clientId) }
                    )
                    5 -> when (moreDestination) {
                        AdminMoreDestination.Gallery -> TransformationGalleryScreen(
                            onBack = { moreDestination = null },
                            showTopBarBack = true
                        )
                        AdminMoreDestination.Profile -> UserProfileScreen(
                            onBack = { moreDestination = null },
                            onEditProfile = {},
                            onSignOut = onSignOut,
                            showTopBarBack = true,
                            onOpenCoachDirectory = { showCoachDirectory = true }
                        )
                        null -> AdminMoreTab(
                            onOpenGallery = { moreDestination = AdminMoreDestination.Gallery },
                            onOpenProfile = { moreDestination = AdminMoreDestination.Profile }
                        )
                    }
                }
            }
        }
    }
    if (showCoachDirectory) {
        CoachDirectoryScreen(
            onBack = { showCoachDirectory = false },
            modifier = Modifier.fillMaxSize()
        )
    }
    }
}

private enum class AdminMoreDestination {
    Gallery,
    Profile
}

private enum class AdminScheduleQuickView {
    Menu,
    Day,
    Week,
    Calendar
}

@Composable
private fun AdminScheduleOptionsTab(
    clients: List<ClientDto>,
    viewModel: ScheduleViewModel
) {
    val state by viewModel.uiState.collectAsState()
    var selectedView by remember { mutableStateOf(AdminScheduleQuickView.Menu) }
    val today = LocalDate.now()
    val weekStart = today.with(DayOfWeek.SUNDAY)
    val weekEnd = weekStart.plusDays(6)
    val assignedIds = remember(clients) { clients.mapNotNull { it.id }.toSet() }
    val clientNameById = remember(clients) {
        clients.mapNotNull { client ->
            val id = client.id ?: return@mapNotNull null
            id to (client.fullName?.takeIf { it.isNotBlank() } ?: "(no name)")
        }.toMap()
    }
    val scopedEvents = remember(state.events, assignedIds) {
        state.events.filter { event -> event.clientId?.let { it in assignedIds } == true }
            .sortedWith(compareBy<ScheduleEvent> { it.date }.thenBy { it.time })
    }
    val dayEvents = remember(scopedEvents, today) {
        scopedEvents.filter { it.date == today }
    }
    val weekEvents = remember(scopedEvents, weekStart, weekEnd) {
        scopedEvents.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) }
    }

    when (selectedView) {
        AdminScheduleQuickView.Menu -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminScheduleOptionCard(
                    title = "Schedule for the day",
                    subtitle = if (dayEvents.isEmpty()) {
                        "No sessions today"
                    } else {
                        "${dayEvents.size} session${if (dayEvents.size == 1) "" else "s"} today"
                    },
                    onClick = { selectedView = AdminScheduleQuickView.Day }
                )
                AdminScheduleOptionCard(
                    title = "Schedule for the Week",
                    subtitle = "${weekEvents.size} session${if (weekEvents.size == 1) "" else "s"} this week",
                    onClick = { selectedView = AdminScheduleQuickView.Week }
                )
                AdminScheduleOptionCard(
                    title = "Calendar view",
                    subtitle = "Month view, date picker, and add events",
                    onClick = { selectedView = AdminScheduleQuickView.Calendar }
                )
            }
        }
        AdminScheduleQuickView.Day -> AdminScheduleEventList(
            title = "Schedule for the day",
            subtitle = today.toDisplayDate(),
            emptyText = "No sessions today.",
            events = dayEvents,
            clientNameById = clientNameById,
            onBack = { selectedView = AdminScheduleQuickView.Menu }
        )
        AdminScheduleQuickView.Week -> AdminScheduleEventList(
            title = "Schedule for the Week",
            subtitle = "${weekStart.toDisplayDate()} - ${weekEnd.toDisplayDate()}",
            emptyText = "No sessions this week.",
            events = weekEvents,
            clientNameById = clientNameById,
            onBack = { selectedView = AdminScheduleQuickView.Menu }
        )
        AdminScheduleQuickView.Calendar -> Column(modifier = Modifier.fillMaxSize()) {
            TextButton(
                onClick = { selectedView = AdminScheduleQuickView.Menu },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to schedule",
                    tint = BurgundyPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Schedule", color = BurgundyPrimary)
            }
            ScheduleTab(clients = clients, viewModel = viewModel)
        }
    }
}

@Composable
private fun AdminScheduleOptionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = BurgundyPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdminScheduleEventList(
    title: String,
    subtitle: String,
    emptyText: String,
    events: List<ScheduleEvent>,
    clientNameById: Map<String, String>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to schedule",
                tint = BurgundyPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Schedule", color = BurgundyPrimary)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = BurgundyPrimary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (events.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = emptyText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = { it.id ?: "${it.date}-${it.time}-${it.title}" }) { event ->
                    AdminScheduleEventSummaryCard(
                        event = event,
                        clientName = event.clientId?.let { clientNameById[it] }.orEmpty()
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminScheduleEventSummaryCard(
    event: ScheduleEvent,
    clientName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isPastDue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.time.ifBlank { event.date.toDisplayDate() },
                    style = MaterialTheme.typography.labelMedium,
                    color = BurgundyPrimary
                )
                if (event.time.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = event.date.toDisplayDate(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                if (event.isCompleted) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Done") }
                    )
                }
            }
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (event.isPastDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (clientName.isNotBlank()) {
                Text(
                    text = clientName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (event.notes.isNotBlank()) {
                Text(
                    text = event.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            if (event.isPastDue) {
                Text(
                    text = "Past due",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AdminMoreTab(
    onOpenGallery: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AdminMoreOptionCard(
            title = "Gallery",
            subtitle = "Transformation gallery",
            icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = BurgundyPrimary) },
            onClick = onOpenGallery
        )
        AdminMoreOptionCard(
            title = "Profile",
            subtitle = "Account settings and sign out",
            icon = { Icon(Icons.Default.Person, contentDescription = null, tint = BurgundyPrimary) },
            onClick = onOpenProfile
        )
    }
}

@Composable
private fun AdminMoreOptionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewTab(
    clients: List<ClientDto>,
    scheduleViewModel: ScheduleViewModel,
    onGoToClients: () -> Unit,
    onGoToSchedule: (focusDate: LocalDate?) -> Unit
) {
    var complianceByClient by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    LaunchedEffect(clients.map { it.id }.toString()) {
        val map = mutableMapOf<String, Int>()
        for (c in clients) {
            val id = c.id ?: continue
            runCatching { AdherenceMetricsRepository.loadSnapshotForCoachView(id) }
                .getOrNull()
                ?.let { map[id] = it.weeklyComplianceScore }
        }
        complianceByClient = map
    }

    val scheduleState by scheduleViewModel.uiState.collectAsState()
    val events = scheduleState.events
    val today = LocalDate.now()

    val startOfWeek = today.with(DayOfWeek.SUNDAY)
    val endOfWeek = startOfWeek.plusDays(6)

    val activeClients = clients.size
    val sessionsThisWeek = events.count { !it.date.isBefore(startOfWeek) && !it.date.isAfter(endOfWeek) }
    val completedThisWeek = events.count { it.isCompleted && !it.date.isBefore(startOfWeek) && !it.date.isAfter(endOfWeek) }
    val pastDueSessions = events.count { !it.isCompleted && it.date.isBefore(today) }

    val weekSessionEvents = events.filter { !it.date.isBefore(startOfWeek) && !it.date.isAfter(endOfWeek) }
    val firstSessionDateThisWeek = weekSessionEvents.minByOrNull { it.date }?.date
    val completedWeekEvents = events.filter {
        it.isCompleted && !it.date.isBefore(startOfWeek) && !it.date.isAfter(endOfWeek)
    }
    val firstCompletedDateThisWeek = completedWeekEvents.minByOrNull { it.date }?.date
    val pastDueEvents = events.filter { !it.isCompleted && it.date.isBefore(today) }
    val earliestPastDueDate = pastDueEvents.minByOrNull { it.date }?.date

    val todayEvents = events
        .filter { it.date == today }
        .sortedBy { it.time }

    val clientById = clients.associateBy { it.id }
    val twoWeeksAgo = today.minusDays(14)
    val atRiskClients = clients.filter { client ->
        val id = client.id ?: return@filter false
        val recentEvents = events.any { it.clientId == id && !it.date.isBefore(twoWeeksAgo) }
        val noSessions = !recentEvents
        val lowCompliance = (complianceByClient[id] ?: 100) < 40
        noSessions || lowCompliance
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // KPI cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewStatCard(
                title = "Active clients",
                value = activeClients.toString(),
                modifier = Modifier.weight(1f),
                onClick = onGoToClients
            )
            OverviewStatCard(
                title = "Sessions this week",
                value = sessionsThisWeek.toString(),
                modifier = Modifier.weight(1f),
                onClick = {
                    val d = firstSessionDateThisWeek
                        ?: if (!today.isBefore(startOfWeek) && !today.isAfter(endOfWeek)) today else startOfWeek
                    onGoToSchedule(d)
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewStatCard(
                title = "Completed this week",
                value = completedThisWeek.toString(),
                modifier = Modifier.weight(1f),
                onClick = {
                    val d = firstCompletedDateThisWeek ?: today.coerceIn(startOfWeek, endOfWeek)
                    onGoToSchedule(d)
                }
            )
            OverviewStatCard(
                title = "Past-due sessions",
                value = pastDueSessions.toString(),
                modifier = Modifier.weight(1f),
                emphasize = pastDueSessions > 0,
                onClick = {
                    val d = earliestPastDueDate ?: today
                    onGoToSchedule(d)
                }
            )
        }

        // Today's schedule
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Today",
            style = MaterialTheme.typography.titleMedium,
            color = BurgundyPrimary
        )
        if (todayEvents.isEmpty()) {
            Text(
                text = "No sessions scheduled today.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(todayEvents, key = { it.id ?: it.hashCode().toString() }) { event ->
                    val clientName = event.clientId?.let { id -> clientById[id]?.fullName } ?: ""
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(event.title, style = MaterialTheme.typography.titleSmall)
                            if (event.time.isNotBlank()) {
                                Text(event.time, style = MaterialTheme.typography.bodySmall)
                            }
                            if (clientName.isNotBlank()) {
                                Text(
                                    text = clientName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // At-risk clients
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "At-risk clients",
            style = MaterialTheme.typography.titleMedium,
            color = BurgundyPrimary
        )
        if (atRiskClients.isEmpty()) {
            Text(
                text = "No clients flagged as at-risk in the last 14 days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(atRiskClients, key = { it.id ?: it.userId }) { client ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = client.fullName.orEmpty().ifBlank { "(no name)" },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            val cid = client.id
                            val score = cid?.let { complianceByClient[it] } ?: 100
                            val hasRecent = cid != null &&
                                events.any { it.clientId == cid && !it.date.isBefore(twoWeeksAgo) }
                            val reason = buildString {
                                if (!hasRecent) append("No sessions in the last 14 days.")
                                if (score < 40) {
                                    if (isNotEmpty()) append(" ")
                                    append("Compliance under 40%.")
                                }
                            }.ifBlank { "Needs attention." }
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (emphasize) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = if (emphasize) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = if (emphasize) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsTab(
    viewModel: AdminHomeViewModel = viewModel(),
    onOpenProfile: (String?) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val assignedClients = state.clients.filter { it.coachId != null }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BurgundyPrimary)
                }
            }
            state.error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.error ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadClients() }) {
                        Text("Retry")
                    }
                }
            }
            assignedClients.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No assigned clients yet. Check New clients to claim new signups.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(assignedClients, key = { it.id ?: it.userId }) { client ->
                        ClientCard(
                            fullName = client.fullName ?: "(no name)",
                            goal = client.goal ?: "",
                            lastActive = client.lastActive ?: "Never",
                            subtitle = "Plans, meals, progress",
                            platformRole = state.profileRolesByUserId[client.userId],
                            age = client.age,
                            heightCm = client.heightCm,
                            weightKg = client.weightKg,
                            onClick = { client.id?.let { onOpenProfile(it) } },
                            onDelete = { client.id?.let { viewModel.deleteClient(it) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewClientsTab(
    clients: List<ClientDto>,
    profileRolesByUserId: Map<String, String>,
    onClaim: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (clients.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No new clients waiting to be claimed.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(clients, key = { it.id ?: it.userId }) { client ->
                NewClientCard(
                    client = client,
                    platformRole = profileRolesByUserId[client.userId],
                    onClaim = {
                        client.id?.let(onClaim)
                    }
                )
            }
        }
    }
}

@Composable
private fun NewClientCard(
    client: ClientDto,
    platformRole: String?,
    onClaim: () -> Unit
) {
    val memberSummary = buildClientMemberSummaryLine(
        client.age,
        client.heightCm,
        client.weightKg,
        client.goal.orEmpty()
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = client.fullName ?: "(no name)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            platformRole?.let { raw ->
                Text(
                    text = if (raw.equals("admin", ignoreCase = true)) "App login: Admin" else "App login: Client",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (raw.equals("admin", ignoreCase = true)) BurgundyPrimary.copy(alpha = 0.9f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (memberSummary.isBlank()) {
                    "No age, height, weight, or goal on file yet."
                } else {
                    memberSummary
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onClaim,
                enabled = client.id != null,
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Claim client")
            }
        }
    }
}

@Composable
fun ClientCard(
    fullName: String,
    goal: String,
    lastActive: String,
    subtitle: String = "Plans, meals, progress",
    /** `public.profiles.role` for this row’s `user_id` (`admin` / `client`). */
    platformRole: String? = null,
    age: Int? = null,
    heightCm: Int? = null,
    weightKg: Int? = null,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Client") },
            text = { Text("Remove $fullName? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                platformRole?.let { raw ->
                    val label = if (raw.equals("admin", ignoreCase = true)) {
                        "App login: Admin"
                    } else {
                        "App login: Client"
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (raw.equals("admin", ignoreCase = true)) BurgundyPrimary.copy(alpha = 0.9f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val memberSummary = buildClientMemberSummaryLine(age, heightCm, weightKg, goal)
                if (memberSummary.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = memberSummary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScheduleTab(
    clients: List<ClientDto>,
    viewModel: ScheduleViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scheduleSnackbar by viewModel.snackbarMessage.collectAsState()
    val scheduleSnackbarIsError by viewModel.snackbarIsError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(scheduleSnackbar) {
        val msg = scheduleSnackbar ?: return@LaunchedEffect
        val isErr = scheduleSnackbarIsError
        viewModel.clearScheduleSnackbar()
        snackbarHostState.showSnackbar(
            msg,
            duration = if (isErr) SnackbarDuration.Long else SnackbarDuration.Short
        )
    }

    val month = state.currentMonth
    val startOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val today = LocalDate.now()

    val firstDayOfWeekIndex = startOfMonth.dayOfWeek.value % 7  // American: Sunday = 0
    val totalCells = firstDayOfWeekIndex + daysInMonth
    val rows = ceil(totalCells / 7f).toInt()

    Box(modifier = Modifier.fillMaxSize()) {
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.previousMonth() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month", tint = BurgundyPrimary)
            }
            Text(
                text = month.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + month.year,
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { viewModel.nextMonth() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month", tint = BurgundyPrimary)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // American: S M T W T F S
        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { d ->
                Text(d, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
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
                        val dayDate = if (showDay) month.atDay(dayCounter) else null
                        val hasEvents = dayDate != null && state.events.any { it.date == dayDate }
                        val isSelected = dayDate == state.selectedDate
                        val isToday = dayDate == today

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .then(
                                    if (showDay) Modifier
                                        .clickable { viewModel.selectDate(dayDate) }
                                        .border(
                                            if (isSelected) 2.dp else 0.dp,
                                            BurgundyPrimary,
                                            CircleShape
                                        )
                                        .background(
                                            if (isToday) BurgundyPrimary.copy(alpha = 0.15f)
                                            else androidx.compose.ui.graphics.Color.Transparent,
                                            CircleShape
                                        )
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (showDay) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(dayCounter.toString(), style = MaterialTheme.typography.bodyMedium)
                                    if (hasEvents) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier.size(6.dp).background(BurgundyPrimary, CircleShape)
                                        )
                                    }
                                }
                                dayCounter++
                            }
                        }
                    }
                }
            }
        }

        state.selectedDate?.let { selected ->
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.openAddEventDialog(selected) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BurgundyPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add event on ${selected.month.value}/${selected.dayOfMonth}")
            }
        }

        if (state.eventsForSelectedDate.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Events on ${state.selectedDate?.month?.value}/${state.selectedDate?.dayOfMonth}",
                style = MaterialTheme.typography.labelLarge,
                color = BurgundyPrimary
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.eventsForSelectedDate, key = { it.id.orEmpty() }) { event ->
                    ScheduleEventCard(
                        event = event,
                        onMarkDone = { viewModel.markCompleted(event) },
                        onDelete = { event.id?.let { viewModel.deleteEvent(it) } }
                    )
                }
            }
        }

        if (state.events.isEmpty() && state.selectedDate == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tap a date to add events. Notifications for upcoming and past-due sessions coming next.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    if (state.showAddEventDialog && state.addEventDate != null) {
        AddScheduleEventDialog(
            date = state.addEventDate!!,
            clients = clients,
            onDismiss = { viewModel.dismissAddEventDialog() },
            onSave = { events ->
                viewModel.addEvents(events)
            }
        )
    }
}

@Composable
private fun ScheduleEventCard(
    event: ScheduleEvent,
    onMarkDone: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isPastDue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (event.isPastDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if (event.time.isNotBlank()) Text(event.time, style = MaterialTheme.typography.bodySmall)
                if (event.notes.isNotBlank()) Text(event.notes, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                if (event.isPastDue) Text("Past due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            if (!event.isCompleted) {
                TextButton(onClick = onMarkDone) { Text("Done") }
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AddScheduleEventDialog(
    date: LocalDate,
    clients: List<ClientDto>,
    onDismiss: () -> Unit,
    onSave: (events: List<ScheduleEvent>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedClientName by remember { mutableStateOf("") }
    var selectedClientId by remember { mutableStateOf<String?>(null) }
    var repeatWeekly by remember { mutableStateOf(false) }
    var weeksText by remember { mutableStateOf("2") }
    var daysOfWeekSelected by remember {
        mutableStateOf(
            mutableMapOf<DayOfWeek, Boolean>().apply {
                DayOfWeek.values().forEach { put(it, false) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New event — ${date.toDisplayDate()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (e.g. 10:00 AM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                // Client selector
                if (clients.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedClientName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assign to client (optional)") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            clients.forEach { client ->
                                DropdownMenuItem(
                                    text = { Text(client.fullName ?: "(no name)") },
                                    onClick = {
                                        selectedClientName = client.fullName ?: "(no name)"
                                        selectedClientId = client.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Simple weekly recurrence pattern
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Repeat weekly?", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = repeatWeekly,
                        onCheckedChange = { repeatWeekly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary)
                    )
                }
                if (repeatWeekly) {
                    Text(
                        "Select days of week and number of weeks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val labels = listOf(
                        DayOfWeek.SUNDAY to "S",
                        DayOfWeek.MONDAY to "M",
                        DayOfWeek.TUESDAY to "T",
                        DayOfWeek.WEDNESDAY to "W",
                        DayOfWeek.THURSDAY to "T",
                        DayOfWeek.FRIDAY to "F",
                        DayOfWeek.SATURDAY to "S"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        labels.forEach { (dow, label) ->
                            val selected = daysOfWeekSelected[dow] == true
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    daysOfWeekSelected = daysOfWeekSelected.toMutableMap().apply {
                                        this[dow] = !(this[dow] ?: false)
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = weeksText,
                        onValueChange = { weeksText = it.filter(Char::isDigit) },
                        label = { Text("Number of weeks") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) return@TextButton

                val clientId = selectedClientId

                // If repeat is off or no days selected, create a single event on the chosen date.
                val events: List<ScheduleEvent> =
                    if (!repeatWeekly || daysOfWeekSelected.values.none { it }) {
                        listOf(
                            ScheduleEvent(
                                title = title.trim(),
                                date = date,
                                time = time.trim(),
                                notes = notes.trim(),
                                clientId = clientId
                            )
                        )
                    } else {
                        val weeks = weeksText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        val selectedDays = daysOfWeekSelected.filterValues { it }.keys
                        val totalDays = weeks * 7
                        (0 until totalDays).mapNotNull { offset ->
                            val d = date.plusDays(offset.toLong())
                            if (d.dayOfWeek in selectedDays) {
                                ScheduleEvent(
                                    title = title.trim(),
                                    date = d,
                                    time = time.trim(),
                                    notes = notes.trim(),
                                    clientId = clientId
                                )
                            } else null
                        }
                    }

                if (events.isNotEmpty()) {
                    onSave(events)
                }
            }) {
                Text("Save", color = BurgundyPrimary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Preview(showBackground = true)
@Composable
fun AdminHomeScreenPreview() {
    Club360FitTheme {
        AdminHomeScreen(
            onOpenCoachNotifications = {},
            onOpenClientProfile = {},
            onOpenClientHub = {},
            onSignOut = {}
        )
    }
}
