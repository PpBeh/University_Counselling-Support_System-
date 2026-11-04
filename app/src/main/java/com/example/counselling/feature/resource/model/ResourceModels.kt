// Module 5 | Owner: Michael
package com.example.counselling.feature.resource.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResourceRow(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    @SerialName("media_type") val mediaType: String,
    @SerialName("content_url") val contentUrl: String,
    @SerialName("target_stress_brackets") val targetStressBrackets: List<String> = emptyList(),
    @SerialName("is_published") val isPublished: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    val isHotline: Boolean get() = mediaType == "Hotline"
}

@Serializable
data class ResourceInsert(
    val title: String,
    val description: String,
    val category: String,
    @SerialName("media_type") val mediaType: String,
    @SerialName("content_url") val contentUrl: String,
    @SerialName("target_stress_brackets") val targetStressBrackets: List<String>,
    @SerialName("is_published") val isPublished: Boolean,
)

data class SelfCheckQuestion(
    val id: Int,
    val text: String,
    val category: String,
)

data class SelfCheckAnswerOption(val value: Int, val label: String)

data class SelfCheckBracket(
    val key: String,
    val label: String,
    val minScore: Int,
    val maxScore: Int,
)

data class SelfCheckResult(
    val score: Int,
    val bracket: SelfCheckBracket,
    val dominantConcern: String,
)

object SelfCheckContent {
    val questions = listOf(
        SelfCheckQuestion(
            1,
            "Over the past two weeks, how often have you felt overwhelmed by everyday demands?",
            "Stress",
        ),
        SelfCheckQuestion(
            2,
            "How often have you felt nervous, anxious or on edge?",
            "Anxiety",
        ),
        SelfCheckQuestion(
            3,
            "How often have you worried that your academic workload is beyond what you can manage?",
            "Academic Pressure",
        ),
        SelfCheckQuestion(
            4,
            "How often have you had difficulty falling or staying asleep because your mind would not switch off?",
            "Sleep",
        ),
        SelfCheckQuestion(
            5,
            "How often have you felt distressed by a conflict or distance in an important relationship?",
            "Relationships",
        ),
        SelfCheckQuestion(
            6,
            "How often have you felt worried about money or covering your essentials?",
            "Financial",
        ),
        SelfCheckQuestion(
            7,
            "How often have you felt lonely, even when you were around other people?",
            "Loneliness",
        ),
        SelfCheckQuestion(
            8,
            "How often have you found little interest or pleasure in things you usually enjoy?",
            "Other",
        ),
        SelfCheckQuestion(
            9,
            "How often have you noticed physical tension, headaches or a racing heart when under pressure?",
            "Stress",
        ),
        SelfCheckQuestion(
            10,
            "How often have you found worry difficult to control or stop?",
            "Anxiety",
        ),
    )

    val answerOptions = listOf(
        SelfCheckAnswerOption(0, "Not at all"),
        SelfCheckAnswerOption(1, "Rarely"),
        SelfCheckAnswerOption(2, "Sometimes"),
        SelfCheckAnswerOption(3, "Often"),
        SelfCheckAnswerOption(4, "Nearly every day"),
    )

    val brackets = listOf(
        SelfCheckBracket("low", "Low", 0, 13),
        SelfCheckBracket("moderate", "Moderate", 14, 26),
        SelfCheckBracket("high", "High", 27, 40),
    )

    fun bracketFor(score: Int): SelfCheckBracket =
        brackets.firstOrNull { score in it.minScore..it.maxScore } ?: brackets.last()

    fun resultFor(answers: List<Int>): SelfCheckResult {
        val score = answers.sum()
        val bracket = bracketFor(score)
        val byCategory = questions.mapIndexed { index, question ->
            question.category to (answers.getOrNull(index) ?: 0)
        }.groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }
        val dominant = byCategory.maxByOrNull { it.value }?.key ?: "Other"
        return SelfCheckResult(score = score, bracket = bracket, dominantConcern = dominant)
    }
}

internal fun recommendResources(
    resources: List<ResourceRow>,
    result: SelfCheckResult,
): List<ResourceRow> = resources
    .filter { it.isPublished && result.bracket.key in it.targetStressBrackets }
    .filter { result.bracket.key == "high" || !it.isHotline }
    .sortedWith(
        compareBy<ResourceRow> {
            if (it.category == result.dominantConcern) 0 else 1
        }.thenBy { it.title },
    )
