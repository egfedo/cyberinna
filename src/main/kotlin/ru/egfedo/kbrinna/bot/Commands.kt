package ru.egfedo.kbrinna.bot

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.annotations.ParamMapping
import eu.vendeli.tgbot.annotations.UnprocessedHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.ParseMode
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.chat.ChatType
import eu.vendeli.tgbot.types.internal.CallbackQueryUpdate
import eu.vendeli.tgbot.types.internal.MessageUpdate
import eu.vendeli.tgbot.types.internal.ProcessedUpdate
import eu.vendeli.tgbot.types.internal.UpdateType
import ru.egfedo.kbrinna.config.Config
import ru.egfedo.kbrinna.database.question.Database
import ru.egfedo.kbrinna.database.question.DatabaseStorage
import ru.egfedo.kbrinna.database.user.UserDataStorage

class Commands {

    /**
     * /start command
     */
    @CommandHandler(["/start"])
    suspend fun start(user: User, bot: TelegramBot, update: MessageUpdate) {
        if (update.message.chat.type == ChatType.Private) {
            message {
                "Ну здравствуй.\nЯ беру ваши файлы с тестов по праву и собираю из них базы для тренировки.\n" +
                        "Жду и от тебя файлов с тестами! Подробнее о формате файлов в /add\n" +
                        "Если хочешь потренироваться то не тормози, в /tests заходи!\n" +
                        "Свою статистику можно увидеть в /stats"
            }.send(user, bot)

        }
    }

    /**
     * /tests command
     */
    @CommandHandler(["/tests"])
    suspend fun tests(user: User, bot: TelegramBot, update: MessageUpdate) {
        if (update.message.chat.type == ChatType.Private) {
            TestHandler.testCommand(user, bot)
        }
    }

    /**
     * Open category callback handler
     */
    @CommandHandler(["openCategory"], scope = [UpdateType.CALLBACK_QUERY])
    suspend fun openCategory(
        user: User,
        bot: TelegramBot,
        update: CallbackQueryUpdate,
        @ParamMapping("name") name: String
    ) {
        TestHandler.chooseFilter(user, bot, name)
    }

    /**
     * Choose amount callback handler
     */
    @CommandHandler(["chooseAmount"], scope = [UpdateType.CALLBACK_QUERY])
    suspend fun chooseAmountHandler(
        user: User,
        bot: TelegramBot,
        @ParamMapping("filter") filter: String,
        @ParamMapping("name") name: String,
        update: CallbackQueryUpdate
    ) {
        TestHandler.chooseAmount(user, bot, name, filter)
    }

    /**
     * Start test callback handler
     */
    @CommandHandler(["startTest"], scope = [UpdateType.CALLBACK_QUERY])
    suspend fun startTestHandler(
        user: User,
        bot: TelegramBot,
        update: CallbackQueryUpdate,
        @ParamMapping("amount") amount: Int,
        @ParamMapping("name") name: String,
        @ParamMapping("filter") filter: String
    ) {
        TestHandler.startTest(bot, user, name, amount, filter)
    }

    /**
     * Stats command
     */
    @CommandHandler(["/stats"])
    suspend fun stats(user: User, bot: TelegramBot, update: MessageUpdate) {
        val userData = UserDataStorage[user.id]

        // Init variables
        var encAmt = 0
        var wrongAmt = 0
        val encStr = StringBuilder()
        val wrongStr = StringBuilder()
        // Build strings
        Config().tests.forEach {
            // If test is single-based
            if (it.dbType == "Single") {
                val db = DatabaseStorage[it.id]
                val encountered = db.getAmount(user.id, Database.DataType.ENCOUNTERED)
                encAmt += encountered
                // Append string if encountered question amount isn't 0
                if (encountered != 0)
                    encStr.append("      <b>»</b> $encountered ")
                        .append("из ${db.size(user.id)} в базе по ${it.displayNames[1]}\n")
                val wrong = db.getAmount(user.id, Database.DataType.WRONG)
                wrongAmt += wrong
                // Append string if wrong answer amount isn't 0
                if (wrong != 0)
                    wrongStr.append("      <b>»</b> $wrong ")
                        .append("в базе по ${it.displayNames[1]}\n")
            }
        }

        message {
            "<b>Твоя Статистика:</b>" - "\n\n" +
                    "<b>- ${userData.correctAnswers} из ${userData.totalAnswers}</b>" + " правильных ответов\n\n" -
                    "<b>- $encAmt</b>" + " вопросов встречено в моей базе:\n" +  encStr.toString() +
                    "\n<b>- $wrongAmt</b>" + " вопросов, на которые ты неправильно ответил:\n" +
                    wrongStr.toString()
        }.options { parseMode = ParseMode.HTML }.send(user, bot)
    }

    /**
     * Unprocessed handler to handle test messages
     */
    @UnprocessedHandler
    suspend fun unprocessed(user: User, bot: TelegramBot, update: ProcessedUpdate) {
        if (update is MessageUpdate) {
            if (update.message.chat.type == ChatType.Private) {
                TestHandler.testRoutine(bot, user, update)
            }
        }
    }
}