package ru.egfedo.kbrinna.database.question

import kotlinx.serialization.Serializable


/**
 * Answer - Encapsulates an answer.
 */
@Serializable
data class Answer(
    /**
     * Answer display text
     */
    val text: String = "text",
    /**
     * Answer correctness flag
     */
    val correct: Boolean = false,
)
