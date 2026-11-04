// Navigation | Owner: shared (Beh, Jeremy, Michael)
// Role decides the five (maximum) bottom-navigation tabs.
package com.example.counselling.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.counselling.core.UserRole

data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

fun destinationsFor(role: UserRole): List<TopLevelDestination> = when (role) {
    UserRole.STUDENT -> listOf(
        TopLevelDestination(Routes.HOME, "Home", Icons.Outlined.Home),
        TopLevelDestination(Routes.STUDENT_APPOINTMENTS, "Appointments", Icons.Outlined.CalendarMonth),
        TopLevelDestination(Routes.RESOURCES, "Resources", Icons.Outlined.MenuBook),
        TopLevelDestination(Routes.ENQUIRIES, "Enquiries", Icons.Outlined.Forum),
        TopLevelDestination(Routes.PROFILE, "Profile", Icons.Outlined.Person),
    )

    UserRole.COUNSELLOR -> listOf(
        TopLevelDestination(Routes.HOME, "Home", Icons.Outlined.Home),
        TopLevelDestination(Routes.COUNSELLOR_SCHEDULE, "Schedule", Icons.Outlined.CalendarMonth),
        TopLevelDestination(Routes.COUNSELLOR_RECORDS, "Records", Icons.Outlined.Description),
        TopLevelDestination(Routes.ENQUIRIES, "Enquiries", Icons.Outlined.Forum),
        TopLevelDestination(Routes.PROFILE, "Profile", Icons.Outlined.Person),
    )

    UserRole.ADMIN -> listOf(
        TopLevelDestination(Routes.HOME, "Home", Icons.Outlined.Home),
        TopLevelDestination(Routes.ADMIN_COUNSELLORS, "Counsellors", Icons.Outlined.Groups),
        TopLevelDestination(Routes.ADMIN_STUDENTS, "Students", Icons.Outlined.School),
        TopLevelDestination(Routes.RESOURCES, "Resources", Icons.Outlined.MenuBook),
        TopLevelDestination(Routes.PROFILE, "Profile", Icons.Outlined.Person),
    )
}
