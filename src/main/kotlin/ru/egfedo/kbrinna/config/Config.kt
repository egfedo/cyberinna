package ru.egfedo.kbrinna.config

import com.charleskorn.kaml.Yaml
import eu.vendeli.tgbot.utils.builders.replyKeyboardMarkup
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlin.io.path.Path
import kotlin.io.path.bufferedReader

private const val CONFIG_PATH = "config.yml"

/**
 * Config - encapsulates the config file. Singleton class with no public constructors.
 * @param token telegram bot token
 * @param tests bot test data
 * @param userDataPath path to directory which holds user data
 * @param defaultDatabases databases which should be allowed to the user by default
 */
@Serializable
class Config private constructor(
    val token: String,
    val tests: List<TestData>,
    val userDataPath: String,
    val defaultDatabases: List<String>
) {

    companion object {
        private var instance: Config? = null

        /**
         * Overloaded invoke operator which allows to mimic a class constructor
         */
        operator fun invoke(): Config {
            // If it isn't initialised yet
            if (instance == null) {
                // Load from yml
                val reader = Path("./config.yml").bufferedReader()
                val str = reader.readText()
                reader.close()
                instance = Yaml.default.decodeFromString(str)
            }
            return instance!!
        }
    }
}
