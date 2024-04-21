package ru.egfedo.kbrinna.database.question.impl

import ru.egfedo.kbrinna.database.question.Database
import ru.egfedo.kbrinna.database.question.QuestionDTO

/**
 * Database implementation which represents a DB which is composed of multiple
 * other databases.
 * @param contents contents which the db is composed of
 */
class ComposedDatabase(private val contents: Map<String, Database>): Database {

    override fun getEntries(userID: Long, getMode: Database.GetMode): Sequence<QuestionDTO> {
        return contents.values.asSequence().flatMap { it.getEntries(userID, getMode) }
    }

    override fun size(userID: Long, getMode: Database.GetMode): Int {
        return contents.values.sumOf { it.size(userID, getMode) }
    }

    override fun getAmount(userID: Long, type: Database.DataType): Int {
        return contents.values.sumOf { it.getAmount(userID, type) }
    }

    override fun checkEncountered(userID: Long, question: QuestionDTO): Boolean {
        return contents[question.originDB]!!.checkEncountered(userID, question)
    }

    override fun checkWrong(userID: Long, question: QuestionDTO): Boolean {
        return contents[question.originDB]!!.checkWrong(userID, question)
    }

    override fun addEncountered(userID: Long, question: QuestionDTO) {
        contents[question.originDB]!!.addEncountered(userID, question)
    }

    override fun addWrong(userID: Long, question: QuestionDTO) {
        contents[question.originDB]!!.addWrong(userID, question)
    }

    override fun removeWrong(userID: Long, question: QuestionDTO) {
        contents[question.originDB]!!.removeWrong(userID, question)
    }
}