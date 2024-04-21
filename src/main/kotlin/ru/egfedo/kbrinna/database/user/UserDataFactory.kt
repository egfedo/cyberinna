package ru.egfedo.kbrinna.database.user

import ru.egfedo.kbrinna.config.Config

/**
 * Constructs an empty user data.
 * @param userID user ID to set
 */
fun emptyUserData(userID: Long): UserData {
    return UserData(
        userID,
        0,
        0,
        Config().defaultDatabases.toSet(),
        -1
    )
}
