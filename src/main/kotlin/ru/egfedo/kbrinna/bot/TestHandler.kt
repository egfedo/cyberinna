package ru.egfedo.kbrinna.bot

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.SendMessageAction
import eu.vendeli.tgbot.api.message.deleteMessage
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.Message
import eu.vendeli.tgbot.types.ParseMode
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.MessageUpdate
import eu.vendeli.tgbot.types.internal.Response
import eu.vendeli.tgbot.types.internal.getOrNull
import eu.vendeli.tgbot.types.keyboard.InlineKeyboardMarkup
import eu.vendeli.tgbot.types.keyboard.ReplyKeyboardRemove
import eu.vendeli.tgbot.utils.builders.inlineKeyboardMarkup
import eu.vendeli.tgbot.utils.builders.replyKeyboardMarkup
import kotlinx.coroutines.delay
import ru.egfedo.kbrinna.bot.data.TempUserData
import ru.egfedo.kbrinna.config.Config
import ru.egfedo.kbrinna.config.TestData
import ru.egfedo.kbrinna.database.question.Database
import ru.egfedo.kbrinna.database.question.DatabaseStorage
import ru.egfedo.kbrinna.database.question.QuestionDTO
import ru.egfedo.kbrinna.database.user.UserDataStorage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.math.round

object TestHandler {

    private val TESTS_KEYBOARD: InlineKeyboardMarkup
    private val TESTS_DISPLAY: String
    private val TESTS: Map<String, TestData>

    private val tempData: ConcurrentMap<Long, TempUserData> = ConcurrentHashMap()
    private val waitMap: ConcurrentMap<Long, Boolean> = ConcurrentHashMap()

    // Building tests keyboard and tests display
    init {
        val tests = Config().tests
        TESTS_KEYBOARD = inlineKeyboardMarkup {
            for (i in tests.indices) {
                val test = tests[i]
                test.displayNames[0] callback "openCategory?name=${test.id}"
                if (i % 2 == 0) {
                    br()
                }
            }
        }
        TESTS_DISPLAY = buildString {
            for (test in tests) {
                append("- ${test.displayNames[1]}\n")
            }
        }
        TESTS = tests.map { it.id to it }.toMap()
    }

    /**
     * Deletes previously sent message.
     * @param user User to apply
     * @param bot Telegram bot instance
     */
    private suspend fun deletePrevMessage(user: User, bot: TelegramBot) {
        // Get user data
        val userData = UserDataStorage[user.id]
        // If last message is set
        if (userData.lastMessageID > 0) {
            // Delete it
            deleteMessage(userData.lastMessageID).send(user, bot)
            UserDataStorage[user.id] = userData.copy(lastMessageID = -1)
        }
    }

    /**
     * Buffers sent message.
     * @param user User to apply
     * @param response async send message request response
     */
    private fun bufferPrevMessage(user: User, response: Response<out Message>) {
        val sent = response.getOrNull()
        val id = sent?.messageId ?: return
        val userData = UserDataStorage[user.id]
        UserDataStorage[user.id] = userData.copy(lastMessageID = id)
    }

    /**
     * Test command handler
     * @param user User to apply
     * @param bot Telegram bot instance
     */
    suspend fun testCommand(user: User, bot: TelegramBot) {
        deletePrevMessage(user, bot)

        // Sends an answer to user's command
        val response = message {
            bold { "Тебе чего, тесты нужны?" } - "\nЩас у меня есть тесты по:\n" -
                    TESTS_DISPLAY - "\nВыбирай, что по душе"
        }.markup(TESTS_KEYBOARD).sendAsync(user, bot).await()
        bufferPrevMessage(user, response)
    }

    /**
     * Handler for choosing a db filter
     * @param user User to apply
     * @param bot Telegram bot instance
     * @param categoryName Test id
     */
    suspend fun chooseFilter(user: User, bot: TelegramBot, categoryName: String) {
        deletePrevMessage(user, bot)

        val userData = UserDataStorage[user.id]

        // Check if database access is allowed
        if (categoryName !in userData.allowedDBs) {
            message { "Кажись у тебя нет прав (хе-хе) на пользование этой базой. Подробнее в /add" }.send(user, bot)
            return
        }

        val id = user.id
        val db = DatabaseStorage[categoryName]

        // If encountered question amount is between 0 and database size
        // or wrong answer amount is greater than 0
        if ((db.getAmount(id, Database.DataType.ENCOUNTERED) > 0 &&
                    db.getAmount(id, Database.DataType.ENCOUNTERED) != db.size(id)) ||
            db.getAmount(id, Database.DataType.WRONG) != 0
        ) {
            // Format keyboard depending on the stats
            val keyboard = inlineKeyboardMarkup {
                "Все" callback "chooseAmount?name=$categoryName&filter=All"
                if (db.getAmount(id, Database.DataType.ENCOUNTERED) > 0 &&
                    db.getAmount(id, Database.DataType.ENCOUNTERED) != db.size(id)
                ) {
                    br()
                    "Новые" callback "chooseAmount?name=$categoryName&filter=New"
                }
                if (db.getAmount(id, Database.DataType.WRONG) != 0) {
                    br()
                    "Неправильные ответы" callback "chooseAmount?name=$categoryName&filter=Wrong"
                }
            }

            val answer: SendMessageAction
            // Format message depending on the stats
            if ((db.getAmount(id, Database.DataType.ENCOUNTERED) > 0 &&
                        db.getAmount(id, Database.DataType.ENCOUNTERED) != db.size(id)) &&
                db.getAmount(id, Database.DataType.WRONG) != 0
            ) {
                answer = message {
                    "О, я вижу, что ты у меня в базе по ${TESTS[categoryName]!!.displayNames[1]} видел" -
                            " <b>${db.getAmount(id, Database.DataType.ENCOUNTERED)} вопросов из ${db.size(id)}</b>" -
                            "." - "\nБерём все или только новые?\n Также у тебя " -
                            "<b>${db.getAmount(id, Database.DataType.WRONG)} неправильных ответов</b>" -
                            ", можем посмотреть их."
                }.options { parseMode = ParseMode.HTML}
            } else if (db.getAmount(id, Database.DataType.ENCOUNTERED) > 0 &&
                db.getAmount(id, Database.DataType.ENCOUNTERED) != db.size(id)
            ) {
                answer = message {
                    "О, я вижу, что ты у меня в базе по ${TESTS[categoryName]!!.displayNames[1]} видел" -
                            " <b>${db.getAmount(id, Database.DataType.ENCOUNTERED)} вопросов из ${db.size(id)}</b>" -
                            "." - "\nБерём все или только новые?"
                }.options { parseMode = ParseMode.HTML}
            } else if (db.getAmount(id, Database.DataType.WRONG) != 0) {
                answer = message {
                    "О, я вижу, что ты у меня в базе по ${TESTS[categoryName]!!.displayNames[1]} " -
                            "<b>неправильно ответил на ${db.getAmount(id, Database.DataType.WRONG)} вопросов</b>" -
                            ".\nПосмотрим их?"
                }.options { parseMode = ParseMode.HTML}
            } else {
                answer = message { "guh" }
            }
            // Show filter selection message
            val response = answer.markup(keyboard).sendAsync(user, bot).await()
            bufferPrevMessage(user, response)
        } else {
            // Go straight to choosing question amount
            chooseAmount(user, bot, categoryName, "All")
        }
    }

    /**
     * Handler for choosing entry amount
     * @param user User to apply
     * @param bot Telegram bot instance
     * @param name Test id
     * @param filter Database get filter
     */
    suspend fun chooseAmount(user: User, bot: TelegramBot, name: String, filter: String) {
        deletePrevMessage(user, bot)

        val db = DatabaseStorage[name]
        // Init keyboard
        val keyboard = inlineKeyboardMarkup {
            "5" callback "startTest?amount=5&name=${name}&filter=${filter}"
            "10" callback "startTest?amount=10&name=${name}&filter=${filter}"
            "20" callback "startTest?amount=20&name=${name}&filter=${filter}"
            newLine() // or br()
            "30" callback "startTest?amount=30&name=${name}&filter=${filter}"
            "50" callback "startTest?amount=50&name=${name}&filter=${filter}"
            "Все" callback "startTest?amount=1000&name=${name}&filter=${filter}"
        }
        // Set get mode depending on the filter
        val getMode = when (filter) {
            "All" -> Database.GetMode.ALL
            "New" -> Database.GetMode.NOT_ENCOUNTERED
            "Wrong" -> Database.GetMode.WRONG
            else -> Database.GetMode.ALL
        }
        // Send message
        val message = message {
            "Так-с, с учетом предпочтений у меня в базе по ${TESTS[name]!!.displayNames[1]} " -
                    "<b>${db.size(user.id, getMode)} вопросов</b>" -
                    ".\nСколько берёшь?"
        }.markup(keyboard).options { parseMode = ParseMode.HTML}

        val response = message.sendAsync(user, bot).await()
        bufferPrevMessage(user, response)
    }

    /**
     * Displays current question to user
     * @param user User to apply
     * @param bot Telegram bot instance
     */
    private suspend fun displayCurrentQuestion(bot: TelegramBot, user: User) {
        val data = tempData[user.id] ?: return
        if (data.questionIndex < 0)
            return
        val question = data.entries[data.questionIndex]

        // Format keyboard depending on the answer amount and
        // question type
        val keyboard = replyKeyboardMarkup {
            for (i in 1..question.answers.size) {
                +"$i"
                if (i % 2 == 0)
                    br()
            }
            if (question.type == QuestionDTO.Type.MULTIPLE) {
                br()
                +"Готово"
            }
        }
        keyboard.resizeKeyboard = true

        // Send message
        message {
            "<b>${data.questionIndex + 1}.</b> " +
                    data.entries[data.questionIndex].display() +
                    "Отправь мне сообщения с номерами ответов без проеблов"
        }.options { parseMode = ParseMode.HTML }.markup(keyboard).send(user, bot)
    }

    /**
     * Handler that starts a test.
     * @param user User to apply
     * @param bot Telegram bot instance
     * @param name Test id
     * @param filter Database get filter
     * @param amount Entry amount
     */
    suspend fun startTest(bot: TelegramBot, user: User, name: String, amount: Int, filter: String) {
        deletePrevMessage(user, bot)
        val data = tempData[user.id]
        bot.inputListener[user] = "testRunning"

        // If user is in a test
        if (data != null) {
            if (data.questionIndex != -1) {
                message { "Ты сейчас уже в тесте, куда ещё? Сначала заверши этот (/stop)" }.send(user, bot)
                return
            }
        }

        // Get entries with a filter
        val db = DatabaseStorage[name]
        val entries = when (filter) {
            "All" -> db.getEntries(user.id)
            "New" -> db.getEntries(user.id, Database.GetMode.NOT_ENCOUNTERED)
            "Wrong" -> db.getEntries(user.id, Database.GetMode.WRONG)
            else -> db.getEntries(user.id)
        }
        // Take the amount of entries
        val entryList = entries.shuffled().take(amount).toList()

        // Init temporary user data
        tempData[user.id] = TempUserData(
            entryList, 0, 0, "", listOf()
        )
        waitMap[user.id] = false

        // Send message
        message { "*Поехали\\!*" }.options { parseMode = ParseMode.MarkdownV2 }.send(user, bot)
        displayCurrentQuestion(bot, user)
    }

    /**
     * Test routine handler.
     * @param user User to apply
     * @param bot Telegram bot instance
     * @param update Message sent to bot
     */
    suspend fun testRoutine(bot: TelegramBot, user: User, update: MessageUpdate) {
        var data = tempData[user.id]!!

        // If current question is not defined
        if (data.questionIndex == -1)
            return
        // If user is already waiting for an answer
        if (waitMap[user.id]!!)
            return

        // Block any requests
        waitMap[user.id] = true

        // If user set a stop request
        if (update.message.text == "/stop") {
            // Send a statistic of current test
            if (data.questionIndex == 0)
                message {"Нихуя не сделал и ливнул, молодец!!!"}.markup(ReplyKeyboardRemove()).send(user, bot)
            else
                message {"Как скажешь. \nУ тебя ${data.correctCount} правильных ответов из ${data.questionIndex}. " +
                        "${round(data.correctCount.toFloat()/data.questionIndex.toFloat()*100).toInt()}%"}.markup(
                    ReplyKeyboardRemove()
                ).send(user, bot)
            tempData[user.id] = data.copy(questionIndex = -1)
            return
        }

        val msgText = update.message.text
        val currentQuestion = data.entries[data.questionIndex]

        // Checks that message text is present
        if (msgText == null) {
            waitMap[user.id] = false
            return
        }

        // Checks that user sent a number or done message
        if (msgText.toIntOrNull() == null && msgText != "Готово") {
            message {"И это студент, называется? Отправь число!"}.send(user, bot)
            waitMap[user.id] = false
            return
        }

        // If it's a multiple answer question and prompt isn't ready
        if (currentQuestion.type == QuestionDTO.Type.MULTIPLE) {
            if (msgText != "Готово") {
                // Buffer input
                tempData[user.id] = data.copy(inputBuffer = data.inputBuffer + msgText)
                waitMap[user.id] = false
                return
            }
        }
        else {
            // Else buffer is message
            data = data.copy(inputBuffer = msgText)
            tempData[user.id] = data
        }

        var correct = true
        val answers = currentQuestion.answers

        // Checks that input digits are correct
        for (char in data.inputBuffer) {
            if (char.digitToInt() > answers.size || char.digitToInt() == 0) {
                message {"Мракобесие! Такой цифры и нет даже! Ответь снова"}.send(user, bot)
                waitMap[user.id] = false
                return
            }
        }

        // Checks all the answers and resets the flag if wrong answers are present
        // Or there aren't enough correct answers
        answers.forEachIndexed { index, answer ->
            if ((answer.correct && (index + 1).toString() !in data.inputBuffer) ||
                (!answer.correct && (index + 1).toString() in data.inputBuffer)) {
                correct = false
            }
        }

        var dbUserData = UserDataStorage[user.id]
        val db = DatabaseStorage[currentQuestion.originDB]

        // Display messages
        if (correct) {
            message {"✅"}.send(user, bot)
            if (!db.checkWrong(user.id, currentQuestion))
                message { "И это правильный ответ!" }.send(user, bot)
            else {
                db.removeWrong(user.id, currentQuestion)
                message { "До этого ты ответил на этот вопрос неправильно!\nВ этот раз ты молодец, и я убрала этот вопрос из списка неправильных." }.send(user, bot)
            }
            // Update user data
            dbUserData = dbUserData.copy(correctAnswers = dbUserData.correctAnswers + 1)
            data = data.copy(correctCount = data.correctCount + 1)
            delay(1500L)
        }
        else {
            db.addWrong(user.id, currentQuestion)
            val correctAnswers : List<String> = answers.filter { it.correct }.map {it.text}
            message {"❌"}.send(user, bot)
            message {"Ответ неправильный. На колени.\n Правильные ответы: \n ${correctAnswers.joinToString(prefix ="- ", separator="\n- ")}"}.send(user, bot)
            delay(2500L)
        }

        // Update persistent and temporary user data
        db.addEncountered(user.id, currentQuestion)
        UserDataStorage[user.id] = dbUserData.copy(totalAnswers = dbUserData.totalAnswers + 1)

        tempData[user.id] = data.copy(inputBuffer = "", questionIndex = data.questionIndex + 1)
        data = tempData[user.id]!!

        // End the test if it's been passed
        if (data.questionIndex >= data.entries.size) {
            message {"${data.correctCount} правильных ответов из ${data.entries.size}. ${round(data.correctCount.toFloat() / data.entries.size.toFloat() * 100).toInt()}%\n" +
                    "Тест окончен, свободен"}.markup(ReplyKeyboardRemove()).send(user, bot)
            tempData[user.id] = data.copy(questionIndex = -1)
        }
        else {
            // Else display next question
            displayCurrentQuestion(bot, user)
            waitMap[user.id] = false
        }
    }

}