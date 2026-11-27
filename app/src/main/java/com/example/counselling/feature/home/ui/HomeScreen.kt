// Home | Owner: shared (Beh, Jeremy, Michael)
// Permission-driven mobile dashboards. The next session is the one bold
// element for students; everything else is quiet rows and icon shortcuts.
package com.example.counselling.feature.home.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.counselling.core.AppConstants
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.domain.AppointmentStatus
import com.example.counselling.core.domain.User
import com.example.counselling.feature.appointment.domain.Appointment
import com.example.counselling.feature.records.domain.AdminDashboardStats
import com.example.counselling.navigation.Routes
import com.example.counselling.ui.components.AppListRow
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.ScreenTitle
import com.example.counselling.ui.components.SectionLabel
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.components.UserAvatar
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@Composable
fun HomeScreen(
    user: User,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    appointments: List<Appointment> = emptyList(),
    isLoadingAppointments: Boolean = false,
    appointmentError: String? = null,
    onRetryAppointments: () -> Unit = {},
    adminStats: AdminDashboardStats? = null,
    isLoadingStats: Boolean = false,
    statsError: String? = null,
    onRetryStats: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            ScreenTitle(
                title = "${DateTimeUtils.greeting()}, ${user.firstName}",
                subtitle = DateTimeUtils.todayHeading(),
                actions = {
                    IconButton(onClick = { onNavigate(Routes.PROFILE) }) {
                        UserAvatar(
                            initials = user.initials,
                            size = 40.dp,
                            contentDescription = "Open profile",
                        )
                    }
                },
            )
            Spacer(Modifier.height(8.dp))

            when {
                user.permissions.canBookAppointments() -> StudentHome(
                    onNavigate = onNavigate,
                    appointments = appointments,
                    isLoadingAppointments = isLoadingAppointments,
                    appointmentError = appointmentError,
                    onRetryAppointments = onRetryAppointments,
                )
                user.permissions.canViewPrivateSessionNotes() -> CounsellorHome(
                    onNavigate = onNavigate,
                    appointments = appointments,
                    isLoadingAppointments = isLoadingAppointments,
                    appointmentError = appointmentError,
                    onRetryAppointments = onRetryAppointments,
                )
                else -> AdminHome(
                    onNavigate = onNavigate,
                    stats = adminStats,
                    isLoadingStats = isLoadingStats,
                    statsError = statsError,
                    onRetryStats = onRetryStats,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Student home: one hero card, four shortcuts, one quiet urgent-help row.
// ---------------------------------------------------------------------------

@Composable
private fun StudentHome(
    onNavigate: (String) -> Unit,
    appointments: List<Appointment>,
    isLoadingAppointments: Boolean,
    appointmentError: String?,
    onRetryAppointments: () -> Unit,
) {
    val now = Instant.now()
    val upcomingSessions = appointments
        .filter {
            it.status in setOf(AppointmentStatus.Pending, AppointmentStatus.Confirmed) &&
                it.slot.start.isAfter(now)
        }
        .sortedBy { it.slot.start }
    val nextAppointment = upcomingSessions.firstOrNull()
    val laterSessions = upcomingSessions.drop(1).take(3)
    val completedCount = appointments.count { it.status == AppointmentStatus.Completed }

    Column(modifier = Modifier.fillMaxWidth()) {
        when {
            isLoadingAppointments -> LoadingShimmer(
                modifier = Modifier.padding(horizontal = 20.dp),
                rows = 1,
            )

            appointmentError != null -> InfoCard(
                title = "Could not load your appointments",
                body = appointmentError,
                icon = Icons.Outlined.CalendarMonth,
                actionLabel = "Try again",
                onAction = onRetryAppointments,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            nextAppointment != null -> NextSessionCard(
                appointment = nextAppointment,
                onOpenDetails = { onNavigate(Routes.STUDENT_APPOINTMENTS) },
                onReschedule = { onNavigate(Routes.reschedule(nextAppointment.id)) },
            )

            else -> NoUpcomingSessionCard(
                onBook = { onNavigate(Routes.bookAppointment()) },
            )
        }

        Spacer(Modifier.height(18.dp))
        QuickActionRow(
            actions = listOf(
                HomeShortcut(
                    label = "Book",
                    description = "Book a counselling session",
                    icon = Icons.Outlined.CalendarMonth,
                    onClick = { onNavigate(Routes.STUDENT_APPOINTMENTS) },
                ),
                HomeShortcut(
                    label = "Resources",
                    description = "Open self-help resources",
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    onClick = { onNavigate(Routes.RESOURCES) },
                ),
                HomeShortcut(
                    label = "Enquiry",
                    description = "Ask a private question",
                    icon = Icons.Outlined.Forum,
                    onClick = { onNavigate(Routes.ENQUIRIES) },
                ),
                HomeShortcut(
                    label = "Check-in",
                    description = "Start a private wellbeing check-in",
                    icon = Icons.Outlined.FavoriteBorder,
                    onClick = { onNavigate(Routes.SELF_CHECK) },
                ),
            ),
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(16.dp))
        UrgentHelpRow(
            onClick = { onNavigate(Routes.RESOURCES_HOTLINE) },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        if (laterSessions.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionLabel(
                title = "Coming up",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            laterSessions.forEach { session ->
                AppListRow(
                    title = session.counsellorName,
                    supportingText = "${sessionTimeLabel(session.slot.start)} · " +
                        session.concern.label,
                    leadingIcon = Icons.Outlined.CalendarMonth,
                    trailingContent = {
                        StatusChip(status = session.status.wireValue)
                    },
                    onClick = { onNavigate(Routes.STUDENT_APPOINTMENTS) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(
            title = "More",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        AppListRow(
            title = "Session history",
            supportingText = if (completedCount == 0) {
                "Completed sessions will appear here"
            } else {
                "$completedCount completed session${if (completedCount == 1) "" else "s"}"
            },
            leadingIcon = Icons.AutoMirrored.Outlined.EventNote,
            trailingContent = { Chevron() },
            onClick = { onNavigate(Routes.STUDENT_APPOINTMENTS) },
        )
        AppListRow(
            title = "Session summaries",
            supportingText = "Notes shared by your counsellor",
            leadingIcon = Icons.Outlined.Description,
            trailingContent = { Chevron() },
            onClick = { onNavigate(Routes.STUDENT_SUMMARIES) },
        )
        AppListRow(
            title = "Your feedback",
            supportingText = "Rate a completed session",
            leadingIcon = Icons.Outlined.FavoriteBorder,
            trailingContent = { Chevron() },
            onClick = { onNavigate(Routes.STUDENT_FEEDBACK) },
            showDivider = false,
        )
    }
}

@Composable
private fun NextSessionCard(
    appointment: Appointment,
    onOpenDetails: () -> Unit,
    onReschedule: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Next session",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = sessionTimeLabel(appointment.slot.start),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${appointment.counsellorName} · ${appointment.mode.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(status = appointment.status.wireValue)
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onReschedule,
                    enabled = DateTimeUtils.canReschedule(appointment.slotStart),
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Text("Reschedule")
                }
                Button(
                    onClick = onOpenDetails,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Text("Details")
                }
            }
        }
    }
}

@Composable
private fun NoUpcomingSessionCard(onBook: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Next session",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "No upcoming session",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "Choose a counsellor and a time that fits.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
            )
            Button(
                onClick = onBook,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("Book session")
            }
        }
    }
}

@Composable
private fun UrgentHelpRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.SupportAgent,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = "Need urgent help?",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Counsellor home: today's sessions as rows, shortcuts, one follow-up row.
// ---------------------------------------------------------------------------

@Composable
private fun CounsellorHome(
    onNavigate: (String) -> Unit,
    appointments: List<Appointment>,
    isLoadingAppointments: Boolean,
    appointmentError: String?,
    onRetryAppointments: () -> Unit,
) {
    val today = DateTimeUtils.nowInKualaLumpur().toLocalDate()
    val todaysSessions = appointments
        .filter {
            it.status in setOf(AppointmentStatus.Pending, AppointmentStatus.Confirmed) &&
                DateTimeUtils.localDate(it.slotStart) == today
        }
        .sortedBy { it.slot.start }
    val upcomingSessions = appointments
        .filter {
            it.status in setOf(AppointmentStatus.Pending, AppointmentStatus.Confirmed) &&
                DateTimeUtils.localDate(it.slotStart) != today &&
                it.slot.start.isAfter(Instant.now())
        }
        .sortedBy { it.slot.start }
        .take(3)

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(
            title = "Today",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(4.dp))

        when {
            isLoadingAppointments -> LoadingShimmer(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                rows = 2,
            )

            appointmentError != null -> InfoCard(
                title = "Could not load today's sessions",
                body = appointmentError,
                icon = Icons.Outlined.CalendarMonth,
                actionLabel = "Try again",
                onAction = onRetryAppointments,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            todaysSessions.isEmpty() -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "No sessions today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onNavigate(Routes.COUNSELLOR_SCHEDULE) }) {
                    Text("Open schedule")
                }
            }

            else -> todaysSessions.forEach { appointment ->
                AppListRow(
                    title = appointment.studentName,
                    supportingText = appointment.concern.label,
                    leadingContent = {
                        Text(
                            text = DateTimeUtils.formatTime(appointment.slotStart),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingContent = {
                        StatusChip(status = appointment.status.wireValue)
                    },
                    onClick = { onNavigate(Routes.COUNSELLOR_SCHEDULE) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        QuickActionRow(
            actions = listOf(
                HomeShortcut(
                    label = "Schedule",
                    description = "Open my schedule",
                    icon = Icons.Outlined.CalendarMonth,
                    onClick = { onNavigate(Routes.COUNSELLOR_SCHEDULE) },
                ),
                HomeShortcut(
                    label = "Records",
                    description = "Open session records",
                    icon = Icons.Outlined.Description,
                    onClick = { onNavigate(Routes.COUNSELLOR_RECORDS) },
                ),
                HomeShortcut(
                    label = "Inbox",
                    description = "Open the counselling inbox",
                    icon = Icons.Outlined.Forum,
                    onClick = { onNavigate(Routes.ENQUIRIES) },
                ),
                HomeShortcut(
                    label = "Students",
                    description = "Open the student directory",
                    icon = Icons.Outlined.School,
                    onClick = { onNavigate(Routes.STUDENT_DIRECTORY) },
                ),
            ),
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(20.dp))
        AppListRow(
            title = "Follow-ups",
            supportingText = "Overdue and high-risk records",
            leadingIcon = Icons.Outlined.Description,
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            onClick = { onNavigate(Routes.COUNSELLOR_RECORDS) },
            showDivider = upcomingSessions.isEmpty(),
        )

        if (upcomingSessions.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionLabel(
                title = "Upcoming",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            upcomingSessions.forEach { session ->
                AppListRow(
                    title = session.studentName,
                    supportingText = "${sessionTimeLabel(session.slot.start)} · " +
                        session.concern.label,
                    leadingIcon = Icons.Outlined.CalendarMonth,
                    trailingContent = {
                        StatusChip(status = session.status.wireValue)
                    },
                    onClick = { onNavigate(Routes.COUNSELLOR_SCHEDULE) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(
            title = "Manage",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        AppListRow(
            title = "My availability",
            supportingText = "Open and close bookable slots",
            leadingIcon = Icons.Outlined.CalendarMonth,
            trailingContent = { Chevron() },
            onClick = { onNavigate(Routes.MY_AVAILABILITY) },
        )
        AppListRow(
            title = "Student directory",
            supportingText = "Find a student profile",
            leadingIcon = Icons.Outlined.School,
            trailingContent = { Chevron() },
            onClick = { onNavigate(Routes.STUDENT_DIRECTORY) },
        )
        AppListRow(
            title = "Session feedback",
            supportingText = "Read student ratings",
            leadingIcon = Icons.Outlined.FavoriteBorder,
            trailingContent = { Chevron() },
            onClick = { onNavigate(Routes.COUNSELLOR_FEEDBACK) },
            showDivider = false,
        )
    }
}

// ---------------------------------------------------------------------------
// Admin home: two hero numbers, compact stat rows, management rows.
// ---------------------------------------------------------------------------

@Composable
private fun AdminHome(
    onNavigate: (String) -> Unit,
    stats: AdminDashboardStats?,
    isLoadingStats: Boolean,
    statsError: String?,
    onRetryStats: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when {
            isLoadingStats -> LoadingShimmer(
                modifier = Modifier.padding(horizontal = 20.dp),
                rows = 2,
            )

            statsError != null -> InfoCard(
                title = "Could not load statistics",
                body = statsError,
                actionLabel = "Try again",
                onAction = onRetryStats,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            stats != null -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HeroNumber(
                        label = "Upcoming sessions",
                        value = stats.upcomingAppointments.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    HeroNumber(
                        label = "Average rating",
                        value = if (stats.feedbackCount == 0) {
                            "—"
                        } else {
                            String.format(Locale.US, "%.1f", stats.averageRating)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(12.dp))
                StatRow(
                    title = "Students",
                    value = "${stats.activeStudents}/${stats.totalStudents}",
                    supportingText = "Active / total",
                )
                StatRow(
                    title = "Counsellors",
                    value = "${stats.activeCounsellors}/${stats.totalCounsellors}",
                    supportingText = "Active / total",
                )
                StatRow(title = "Completed", value = stats.completedAppointments.toString())
                StatRow(title = "Session records", value = stats.sessionRecords.toString())
                StatRow(title = "Resources", value = stats.publishedResources.toString())
                StatRow(
                    title = "Feedback ratings",
                    value = stats.feedbackCount.toString(),
                    showDivider = false,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel(
            title = "Manage",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(4.dp))
        AppListRow(
            title = "Counsellors",
            supportingText = "Directory and availability",
            leadingIcon = Icons.Outlined.Groups,
            trailingContent = { Chevron() },
            onClick = { onNavigate(Routes.ADMIN_COUNSELLORS) },
        )
        AppListRow(
            title = "Students",
            supportingText = "Search student profiles",
            leadingIcon = Icons.Outlined.School,
            trailingContent = { Chevron() },
            onClick = { onNavigate(Routes.ADMIN_STUDENTS) },
        )
        AppListRow(
            title = "Resources",
            supportingText = "Publish self-help content",
            leadingIcon = Icons.AutoMirrored.Outlined.MenuBook,
            trailingContent = { Chevron() },
            onClick = { onNavigate(Routes.RESOURCES) },
        )
        AppListRow(
            title = "Session feedback",
            supportingText = "Read student ratings",
            leadingIcon = Icons.Outlined.FavoriteBorder,
            trailingContent = { Chevron() },
            onClick = { onNavigate(Routes.COUNSELLOR_FEEDBACK) },
        )
        AppListRow(
            title = "All appointments",
            supportingText = "Read-only system view",
            leadingIcon = Icons.AutoMirrored.Outlined.EventNote,
            trailingContent = { Chevron() },
            onClick = { onNavigate(Routes.ADMIN_APPOINTMENTS) },
            showDivider = false,
        )
    }
}

@Composable
private fun HeroNumber(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatRow(
    title: String,
    value: String,
    supportingText: String? = null,
    showDivider: Boolean = true,
) {
    AppListRow(
        title = title,
        supportingText = supportingText,
        trailingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        showDivider = showDivider,
    )
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---------------------------------------------------------------------------
// Shared shortcuts
// ---------------------------------------------------------------------------

private class HomeShortcut(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun QuickActionRow(
    actions: List<HomeShortcut>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        actions.forEach { action ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClickLabel = action.description, onClick = action.onClick)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.description,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

private fun sessionTimeLabel(start: Instant): String {
    val zone = ZoneId.of(AppConstants.TIME_ZONE)
    val date = start.atZone(zone).toLocalDate()
    val today = DateTimeUtils.nowInKualaLumpur().toLocalDate()
    val day = when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> DateTimeUtils.formatDate(start)
    }
    return "$day, ${DateTimeUtils.formatTime(start)}"
}
