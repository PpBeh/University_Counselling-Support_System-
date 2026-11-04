// Navigation | Owner: shared (Beh, Jeremy, Michael)
// One root decides between auth and the signed-in role shell. Navigation
// Compose then owns the login/register stack and the role-based main shell.
package com.example.counselling.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.counselling.core.AppContainer
import com.example.counselling.core.MessageKind
import com.example.counselling.core.SessionManager
import com.example.counselling.core.SessionState
import com.example.counselling.core.UiMessage
import com.example.counselling.core.UserRole
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.appointment.ui.AdminAppointmentsRoute
import com.example.counselling.feature.appointment.ui.BookAppointmentRoute
import com.example.counselling.feature.appointment.ui.CounsellorScheduleRoute
import com.example.counselling.feature.appointment.ui.RescheduleRoute
import com.example.counselling.feature.appointment.ui.StudentAppointmentsRoute
import com.example.counselling.feature.auth.data.AuthRepository
import com.example.counselling.feature.auth.ui.ConfigurationErrorScreen
import com.example.counselling.feature.auth.ui.LoginRoute
import com.example.counselling.feature.auth.ui.RegisterRoute
import com.example.counselling.feature.auth.ui.SplashScreen
import com.example.counselling.feature.home.ui.HomeRoute
import com.example.counselling.feature.student.data.ProfileRepository
import com.example.counselling.feature.student.model.Profile
import com.example.counselling.feature.student.ui.ProfileRoute
import com.example.counselling.feature.student.ui.StudentDetailRoute
import com.example.counselling.feature.student.ui.StudentListRoute
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.ModulePlaceholderScreen
import com.example.counselling.ui.components.showAppMessage
import kotlinx.coroutines.launch

@Composable
fun AppRoot(container: AppContainer) {
    val configurationError = container.configurationError
    if (configurationError != null) {
        ConfigurationErrorScreen(message = configurationError)
        return
    }

    val sessionManager = container.sessionManager
    val authRepository = container.authRepository
    val profileRepository = container.profileRepository
    val appointmentRepository = container.appointmentRepository

    if (
        sessionManager == null ||
        authRepository == null ||
        profileRepository == null ||
        appointmentRepository == null
    ) {
        ConfigurationErrorScreen(
            message = "The services could not be initialised. Check local.properties and rebuild.",
        )
        return
    }

    val sessionState by sessionManager.state.collectAsStateWithLifecycle()

    LaunchedEffect(sessionManager) {
        sessionManager.initialize()
    }

    when (val state = sessionState) {
        SessionState.Loading -> SplashScreen()
        is SessionState.SignedOut -> AuthNavGraph(
            sessionManager = sessionManager,
            notice = state.notice,
        )
        is SessionState.SignedIn -> MainApp(
            profile = state.profile,
            sessionManager = sessionManager,
            authRepository = authRepository,
            profileRepository = profileRepository,
            appointmentRepository = appointmentRepository,
        )
    }
}

@Composable
private fun AuthNavGraph(
    sessionManager: SessionManager,
    notice: UiMessage?,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(Routes.LOGIN) {
            LoginRoute(
                sessionManager = sessionManager,
                notice = notice,
                onNoticeConsumed = sessionManager::clearNotice,
                onRegister = {
                    navController.navigate(Routes.REGISTER) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.REGISTER) {
            RegisterRoute(
                sessionManager = sessionManager,
                onBackToLogin = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.LOGIN) {
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun MainApp(
    profile: Profile,
    sessionManager: SessionManager,
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    appointmentRepository: AppointmentRepository,
) {
    val role = profile.userRole ?: UserRole.STUDENT
    val destinations = remember(role) { destinationsFor(role) }
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = destinations.any { it.route == currentRoute }

    LaunchedEffect(profile.id) {
        snackbarHostState.showAppMessage(
            UiMessage(
                text = "Welcome, ${profile.fullName.substringBefore(' ')}.",
                kind = MessageKind.SUCCESS,
            ),
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navController.navigateToTab(destination.route) },
                            icon = {
                                androidx.compose.material3.Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeRoute(
                    profile = profile,
                    repository = appointmentRepository,
                    onNavigate = { route -> navController.navigateOnce(route) },
                )
            }

            composable(Routes.STUDENT_APPOINTMENTS) {
                StudentAppointmentsRoute(
                    repository = appointmentRepository,
                    onBook = { navController.navigateOnce(Routes.BOOK_APPOINTMENT) },
                    onReschedule = { id ->
                        navController.navigateOnce(Routes.reschedule(id))
                    },
                )
            }

            composable(Routes.BOOK_APPOINTMENT) {
                BookAppointmentRoute(
                    repository = appointmentRepository,
                    sessionManager = sessionManager,
                    onClose = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.RESCHEDULE,
                arguments = listOf(navArgument("appointmentId") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("appointmentId").orEmpty()
                RescheduleRoute(
                    appointmentId = id,
                    repository = appointmentRepository,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.ENQUIRIES) {
                val isCounsellor = role == UserRole.COUNSELLOR
                ModulePlaceholderScreen(
                    title = if (isCounsellor) "Counselling inbox" else "Enquiries",
                    moduleLabel = "Module 6",
                    owner = "Michael",
                    description = if (isCounsellor) {
                        "Anonymous and linked enquiries, replies and recommended resources will be managed here."
                    } else {
                        "Ask a question privately, check a reply by reference code and rate completed sessions here."
                    },
                    icon = Icons.Outlined.Forum,
                )
            }

            composable(Routes.RESOURCES) {
                val isAdmin = role == UserRole.ADMIN
                ModulePlaceholderScreen(
                    title = "Resources",
                    moduleLabel = "Module 5",
                    owner = "Michael",
                    description = if (isAdmin) {
                        "Create, publish and remove self-help resources, and review the library."
                    } else {
                        "Browse practical self-help articles, videos, audio and urgent support contacts."
                    },
                    icon = Icons.Outlined.MenuBook,
                )
            }

            composable(Routes.COUNSELLOR_SCHEDULE) {
                CounsellorScheduleRoute(repository = appointmentRepository)
            }

            composable(Routes.COUNSELLOR_RECORDS) {
                ModulePlaceholderScreen(
                    title = "Session records",
                    moduleLabel = "Module 4",
                    owner = "Jeremy",
                    description = "Private notes, public summaries, risk levels and the urgency triage follow-up board will appear here.",
                    icon = Icons.Outlined.Description,
                )
            }

            composable(Routes.ADMIN_COUNSELLORS) {
                ModulePlaceholderScreen(
                    title = "Counsellors",
                    moduleLabel = "Module 3",
                    owner = "Jeremy",
                    description = "Add counsellors, manage their availability and remove or deactivate directory entries here.",
                    icon = Icons.Outlined.Groups,
                )
            }

            composable(Routes.ADMIN_STUDENTS) {
                StudentListRoute(
                    profileRepository = profileRepository,
                    onBack = null,
                    onOpenStudent = { studentId ->
                        navController.navigateOnce(Routes.studentDetail(studentId))
                    },
                )
            }

            composable(Routes.STUDENT_DIRECTORY) {
                StudentListRoute(
                    profileRepository = profileRepository,
                    onBack = { navController.popBackStack() },
                    onOpenStudent = { studentId ->
                        navController.navigateOnce(Routes.studentDetail(studentId))
                    },
                )
            }

            composable(
                route = Routes.STUDENT_DETAIL,
                arguments = listOf(navArgument("studentId") { type = NavType.StringType }),
            ) { entry ->
                val studentId = entry.arguments?.getString("studentId").orEmpty()
                StudentDetailRoute(
                    studentId = studentId,
                    profileRepository = profileRepository,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.PROFILE) {
                ProfileRoute(
                    sessionManager = sessionManager,
                    profileRepository = profileRepository,
                    authRepository = authRepository,
                    appointmentRepository = appointmentRepository,
                    onSignOut = {
                        scope.launch { sessionManager.signOut() }
                    },
                )
            }

            composable(Routes.SELF_CHECK) {
                ModulePlaceholderScreen(
                    title = "Wellbeing check-in",
                    moduleLabel = "Module 5",
                    owner = "Michael",
                    description = "The private 10-question self-check, score bracket and matched resources will appear here. It is a screening aid, not a diagnosis.",
                    icon = Icons.Outlined.Psychology,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.RESOURCES_HOTLINE) {
                ModulePlaceholderScreen(
                    title = "Urgent support",
                    moduleLabel = "Module 5",
                    owner = "Michael",
                    description = "Published hotline resources for urgent moments will be shown here. If you are in immediate danger, contact campus security or local emergency services.",
                    icon = Icons.Outlined.SupportAgent,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.ADMIN_APPOINTMENTS) {
                AdminAppointmentsRoute(
                    repository = appointmentRepository,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) {
            saveState = true
            inclusive = false
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavController.navigateOnce(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}
