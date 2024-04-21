package ru.egfedo.kbrinna.config

import kotlinx.serialization.Serializable

/**
 * Bot test data.
 */
@Serializable
data class TestData(
    /**
     * Test ID in the DatabaseStorage.
     */
    val id: String,
    /**
     * Database display prefix.
     */
    val dbPrefix: String,
    /**
     * Display name list. Two word forms are being used.
     */
    val displayNames: List<String>,
    /**
     * Database type. Only Single or Composed are being processed.
     */
    val dbType: String,
    /**
     * Single database folder path. It must contain data.json file to work.
     */
    val dbFile: String,
    /**
     * Nested database if the database is Composed. Nested DBs must be created before
     * the composed.
     */
    val nestedDBs: List<String>,
)
