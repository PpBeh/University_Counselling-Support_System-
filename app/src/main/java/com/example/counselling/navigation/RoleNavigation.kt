// Navigation | Owner: shared (Beh, Jeremy, Michael)
// The polymorphic User decides its five (maximum) bottom-navigation tabs.
// Each tab carries a filled icon for its selected state and an outlined icon
// for the unselected state, which is the standard Material 3 pattern.
package com.example.counselling.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
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
    val selectedIcon: ImageVector,
)

// OOP: Polymorphism - navigation asks the User object for its tabs.
fun destinationsFor(user: User): List<TopLevelDestination> =
    user.homeTabs().map { it.toDestination() }

// OOP: Polymorphism - HomeTab owns its logical identity; this UI mapping is the
// only place that knows the Android icon and route string. Labels stay short
// enough for one line in the bottom bar.
private fun HomeTab.toDestination(): TopLevelDestination = when (this) {
    HomeTab.HOME -> TopLevelDestination(
        Routes.HOME,
        label,
        Icons.Outlined.Home,
        Icons.Filled.Home,
    )
    HomeTab.APPOINTMENTS -> TopLevelDestination(
        Routes.STUDENT_APPOINTMENTS,
        "Sessions",
        Icons.Outlined.CalendarMonth,
        Icons.Filled.CalendarMonth,
    )
    HomeTab.SCHEDULE -> TopLevelDestination(
        Routes.COUNSELLOR_SCHEDULE,
        label,
        Icons.Outlined.CalendarMonth,
        Icons.Filled.CalendarMonth,
    )
    HomeTab.RECORDS -> TopLevelDestination(
        Routes.COUNSELLOR_RECORDS,
        label,
        Icons.Outlined.Description,
        Icons.Filled.Description,
    )
    HomeTab.COUNSELLORS -> TopLevelDestination(
        Routes.ADMIN_COUNSELLORS,
        label,
        Icons.Outlined.Groups,
        Icons.Filled.Groups,
    )
    HomeTab.STUDENTS -> TopLevelDestination(
        Routes.ADMIN_STUDENTS,
        label,
        Icons.Outlined.School,
        Icons.Filled.School,
    )
    HomeTab.RESOURCES -> TopLevelDestination(
        Routes.RESOURCES,
        label,
        Icons.AutoMirrored.Outlined.MenuBook,
        Icons.AutoMirrored.Filled.MenuBook,
    )
    HomeTab.ENQUIRIES -> TopLevelDestination(
        Routes.ENQUIRIES,
        label,
        Icons.Outlined.Forum,
        Icons.Filled.Forum,
    )
    HomeTab.PROFILE -> TopLevelDestination(
        Routes.PROFILE,
        label,
        Icons.Outlined.Person,
        Icons.Filled.Person,
    )
}
