package org.example.KTB.lesson_1

import java.io.File

class DatabaseUserDictionary(
    private val chatId: Long,
    private val answersCountToLearn: Int = 3
) : IUserDictionaryExtended {

    private val userId: Long = getOrCreateUserId()

    override fun getNumOfLearnedWords(): Int {
        return getLearnedWords().size
    }

    override fun getSize(): Int {
        Database.getConnection().use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM words").use { statement ->
                statement.executeQuery().use { resultSet ->
                    return if (resultSet.next()) resultSet.getInt(1) else 0
                }
            }
        }
    }

    override fun getLearnedWords(): List<Word> =
        getWords(
            whereClause = "ua.correct_answer_count >= ?",
            thresholdParam = answersCountToLearn
        )

    override fun getUnlearnedWords(): List<Word> =
        getWords(
            whereClause = "(ua.correct_answer_count < ? OR ua.correct_answer_count IS NULL)",
            thresholdParam = answersCountToLearn
        )

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        Database.getConnection().use { connection ->

            val wordId: Long? = connection.prepareStatement(
                "SELECT id FROM words WHERE text = ?"
            ).use { statement ->
                statement.setString(1, word)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) resultSet.getLong(1) else null
                }
            }

            if (wordId == null) {
                println("Слово не найдено в словаре: $word")
                return
            }

            connection.prepareStatement(
                """
                INSERT INTO user_answers (user_id, word_id, correct_answer_count, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(user_id, word_id) DO UPDATE SET
                    correct_answer_count = excluded.correct_answer_count,
                    updated_at = CURRENT_TIMESTAMP
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, userId)
                statement.setLong(2, wordId)
                statement.setInt(3, correctAnswersCount)
                statement.executeUpdate()
            }
        }
    }

    override fun resetUserProgress() {
        Database.getConnection().use { connection ->
            connection.prepareStatement(
                "DELETE FROM user_answers WHERE user_id = ?"
            ).use { statement ->
                statement.setLong(1, userId)
                statement.executeUpdate()
            }
        }
    }

    override fun addWordsFromFile(fileName: String): Boolean {
        val file = File(fileName)
        if (!file.exists()) {
            println("Файл не найден: $fileName")
            return false
        }

        return try {
            Database.getConnection().use { connection ->
                connection.prepareStatement(
                    "INSERT OR IGNORE INTO words (text, translate) VALUES (?, ?)"
                ).use { statement ->
                    file.useLines { lines ->
                        lines.forEach { line ->
                            val parts = line.split("|")
                            val original = parts.getOrNull(0)?.trim().orEmpty()
                            val translate = parts.getOrNull(1)?.trim().orEmpty()
                            if (original.isNotBlank() && translate.isNotBlank()) {
                                statement.setString(1, original)
                                statement.setString(2, translate)
                                statement.addBatch()
                            }
                        }
                    }
                    statement.executeBatch()
                }
            }
            true
        } catch (e: Exception) {
            println("Ошибка добавления слов из файла: ${e.message}")
            false
        }
    }

    override fun saveImageFileId(word: Word, fileId: String) {
        println("Получен file_id для слова: ${word.original}: $fileId")
    }

    private fun getWords(
        whereClause: String,
        thresholdParam: Int
    ): List<Word> {
        val words = mutableListOf<Word>()

        Database.getConnection().use { connection ->
            connection.prepareStatement(
                """
                SELECT w.text, w.translate, COALESCE(ua.correct_answer_count, 0)
                FROM words w
                LEFT JOIN user_answers ua
                    ON ua.word_id = w.id AND ua.user_id = ?
                WHERE $whereClause
                ORDER BY w.id
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, userId)
                statement.setInt(2, thresholdParam)

                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        words.add(
                            Word(
                                original = resultSet.getString(1),
                                translate = resultSet.getString(2),
                                correctAnswersCount = resultSet.getInt(3)
                            )
                        )
                    }
                }
            }
        }

        return words
    }

    private fun getOrCreateUserId(): Long {
        Database.getConnection().use { connection ->
            connection.prepareStatement(
                "SELECT id FROM users WHERE chat_id = ?"
            ).use { statement ->
                statement.setLong(1, chatId)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) return resultSet.getLong(1)
                }
            }

            connection.prepareStatement(
                "INSERT INTO users (username, chat_id) VALUES (?, ?)"
            ).use { statement ->
                statement.setString(1, null)
                statement.setLong(2, chatId)
                statement.executeUpdate()
            }

            connection.prepareStatement(
                "SELECT id FROM users WHERE chat_id = ?"
            ).use { statement ->
                statement.setLong(1, chatId)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) return resultSet.getLong(1)
                }
            }
        }

        error("Не удалось создать пользователя для chatId=$chatId")
    }
}
