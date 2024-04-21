package ru.egfedo.kbrinna.database.question


/**
 * Database interface. Its implementations are being used for encapsulating different db categories.
 */
interface Database {
    /**
     * Defines the Get mode which specifies what
     * database entries to get depending on user stats.
     */
    enum class GetMode {
        ALL, NOT_ENCOUNTERED, WRONG
    }

    /**
     * Defines the type of data to get in methods.
     */
    enum class DataType {
        ENCOUNTERED, WRONG
    }


    /**
     * Gets the entries which depend on the GetMode.
     * @param userID ID of user which is performing the operation
     * @param getMode Mode: All questions, questions not encountered by the user or question which have been answered wrongly before
     * @return Sequence which contains the appropriate question DTOs
     */
    fun getEntries(userID: Long, getMode: GetMode = GetMode.ALL): Sequence<QuestionDTO>

    /**
     * Gets the size of the database depending on the Get Mode. It represents the max amount of
     * entries which you are able to get with this Mode.
     * @param userID ID of user which is performing the operation
     * @param getMode Mode: All questions, questions not encountered by the user or question which have been answered wrongly before
     * @return Amount of entries
     */
    fun size(userID: Long, getMode: GetMode = GetMode.ALL): Int

    /**
     * Gets the amount of some user's metric.
     * @param userID ID of user which is performing the operation
     * @param type Get encountered amount or wrong answer amount
     * @return Specified statistic
     */
    fun getAmount(userID: Long, type: DataType): Int

    /**
     * Checks if a question was encountered by a user.
     * @param userID ID of user which is performing the operation
     * @param question Question to check
     * @return true or false depending on a check result
     */
    fun checkEncountered(userID: Long, question: QuestionDTO): Boolean

    /**
     * Checks if a question was wrongly answered by a user before.
     * @param userID ID of user which is performing the operation
     * @param question Question to check
     * @return true or false depending on a check result
     */
    fun checkWrong(userID: Long, question: QuestionDTO): Boolean

    /**
     * Adds a question to this user encountered list.
     * @param userID ID of user which is performing the operation
     * @param question Question to add
     */
    fun addEncountered(userID: Long, question: QuestionDTO)

    /**
     * Adds a question to this user wrongly answered list.
     * @param userID ID of user which is performing the operation
     * @param question Question to add
     */
    fun addWrong(userID: Long, question: QuestionDTO)

    /**
     * Removes a question from this user wrongly answered list.
     * @param userID ID of user which is performing the operation
     * @param question Question to remove
     */
    fun removeWrong(userID: Long, question: QuestionDTO)
}