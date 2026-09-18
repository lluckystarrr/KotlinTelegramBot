package org.example.KTB.lesson_1

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

private const val DATABASE_URL = "jdbc:sqlite:words.db"

object Database {

    init {
        Class.forName("org.sqlite.JDBC")
        createTables()
        migrateSchema()
    }

    fun getConnection(): Connection {
        return DriverManager.getConnection(DATABASE_URL)
    }

    private fun createTables() {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS words (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        text VARCHAR NOT NULL UNIQUE,
                        translate VARCHAR NOT NULL,
                        image_path VARCHAR,
                        image_file_id VARCHAR
                    )
                    """.trimIndent()
                )

                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username VARCHAR,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        chat_id INTEGER UNIQUE
                    )
                    """.trimIndent()
                )

                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS user_answers (
                        user_id INTEGER NOT NULL,
                        word_id INTEGER NOT NULL,
                        correct_answer_count INTEGER NOT NULL DEFAULT 0,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (user_id, word_id),
                        FOREIGN KEY (user_id) REFERENCES users(id),
                        FOREIGN KEY (word_id) REFERENCES words(id)
                    )
                    """.trimIndent()
                )
            }
        }
    }

    private fun migrateSchema() {
        getConnection().use { connection ->
            val existingColumns = mutableSetOf<String>()

            connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA table_info(words)").use { rs ->
                    while (rs.next()) {
                        existingColumns.add(rs.getString("name"))
                    }
                }
            }

            if ("image_path" !in existingColumns) {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        "ALTER TABLE words ADD COLUMN image_path VARCHAR"
                    )
                }
            }

            if ("image_file_id" !in existingColumns) {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        "ALTER TABLE words ADD COLUMN image_file_id VARCHAR"
                    )
                }
            }
        }
    }
}

fun updateDictionary(wordsFile: File) {
    require(wordsFile.exists()) {
        "Файл словаря не найден: ${wordsFile.path}"
    }

    Database.getConnection().use { connection ->
        connection.prepareStatement(
            """
            INSERT INTO words (text, translate, image_path, image_file_id)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(text) DO UPDATE SET
                translate = excluded.translate,
                image_path = COALESCE(excluded.image_path, words.image_path),
                image_file_id = COALESCE(excluded.image_file_id, words.image_file_id)
            """.trimIndent()
        ).use { statement ->

            wordsFile.useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split("|")

                    val rawText = parts.getOrNull(0)?.trim().orEmpty()
                    val rawTranslate = parts.getOrNull(1)?.trim().orEmpty()
                    val imagePath = parts.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() }
                    val imageFileId = parts.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() }

                    if (rawText.isBlank() || rawTranslate.isBlank()) {
                        return@forEach
                    }

                    val text = try {
                        validateWordInput("word.text", rawText)
                    } catch (e: IllegalArgumentException) {
                        println("Пропущено слово: ${e.message}")
                        return@forEach
                    }

                    val translate = try {
                        validateWordInput("word.translate", rawTranslate)
                    } catch (e: IllegalArgumentException) {
                        println("Пропущен перевод: ${e.message}")
                        return@forEach
                    }

                    statement.setString(1, text)
                    statement.setString(2, translate)
                    statement.setString(3, imagePath)
                    statement.setString(4, imageFileId)
                    statement.addBatch()
                }
            }

            statement.executeBatch()
        }
    }
}
