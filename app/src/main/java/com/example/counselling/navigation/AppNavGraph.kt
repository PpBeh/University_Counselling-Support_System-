// Navigation | Owner: shared (Beh, Jeremy, Michael)
// One root decides between auth and the signed-in role shell. Navigation
// Compose then owns the login/register stack and the role-based main shell.
package com.example.counselling.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import com.example.counselling.feature.counsellor.data.CounsellorRepository
import com.example.counselling.feature.counsellor.ui.CounsellorAvailabilityRoute
import com.example.counselling.feature.counsellor.ui.CounsellorDetailRoute
import com.example.counselling.feature.counsellor.ui.CounsellorFormRoute
import com.example.counselling.feature.counsellor.ui.CounsellorListRoute
import com.example.counselling.feature.enquiry.data.EnquiryRepository
import com.example.counselling.feature.enquiry.data.FeedbackRepository
import com.example.counselling.feature.enquiry.ui.CounsellorFeedbackRoute
import com.example.counselling.feature.enquiry.ui.CounsellorInboxRoute
import com.example.counselling.feature.enquiry.ui.StudentEnquiriesRoute
import com.example.counselling.feature.enquiry.ui.StudentFeedbackRoute
import com.example.counselling.feature.home.ui.HomeRoute
import com.example.counselling.feature.records.data.SessionRecordRepository
import com.example.counselling.feature.records.ui.FollowUpRoute
import com.example.counselling.feature.records.ui.SessionRecordFormRoute
import com.example.counselling.feature.records.ui.SessionRecordsRoute
import com.example.counselling.feature.records.ui.StudentSummariesRoute
import com.example.counselling.feature.resource.data.ResourceRepository
import com.example.counselling.feature.resource.ui.ResourceFormRoute
import com.example.counselling.feature.resource.ui.ResourcesRoute
import com.example.counselling.feature.resource.ui.SelfCheckRoute
import com.example.counselling.feature.student.data.ProfileRepository
import com.example.counselling.feature.student.model.Profile
import com.example.counselling.feature.student.ui.ProfileRoute
import com.example.counselling.feature.student.ui.StudentDetailRoute
import com.example.counselling.feature.student.ui.StudentListRoute
import com.example.counselling.ui.components.AppSnackbarHost
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
    val counsellorRepository = container.counsellorRepository
    val recordRepository = container.recordRepository
    val resourceRepository = container.resourceRepository
    val enquiryRepository = container.enquiryRepository
    val feedbackRepository = container.feedbackRepository

    if (
        sessionManager == null ||
        authRepository == null ||
        profileRepository == null ||
        appointmentRepository == null ||
        counsellorRepository == null ||
        recordRepository == null ||
        resourceRepository == null ||
        enquiryRepository == null ||
        feedbackRepository == null
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
            counsellorRepository = counsellorRepository,
            recordRepository = recordRepository,
            resourceRepository = resourceRepository,
            enquiryRepository = enquiryRepository,
            feedbackRepository = feedbackRepository,
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
    counsellorRepository: CounsellorRepository,
    recordRepository: SessionRecordRepository,
    resourceRepository: ResourceRepository,
    enquiryRepository: EnquiryRepository,
    feedbackRepository: FeedbackRepository,
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
                    appointmentRepository = appointmentRepository,
                    recordRepository = recordRepository,
                    onNavigate = { route -> navController.navigateOnce(route) },
                )
            }

            composable(Routes.STUDENT_APPOINTMENTS) {
                StudentAppointmentsRoute(
                    repository = appointmentRepository,
                    onBook = { navController.navigateOnce(Routes.bookAppointment()) },
                    onReschedule = { id ->
                        navController.navigateOnce(Routes.reschedule(id))
                    },
                    onOpenSummaries = {
                        navController.navigateOnce(Routes.STUDENT_SUMMARIES)
                    },
                    onOpenFeedback = {
                        navController.navigateOnce(Routes.STUDENT_FEEDBACK)
                    },
                )
            }

            composable(
                route = Routes.BOOK_APPOINTMENT,
                arguments = listOf(
                    navArgument("concern") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                BookAppointmentRoute(
                    repository = appointmentRepository,
                    sessionManager = sessionManager,
                    initialConcern = entry.arguments?.getString("concern"),
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

            composable(
                route = Routes.COUNSELLOR_RESCHEDULE,
                arguments = listOf(navArgument("appointmentId") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("appointmentId").orEmpty()
                RescheduleRoute(
                    appointmentId = id,
                    repository = appointmentRepository,
                    isCounsellor = true,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.ENQUIRIES) {
                if (role == UserRole.COUNSELLOR) {
                    CounsellorInboxRoute(
                        enquiryRepository = enquiryRepository,
                        resourceRepository = resourceRepository,
                        onOpenFeedback = {
                            navController.navigateOnce(Routes.COUNSELLOR_FEEDBACK)
                        },
                    )
                } else {
                    StudentEnquiriesRoute(
                        repository = enquiryRepository,
                        onOpenFeedback = {
                            navController.navigateOnce(Routes.STUDENT_FEEDBACK)
                        },
                    )
                }
            }

            composable(Routes.RESOURCES) {
                ResourcesRoute(
                    role = role,
                    repository = resourceRepository,
                    onAdd = { navController.navigateOnce(Routes.resourceForm()) },
                    onEdit = { id -> navController.navigateOnce(Routes.resourceForm(id)) },
                )
            }

            composable(
                route = Routes.RESOURCE_FORM,
                arguments = listOf(
                    navArgument("resourceId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                ResourceFormRoute(
                    resourceId = entry.arguments?.getString("resourceId"),
                    repository = resourceRepository,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(Routes.SELF_CHECK) {
                SelfCheckRoute(
                    repository = resourceRepository,
                    onBack = { navController.popBackStack() },
                    onBook = { concern ->
                        navController.navigateOnce(Routes.bookAppointment(concern))
                    },
                )
            }

            composable(Routes.RESOURCES_HOTLINE) {
                ResourcesRoute(
                    role = role,
                    repository = resourceRepository,
                    hotlineOnly = true,
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigateOnce(Routes.resourceForm()) },
                    onEdit = { id -> navController.navigateOnce(Routes.resourceForm(id)) },
                )
            }

            composable(Routes.COUNSELLOR_SCHEDULE) {
                CounsellorScheduleRoute(
                    repository = appointmentRepository,
                    onManageAvailability = {
                        navController.navigateOnce(Routes.MY_AVAILABILITY)
                    },
                    onReschedule = { id ->
                        navController.navigateOnce(Routes.counsellorReschedule(id))
                    },
                )
            }

            composable(Routes.COUNSELLOR_RECORDS) {
                SessionRecordsRoute(
                    repository = recordRepository,
                    appointmentRepository = appointmentRepository,
                    onAddRecord = { appointmentId ->
                        navController.navigateOnce(
                            Routes.sessionRecordForm(appointmentId = appointmentId),
                        )
                    },
                    onEditRecord = { recordId ->
                        navController.navigateOnce(
                            Routes.sessionRecordForm(recordId = recordId),
                        )
                    },
                    onScheduleFollowUp = { recordId ->
                        navController.navigateOnce(Routes.followUp(recordId))
                    },
                )
            }

            composable(Routes.ADMIN_COUNSELLORS) {
                CounsellorListRoute(
                    repository = counsellorRepository,
                    onAdd = { navController.navigateOnce(Routes.counsellorForm()) },
                    onOpen = { id ->
                        navController.navigateOnce(Routes.counsellorDetail(id))
                    },
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

            composable(
                route = Routes.COUNSELLOR_DETAIL,
                arguments = listOf(navArgument("counsellorId") { type = NavType.StringType }),
            ) { entry ->
                val counsellorId = entry.arguments?.getString("counsellorId").orEmpty()
                CounsellorDetailRoute(
                    counsellorId = counsellorId,
                    repository = counsellorRepository,
                    onBack = { navController.popBackStack() },
                    onEdit = { id ->
                        navController.navigateOnce(Routes.counsellorForm(id))
                    },
                    onManageAvailability = { id ->
                        navController.navigateOnce(Routes.counsellorAvailability(id))
                    },
                    onRemoved = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.COUNSELLOR_FORM,
                arguments = listOf(
                    navArgument("counsellorId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val counsellorId = entry.arguments?.getString("counsellorId")
                CounsellorFormRoute(
                    counsellorId = counsellorId,
                    repository = counsellorRepository,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.COUNSELLOR_AVAILABILITY,
                arguments = listOf(navArgument("counsellorId") { type = NavType.StringType }),
            ) { entry ->
                val counsellorId = entry.arguments?.getString("counsellorId")
                CounsellorAvailabilityRoute(
                    counsellorId = counsellorId,
                    currentUserId = profile.id,
                    repository = counsellorRepository,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.MY_AVAILABILITY) {
                CounsellorAvailabilityRoute(
                    counsellorId = null,
                    currentUserId = profile.id,
                    repository = counsellorRepository,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.SESSION_RECORD_FORM,
                arguments = listOf(
                    navArgument("recordId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("appointmentId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                SessionRecordFormRoute(
                    recordId = entry.arguments?.getString("recordId"),
                    appointmentId = entry.arguments?.getString("appointmentId"),
                    recordRepository = recordRepository,
                    appointmentRepository = appointmentRepository,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.FOLLOW_UP,
                arguments = listOf(navArgument("recordId") { type = NavType.StringType }),
            ) { entry ->
                FollowUpRoute(
                    recordId = entry.arguments?.getString("recordId").orEmpty(),
                    repository = recordRepository,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.STUDENT_SUMMARIES) {
                StudentSummariesRoute(
                    repository = recordRepository,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.STUDENT_FEEDBACK) {
                StudentFeedbackRoute(
                    feedbackRepository = feedbackRepository,
                    appointmentRepository = appointmentRepository,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.COUNSELLOR_FEEDBACK) {
                CounsellorFeedbackRoute(
                    repository = feedbackRepository,
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
