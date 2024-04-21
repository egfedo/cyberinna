package ru.egfedo.kbrinna.database.user

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.egfedo.kbrinna.config.Config
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * UserDataStorage - contains the persistent user data
 */
object UserDataStorage {
    /**
     * Map with data
     */
    private val userMap: MutableMap<Long, UserData> = mutableMapOf()

    /**
     * Path to data directory
     */
    private val path = Config().userDataPath

    /**
     * Gets user data for user id
     * @param key user id
     */
    operator fun get(key: Long): UserData {
        // If not initialised
        if (key !in userMap) {
            // Try to load from file
            val file = Path("$path/user_$key.json")
            if (file.exists()) {
                userMap[key] = Json.decodeFromString(file.readText())
            }
            else {
                // Else set to empty
                userMap[key] = emptyUserData(key)
            }
        }
        return userMap[key]!!
    }

    /**
     * Sets user data
     * @param key user id
     * @param value user data
     */
    operator fun set(key: Long, value: UserData) {
        // Save to json
        userMap[key] = value
        Path("${path}/user_${key}.json").writeText(Json.encodeToString(value))
    }
}