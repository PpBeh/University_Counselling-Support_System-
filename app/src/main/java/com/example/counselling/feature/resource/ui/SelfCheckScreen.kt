// Module 5 / Unique Feature 3 | Owner: Michael
package com.example.counselling.feature.resource.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.MessageKind
import com.example.counselling.core.UiMessage
import com.example.counselling.feature.resource.data.ResourceRepository
import com.example.counselling.feature.resource.model.ResourceRow
import com.example.counselling.feature.resource.model.SelfCheckContent
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.SecondaryButton
import com.example.counselling.ui.components.SectionHeader
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.components.showAppMessage
import com.example.counselling.ui.theme.SuccessContainer
import com.example.counselling.ui.theme.SuccessGreen
import com.example.counselling.ui.theme.WarningContainer
import com.example.counselling.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@Composable
fun SelfCheckRoute(
    repository: ResourceRepository,
    onBack: () -> Unit,
    onBook: (String) -> Unit,
) {
    val viewModel: SelfCheckViewModel = viewModel(
        factory = SelfCheckViewModel.factory(repository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // No ViewModel message channel needed here; resource opening failures
        // are reported directly through this host.
    }

    SelfCheckScreen(
        state = state,
        questions = viewModel.questions,
        options = viewModel.options,
        snackbarHostState = snackbarState,
        onBack = onBack,
        onStart = viewModel::start,
        onSelect = viewModel::selectAnswer,
        onNext = viewModel::next,
        onPrevious = viewModel::back,
        onRetryRecommendations = viewModel::retryRecommendations,
        onRetake = viewModel::retake,
        onBook = onBook,
        onOpenResource = { resource ->
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resource.contentUrl)))
            }.onFailure {
                scope.launch {
                    snackbarState.showAppMessage(
                        UiMessage("No app can open this resource link.", MessageKind.ERROR),
                    )
                }
            }
        },
    )
}

@Composable
private fun SelfCheckScreen(
    state: SelfCheckUiState,
    questions: List<com.example.counselling.feature.resource.model.SelfCheckQuestion>,
    options: List<com.example.counselling.feature.resource.model.SelfCheckAnswerOption>,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onRetryRecommendations: () -> Unit,
    onRetake: () -> Unit,
    onBook: (String) -> Unit,
    onOpenResource: (ResourceRow) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = "Wellbeing check-in",
                onBack = if (state.started && state.result == null) onPrevious else onBack,
            )
            when {
                state.result != null -> ResultContent(
                    state = state,
                    onRetryRecommendations = onRetryRecommendations,
                    onRetake = onRetake,
                    onBook = onBook,
                    onOpenResource = onOpenResource,
                )

                !state.started -> IntroContent(onStart = onStart)

                else -> QuestionContent(
                    state = state,
                    questions = questions,
                    options = options,
                    onSelect = onSelect,
                    onNext = onNext,
                    onPrevious = onPrevious,
                )
            }
        }
        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}

@Composable
private fun IntroContent(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "A private moment to check in.",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 22.dp),
        )
        Text(
            text = "Ten short questions about the last two weeks. Your answers stay on this screen and are never saved.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(20.dp))
        InfoCard(
            title = "Screening aid, not a diagnosis",
            body = "This check-in can help you notice what support might be useful. It does not diagnose a condition and does not replace a counsellor or doctor.",
            containerColor = WarningContainer.copy(alpha = 0.55f),
            accentColor = WarningAmber,
        )
        Spacer(Modifier.height(16.dp))
        InfoCard(
            title = "Your answers are not stored",
            body = "No score or answer is written to the database. Only this device's current screen holds them.",
            containerColor = SuccessContainer.copy(alpha = 0.45f),
            accentColor = SuccessGreen,
        )
        Spacer(Modifier.height(22.dp))
        PrimaryButton(text = "Start check-in", onClick = onStart)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuestionContent(
    state: SelfCheckUiState,
    questions: List<com.example.counselling.feature.resource.model.SelfCheckQuestion>,
    options: List<com.example.counselling.feature.resource.model.SelfCheckAnswerOption>,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val question = questions[state.currentIndex]
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Question ${state.currentIndex + 1} of ${questions.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = question.text,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        options.forEach { option ->
            val selected = state.answers[state.currentIndex] == option.value
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .selectable(
                        selected = selected,
                        onClick = { onSelect(option.value) },
                    ),
                shape = MaterialTheme.shapes.medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onSelect(option.value) },
                    )
                    Text(option.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(
                text = "Back",
                onClick = onPrevious,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = if (state.currentIndex == questions.lastIndex) "See my result" else "Next",
                onClick = onNext,
                enabled = state.answers[state.currentIndex] != null,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ResultContent(
    state: SelfCheckUiState,
    onRetryRecommendations: () -> Unit,
    onRetake: () -> Unit,
    onBook: (String) -> Unit,
    onOpenResource: (ResourceRow) -> Unit,
) {
    val result = state.result ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Your check-in result",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            text = "${result.score} out of 40",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(
                status = when (result.bracket.key) {
                    "low" -> "low"
                    "moderate" -> "moderate"
                    else -> "high"
                },
            )
            Text(
                text = result.bracket.label + " wellbeing bracket",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            text = "Screening aid only — this is not a diagnosis. If you are worried about your safety, contact campus security or local emergency services now.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
        Spacer(Modifier.height(16.dp))
        InfoCard(
            title = "What seems to be weighing most",
            body = "${result.dominantConcern} was the area with the highest answers. You can talk about anything in a session.",
        )
        if (result.bracket.key != "low") {
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                text = "Book a counsellor",
                onClick = { onBook(result.dominantConcern) },
            )
        }
        Spacer(Modifier.height(20.dp))
        SectionHeader(
            title = "Resources for this bracket",
            subtitle = "Published resources matched to ${result.bracket.label} wellbeing. Hotline resources only appear for High.",
        )
        Spacer(Modifier.height(8.dp))
        when {
            state.isLoadingRecommendations -> LoadingShimmer(rows = 3)
            state.recommendationError != null -> ErrorState(
                message = state.recommendationError,
                onRetry = onRetryRecommendations,
            )
            state.recommendations.isEmpty() -> EmptyState(
                icon = Icons.Outlined.MenuBook,
                title = "No matching resources yet",
                message = "You can still book a session or explore the full resource library.",
            )
            else -> state.recommendations.forEach { resource ->
                ResourceRecommendation(resource = resource, onOpen = { onOpenResource(resource) })
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        SecondaryButton(text = "Retake check-in", onClick = onRetake)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ResourceRecommendation(resource: ResourceRow, onOpen: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(resource.title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${resource.category} · ${resource.mediaType}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = resource.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 2,
            )
        }
    }
}
