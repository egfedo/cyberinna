package ru.egfedo.kbrinna.database.question

/**
 * Question data transfer object. Not used in the database storage.
 */
data class QuestionDTO(
    /**
     * Question text
     */
    val text: String,
    /**
     * Question answer list
     */
    val answers: List<Answer>,
    /**
     * Question type
     */
    val type: Type,
    /**
     * Question ID which is being displayed to the user
     */
    val displayID: String,
    /**
     * Question origin database id in the Storage
     */
    val originDB: String,
    /**
     * Question origin ID inside the database
     */
    val originID: String
) {
    enum class Type {
        SINGLE, MULTIPLE
    }

    companion object {
        /**
         * Constructs a DTO from a database question
         */
        fun fromQuestion(question: Question,
                         displayID: String,
                         originDB: String,
                         originID: String): QuestionDTO {
            return QuestionDTO(
                question.text,
                question.answers.toList().shuffled(),
                if (question.type == Question.AnswerType.SINGLE) Type.SINGLE else Type.MULTIPLE,
                displayID,
                originDB,
                originID
            )
        }
    }

    /**
     * Returns a string which is displayed to the user
     */
    fun display(): String {
        return buildString {
            append("<b>Вопрос:</b> ")
            append(text)
            append(" [$displayID]\n\n")
            for (i in answers.indices) {
                append("<b>")
                append("${i+1}.")
                append("</b> ")
                append(answers[i].text)
                append("\n")
            }
            if (type == Type.SINGLE) {
                append("\nВыберите <b>один</b> правильный ответ\n")
            }
            else {
                append("\nВыберите <b>один или несколько</b> правильных ответов\n")
            }
        }
    }
}
