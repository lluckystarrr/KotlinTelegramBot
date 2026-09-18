package org.example.KTB.lesson_1

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

private const val DATABASE_URL = "jdbc:sqlite:words.db"

object Database {

    init {
        Class.forName("org.sqlite.JDBC")
        createTables()
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
                        translate VARCHAR NOT NULL
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
}

fun updateDictionary(wordsFile: File) {
    require(wordsFile.exists()) {
        "Файл словаря не найден: ${wordsFile.path}"
    }

    Database.getConnection().use { connection ->
        connection.prepareStatement(
            """
            INSERT OR IGNORE INTO words (text, translate)
            VALUES (?, ?)
            """.trimIndent()
        ).use { statement ->

            wordsFile.useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split("|")

                    val text = parts
                        .getOrNull(0)
                        ?.trim()
                        .orEmpty()

                    val translate = parts
                        .getOrNull(1)
                        ?.trim()
                        .orEmpty()

                    if (text.isNotBlank() && translate.isNotBlank()) {
                        statement.setString(1, text)
                        statement.setString(2, translate)
                        statement.addBatch()
                    }
                }
            }

            statement.executeBatch()
        }
    }
}