package ru.egfedo.kbrinna.database.question.impl

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.egfedo.kbrinna.database.question.Database
import ru.egfedo.kbrinna.database.question.Question
import ru.egfedo.kbrinna.database.question.QuestionDTO
import javax.xml.crypto.Data
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText


/**
 * Database implementation which is based on a json format question storage.
 * @param path path to this database folder
 * @param prefix database display prefix
 * @param dbID database ID as recorded in DatabaseStorage
 * @see Database
 * @see DatabaseStorage
 */
class SingleDatabase(
    private val path: String,
    private val prefix: String,
    private val dbID: String
): Database {

    /**
     * User statistics for this database
     */
    @Serializable
    data class UserStats(
        /**
         * IDs of questions which were encountered by the user
         */
        val encounteredQuestions: Set<String>,
        /**
         * IDs of questions which were wrongly answered by the user
         */
        val wrongAnswerList: Set<String>
    )

    /**
     * A storage for database-related user data
     */
    private class UserData(private val path: String) {

        private val data: MutableMap<Long, UserStats> = mutableMapOf()

        /**
         * Gets user data
         * @param id user ID
         */
        operator fun get(id: Long): UserStats {
            // If not present
            if (!data.containsKey(id)) {
                // Load from json if it exists
                val file = Path("${path}/user_${id}.json")
                if (file.exists()) {
                    data[id] = Json.decodeFromString(file.readText())
                }
                else {
                    // Else create empty data
                    data[id] = UserStats(setOf(), setOf())
                }
            }
            return data[id]!!
        }

        /**
         * Sets user data for this database
         * @param id user ID
         * @param value new statistics
         */
        operator fun set(id: Long, value: UserStats) {
            data[id] = value
            // Save to json
            Path("${path}/user_${id}.json").writeText(Json.encodeToString(value))
        }
    }

    private val questionData: Map<Int, Question> = Json.decodeFromString<Map<Int, Question>>(Path("${path}/data.json").readText())
    private val userData: UserData = UserData(path)

    override fun getEntries(userID: Long, getMode: Database.GetMode): Sequence<QuestionDTO> {
        val data = userData[userID]
        var output = questionData
        // Filter the database depending on the get mode
        when (getMode) {
            Database.GetMode.NOT_ENCOUNTERED -> {
                output = output.filter { (key, _) ->
                    key.toString() !in data.encounteredQuestions
                }
            }
            Database.GetMode.WRONG -> {
                output = output.filter { (key, _) ->
                    key.toString() in data.wrongAnswerList
                }
            }
            else -> {}
        }
        // Map output from question to question DTO
        return output.map { (key, value) ->
            QuestionDTO.fromQuestion(
                value,
                "$prefix$key",
                dbID,
                "$key"
            )
        }.asSequence()
    }

    override fun size(userID: Long, getMode: Database.GetMode): Int {
        val data = userData[userID]
        // Size depends on get mode
        return when (getMode) {
            Database.GetMode.ALL -> questionData.size
            Database.GetMode.NOT_ENCOUNTERED -> questionData.size - data.encounteredQuestions.size
            Database.GetMode.WRONG -> data.wrongAnswerList.size
        }
    }

    override fun getAmount(userID: Long, type: Database.DataType): Int {
        val data = userData[userID]
        // Amount depends on the type
        return when (type) {
            Database.DataType.ENCOUNTERED -> data.encounteredQuestions.size
            Database.DataType.WRONG -> data.wrongAnswerList.size
        }
    }

    override fun checkEncountered(userID: Long, question: QuestionDTO): Boolean {
        val data = userData[userID]
        return question.originID in data.encounteredQuestions
    }

    override fun checkWrong(userID: Long, question: QuestionDTO): Boolean {
        val data = userData[userID]
        return question.originID in data.wrongAnswerList
    }

    override fun addEncountered(userID: Long, question: QuestionDTO) {
        val data = userData[userID]
        userData[userID] = data.copy(
            encounteredQuestions = data.encounteredQuestions + question.originID
        )
    }

    override fun addWrong(userID: Long, question: QuestionDTO) {
        val data = userData[userID]
        userData[userID] = data.copy(
            wrongAnswerList = data.wrongAnswerList + question.originID
        )
    }

    override fun removeWrong(userID: Long, question: QuestionDTO) {
        val data = userData[userID]
        userData[userID] = data.copy(
            wrongAnswerList = data.wrongAnswerList - question.originID
        )
    }


}