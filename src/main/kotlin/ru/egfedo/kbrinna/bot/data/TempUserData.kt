package ru.egfedo.kbrinna.bot.data

import ru.egfedo.kbrinna.database.question.QuestionDTO

data class TempUserData(
    val entries: List<QuestionDTO>,
    val questionIndex: Int,
    val correctCount: Int,
    val inputBuffer: String,
    val wrongAnswers: List<String>,
    )
