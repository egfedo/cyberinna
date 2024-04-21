package ru.egfedo.kbrinna

import eu.vendeli.tgbot.TelegramBot
import ru.egfedo.kbrinna.config.Config
import ru.egfedo.kbrinna.database.question.Answer
import ru.egfedo.kbrinna.database.question.Question
import java.nio.file.Paths

suspend fun main() {
    val bot = TelegramBot(Config().token)
    bot.handleUpdates()
}