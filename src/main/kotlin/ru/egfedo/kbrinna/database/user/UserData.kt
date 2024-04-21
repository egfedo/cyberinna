package ru.egfedo.kbrinna.database.user

import kotlinx.serialization.Serializable

/**
 * Persistent user data
 */
@Serializable
data class UserData(
    /**
     * User telegram id.
     */
    val tgID: Long,
    /**
     * Correct answer amount.
     */
    val correctAnswers: Int,
    /**
     * Total answer amount.
     */
    val totalAnswers: Int,
    /**
     * Allowed databases' id.
     */
    val allowedDBs: Set<String>,
    /**
     * Last message sent by the user.
     */
    val lastMessageID: Long
)
