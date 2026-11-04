// Module 1 | Owner: Beh
// Staff view of a student's permitted profile fields. Private session notes
// are never included here.
package com.example.counselling.feature.student.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.UiState
import com.example.counselling.feature.student.data.ProfileRepository
import com.example.counselling.feature.student.model.Profile
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.SectionHeader
import com.example.counselling.ui.components.UserAvatar

@Composable
fun StudentDetailRoute(
    studentId: String,
    profileRepository: ProfileRepository,
    onBack: () -> Unit,
) {
    val viewModel: StudentDetailViewModel = viewModel(
        factory = StudentDetailViewModel.factory(studentId, profileRepository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Student details", onBack = onBack)
        when (val current = state) {
            UiState.Loading -> LoadingShimmer(
                modifier = Modifier.padding(20.dp),
                rows = 4,
            )

            is UiState.Error -> ErrorState(
                message = current.message,
                onRetry = viewModel::retry,
            )

            is UiState.Success -> StudentDetailContent(current.data)
        }
    }
}

@Composable
private fun StudentDetailContent(student: Profile) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                initials = student.initials,
                size = 64.dp,
                contentDescription = "${student.fullName} initials",
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = student.fullName,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Student",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        SectionHeader(title = "Contact and study details")
        Spacer(Modifier.height(10.dp))
        InfoCard(title = "Student information", icon = Icons.Outlined.School) {
            DetailLine("Student ID", student.studentId ?: "—")
            DetailLine("Programme", student.programme ?: "—")
            DetailLine(
                "Preferred mode",
                when (student.preferredSessionMode) {
                    "in_person" -> "In person"
                    "online" -> "Online"
                    else -> "Not set"
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        InfoCard(title = "Contact", icon = Icons.Outlined.Email) {
            DetailLine("Email", student.email)
            DetailLine("Phone", student.phone ?: "—")
        }
        Spacer(Modifier.height(16.dp))
        InfoCard(
            title = "Privacy",
            body = "Private counselling notes are never shown in the student directory. They are visible only to the counsellor who wrote them.",
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1.4f),
        )
    }
}
