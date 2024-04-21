package ru.egfedo.kbrinna.database.question

import kotlinx.serialization.Serializable
import java.util.*


/**
 * Test question data that is stored inside the database.
 */
@Serializable
data class Question(
    /**
     * Question display text.
     */
    val text: String,
    /**
     * Question's set of answers.
     */
    val answers: Set<Answer>,
    /**
     * Question type depending on amount of correct answers.
     */
    val type: AnswerType = if (answers.count { it.correct } == 1) AnswerType.SINGLE else AnswerType.MULTIPLE
) {
    enum class AnswerType {
        SINGLE, MULTIPLE
    }

    /**
     * To string function.
     * @return String depicting question data
     */
    fun display(): String {
        return buildString {
            append("Вопрос: ")
            append(text)
            appendLine()
            appendLine()

            answers.forEachIndexed { i, answer ->
                append(i + 1)
                append(". ")
                append(answer.text)
                appendLine()
            }

            appendLine()
            if (type == AnswerType.SINGLE)
                append("Выберите один правильный ответ")
            else
                append("Выберите один или несколько правильных ответов")
            appendLine()
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(text, answers.sortedBy { it.text })
    }

    override fun equals(other: Any?): Boolean {
        if (other is Question) {
            return this.hashCode() == other.hashCode()
        }
        return false
    }
}
