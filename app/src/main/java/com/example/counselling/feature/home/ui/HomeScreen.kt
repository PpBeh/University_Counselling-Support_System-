// Home | Owner: shared (Beh, Jeremy, Michael)
// Role-based dashboard shells. The appointment/stat widgets are connected to
// the module repositories as those modules are delivered.
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.UserRole
import com.example.counselling.feature.appointment.model.Appointment
import com.example.counselling.feature.appointment.ui.AppointmentCard
import com.example.counselling.feature.student.model.Profile
import com.example.counselling.navigation.Routes
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.SectionHeader
import com.example.counselling.ui.components.UserAvatar
import com.example.counselling.ui.theme.SoftError
import com.example.counselling.ui.theme.SuccessContainer
import com.example.counselling.ui.theme.SuccessGreen
import com.example.counselling.ui.theme.WarningContainer

@Composable
fun HomeScreen(
    profile: Profile,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    appointments: List<Appointment> = emptyList(),
    isLoadingAppointments: Boolean = false,
    appointmentError: String? = null,
    onRetryAppointments: () -> Unit = {},
) {
    val role = profile.userRole ?: UserRole.STUDENT

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(
            title = "Counselling Support",
            actions = {
                IconButton(onClick = { onNavigate(Routes.PROFILE) }) {
                    UserAvatar(
                        initials = profile.initials,
                        size = 34.dp,
                        contentDescription = "Open profile",
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            GreetingCard(profile = profile, role = role)
            Spacer(Modifier.height(22.dp))

            when (role) {
                UserRole.STUDENT -> StudentHome(
                    profile = profile,
                    onNavigate = onNavigate,
                    appointments = appointments,
                    isLoadingAppointments = isLoadingAppointments,
                    appointmentError = appointmentError,
                    onRetryAppointments = onRetryAppointments,
                )
                UserRole.COUNSELLOR -> CounsellorHome(onNavigate)
                UserRole.ADMIN -> AdminHome(onNavigate)
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun GreetingCard(profile: Profile, role: UserRole) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(
                text = DateTimeUtils.greeting(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Text(
                text = profile.fullName.substringBefore(' '),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = when (role) {
                    UserRole.STUDENT -> "You do not have to manage everything alone. What feels useful today?"
                    UserRole.COUNSELLOR -> "Your space to support students with care and privacy."
                    UserRole.ADMIN -> "A calm overview of the counselling service."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = DateTimeUtils.todayHeading(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun StudentHome(
    profile: Profile,
    onNavigate: (String) -> Unit,
    appointments: List<Appointment>,
    isLoadingAppointments: Boolean,
    appointmentError: String?,
    onRetryAppointments: () -> Unit,
) {
    val nextAppointment = appointments
        .filter {
            it.status in listOf("pending", "confirmed") &&
                DateTimeUtils.isFuture(it.slotStart)
        }
        .minByOrNull { it.slotStart }

    SectionHeader(
        title = "Quick actions",
        subtitle = "Small steps are still progress.",
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickActionCard(
            title = "Book a session",
            subtitle = "Find a counsellor and a time that fits",
            icon = Icons.Outlined.CalendarMonth,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Routes.STUDENT_APPOINTMENTS) },
        )
        QuickActionCard(
            title = "Self-help",
            subtitle = "Calm, practical resources",
            icon = Icons.Outlined.MenuBook,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Routes.RESOURCES) },
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickActionCard(
            title = "Private enquiry",
            subtitle = "Ask without sharing your name",
            icon = Icons.Outlined.Forum,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Routes.ENQUIRIES) },
        )
        QuickActionCard(
            title = "Wellbeing check-in",
            subtitle = "A private self-check, not a diagnosis",
            icon = Icons.Outlined.Psychology,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Routes.SELF_CHECK) },
        )
    }

    Spacer(Modifier.height(22.dp))
    SectionHeader(title = "Your next appointment")
    Spacer(Modifier.height(10.dp))
    when {
        isLoadingAppointments -> LoadingShimmer(rows = 1)
        appointmentError != null -> InfoCard(
            title = "Could not load your appointments",
            body = appointmentError,
            icon = Icons.Outlined.CalendarMonth,
            actionLabel = "Try again",
            onAction = onRetryAppointments,
        )
        nextAppointment != null -> {
            AppointmentCard(
                appointment = nextAppointment,
                viewerRole = UserRole.STUDENT,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { onNavigate(Routes.STUDENT_APPOINTMENTS) }) {
                    Text("Manage appointment")
                }
            }
        }
        else -> InfoCard(
            title = "No upcoming session yet",
            body = "When you book a counselling session, your next appointment and its status will appear here.",
            icon = Icons.Outlined.CalendarMonth,
            actionLabel = "Book a session",
            onAction = { onNavigate(Routes.STUDENT_APPOINTMENTS) },
        )
    }

    Spacer(Modifier.height(22.dp))
    InfoCard(
        title = "Need urgent help?",
        body = "If you are in immediate danger, contact campus security or local emergency services. You can also open the urgent support resources.",
        icon = Icons.Outlined.SupportAgent,
        actionLabel = "View urgent resources",
        onAction = { onNavigate(Routes.RESOURCES_HOTLINE) },
        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
        accentColor = SoftError,
    )

    Spacer(Modifier.height(16.dp))
    InfoCard(
        title = "Private by design",
        body = "Your sessions are only visible to you and the counsellor involved. Anonymous enquiries do not store your identity.",
        icon = Icons.Outlined.Forum,
        containerColor = SuccessContainer.copy(alpha = 0.45f),
        accentColor = SuccessGreen,
    )

    if (profile.preferredSessionMode != null) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Preferred mode: " +
                (if (profile.preferredSessionMode == "online") "Online" else "In person"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CounsellorHome(onNavigate: (String) -> Unit) {
    SectionHeader(
        title = "Your work today",
        subtitle = "Support students while keeping clear boundaries.",
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickActionCard(
            title = "My schedule",
            subtitle = "Availability and confirmed sessions",
            icon = Icons.Outlined.CalendarMonth,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Routes.COUNSELLOR_SCHEDULE) },
        )
        QuickActionCard(
            title = "Session records",
            subtitle = "Notes, summaries and follow-ups",
            icon = Icons.Outlined.Description,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Routes.COUNSELLOR_RECORDS) },
        )
    }
    Spacer(Modifier.height(12.dp))
    QuickActionCard(
        title = "Counselling inbox",
        subtitle = "Read and reply to student enquiries",
        icon = Icons.Outlined.Forum,
        tint = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.fillMaxWidth(),
        onClick = { onNavigate(Routes.ENQUIRIES) },
    )
    Spacer(Modifier.height(12.dp))
    QuickActionCard(
        title = "Student directory",
        subtitle = "Search by name or student ID",
        icon = Icons.Outlined.School,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
        onClick = { onNavigate(Routes.STUDENT_DIRECTORY) },
    )

    Spacer(Modifier.height(22.dp))
    SectionHeader(title = "Follow-up care")
    Spacer(Modifier.height(10.dp))
    InfoCard(
        title = "Urgency triage board",
        body = "High-risk and follow-up-needed session records are grouped into Overdue, Due soon and Later so nothing slips through.",
        icon = Icons.Outlined.Description,
        actionLabel = "Open records",
        onAction = { onNavigate(Routes.COUNSELLOR_RECORDS) },
        containerColor = WarningContainer.copy(alpha = 0.45f),
    )

    Spacer(Modifier.height(16.dp))
    InfoCard(
        title = "Privacy reminder",
        body = "Private session notes are visible only to you. Students see the public summary, and administrators see aggregate counts only.",
        icon = Icons.Outlined.Groups,
        containerColor = SuccessContainer.copy(alpha = 0.45f),
        accentColor = SuccessGreen,
    )
}

@Composable
private fun AdminHome(onNavigate: (String) -> Unit) {
    SectionHeader(
        title = "Service overview",
        subtitle = "Manage the people and resources around student care.",
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickActionCard(
            title = "Counsellors",
            subtitle = "Directory and availability",
            icon = Icons.Outlined.Groups,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Routes.ADMIN_COUNSELLORS) },
        )
        QuickActionCard(
            title = "Students",
            subtitle = "Search student profiles",
            icon = Icons.Outlined.School,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Routes.ADMIN_STUDENTS) },
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickActionCard(
            title = "Resources",
            subtitle = "Publish self-help content",
            icon = Icons.Outlined.MenuBook,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Routes.RESOURCES) },
        )
        QuickActionCard(
            title = "All appointments",
            subtitle = "Read-only system view",
            icon = Icons.Outlined.EventNote,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Routes.ADMIN_APPOINTMENTS) },
        )
    }

    Spacer(Modifier.height(22.dp))
    SectionHeader(title = "Oversight")
    Spacer(Modifier.height(10.dp))
    InfoCard(
        title = "Appointment list",
        body = "Administrators can read every appointment for oversight, but cannot change bookings. Session notes remain counsellor-only.",
        icon = Icons.Outlined.EventNote,
        actionLabel = "View all appointments",
        onAction = { onNavigate(Routes.ADMIN_APPOINTMENTS) },
    )

    Spacer(Modifier.height(16.dp))
    InfoCard(
        title = "Student care stays private",
        body = "Admins do not have access to private session notes or individual feedback. The dashboard shows aggregate counts only.",
        icon = Icons.Outlined.Groups,
        containerColor = SuccessContainer.copy(alpha = 0.45f),
        accentColor = SuccessGreen,
    )
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.13f),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
