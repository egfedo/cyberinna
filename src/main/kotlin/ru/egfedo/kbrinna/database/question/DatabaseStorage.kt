package ru.egfedo.kbrinna.database.question

import ru.egfedo.kbrinna.config.Config
import ru.egfedo.kbrinna.database.question.impl.ComposedDatabase
import ru.egfedo.kbrinna.database.question.impl.SingleDatabase

/**
 * Database Storage which is initialized with the config data.
 */
object DatabaseStorage {
    private val storage: MutableMap<String, Database> = mutableMapOf()

    init {
        // Init databases depending on the config data
        Config().tests.forEach { data ->
            if (data.dbType == "Single") {
                storage[data.id] = SingleDatabase(data.dbFile, data.dbPrefix, data.id)
            }
            if (data.dbType == "Composed") {
                val map: MutableMap<String, Database> = mutableMapOf()
                data.nestedDBs.forEach {dbID ->
                    map[dbID] = storage[dbID]!!
                }
                storage[data.id] = ComposedDatabase(map)
            }
        }
    }

    operator fun get(key: String): Database {
        println("Getting database")
        return storage[key]!!
    }
}