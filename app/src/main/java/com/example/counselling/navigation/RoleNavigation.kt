// Navigation | Owner: shared (Beh, Jeremy, Michael)
// The polymorphic User decides its five (maximum) bottom-navigation tabs.
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
import com.example.counselling.core.domain.HomeTab
import com.example.counselling.core.domain.User

data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

// OOP: Polymorphism - navigation asks the User object for its tabs.
fun destinationsFor(user: User): List<TopLevelDestination> =
    user.homeTabs().map { it.toDestination() }

// OOP: Polymorphism - HomeTab owns its logical identity; this UI mapping is the
// only place that knows the Android icon and route string.
private fun HomeTab.toDestination(): TopLevelDestination = when (this) {
    HomeTab.HOME -> TopLevelDestination(Routes.HOME, label, Icons.Outlined.Home)
    HomeTab.APPOINTMENTS -> TopLevelDestination(
        Routes.STUDENT_APPOINTMENTS,
        label,
        Icons.Outlined.CalendarMonth,
    )
    HomeTab.SCHEDULE -> TopLevelDestination(
        Routes.COUNSELLOR_SCHEDULE,
        label,
        Icons.Outlined.CalendarMonth,
    )
    HomeTab.RECORDS -> TopLevelDestination(
        Routes.COUNSELLOR_RECORDS,
        label,
        Icons.Outlined.Description,
    )
    HomeTab.COUNSELLORS -> TopLevelDestination(
        Routes.ADMIN_COUNSELLORS,
        label,
        Icons.Outlined.Groups,
    )
    HomeTab.STUDENTS -> TopLevelDestination(
        Routes.ADMIN_STUDENTS,
        label,
        Icons.Outlined.School,
    )
    HomeTab.RESOURCES -> TopLevelDestination(Routes.RESOURCES, label, Icons.Outlined.MenuBook)
    HomeTab.ENQUIRIES -> TopLevelDestination(Routes.ENQUIRIES, label, Icons.Outlined.Forum)
    HomeTab.PROFILE -> TopLevelDestination(Routes.PROFILE, label, Icons.Outlined.Person)
}
