package org.example.KTB.lesson_1

import java.io.File

class DatabaseUserDictionary(
    private val chatId: Long,
    private val answersCountToLearn: Int = 3
) : IUserDictionaryExtended {

    private val userId: Long = getOrCreateUserId()

    override fun getNumOfLearnedWords(): Int {
        Database.getConnection().use { connection ->
            connection.prepareStatement(
                """
                SELECT COUNT(*)
                FROM user_answers
                WHERE user_id = ? AND correct_answer_count >= ?
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, userId)
                statement.setInt(2, answersCountToLearn)

                statement.executeQuery().use { resultSet ->
                    return if (resultSet.next()) resultSet.getInt(1) else 0
                }
            }
        }
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
                    """
                    INSERT OR IGNORE INTO words (text, translate, image_path, image_file_id)
                    VALUES (?, ?, ?, ?)
                    """.trimIndent()
                ).use { wordStatement ->

                    connection.prepareStatement(
                        """
                        INSERT OR IGNORE INTO user_answers
                            (user_id, word_id, correct_answer_count, updated_at)
                        SELECT ?, id, ?, CURRENT_TIMESTAMP
                        FROM words
                        WHERE text = ?
                        """.trimIndent()
                    ).use { answerStatement ->

                        file.useLines { lines ->
                            lines.forEach { line ->
                                val parts = line.split("|")

                                val original = parts.getOrNull(0)?.trim().orEmpty()
                                val translate = parts.getOrNull(1)?.trim().orEmpty()
                                val count = parts.getOrNull(2)?.trim()?.toIntOrNull() ?: 0
                                val imagePath = parts.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() }
                                val imageFileId = parts.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() }

                                if (original.isNotBlank() && translate.isNotBlank()) {

                                    wordStatement.setString(1, original)
                                    wordStatement.setString(2, translate)
                                    wordStatement.setString(3, imagePath)
                                    wordStatement.setString(4, imageFileId)
                                    wordStatement.addBatch()

                                    // счётчик сохраняем только если > 0,
                                    // чтобы не плодить пустые записи
                                    if (count > 0) {
                                        answerStatement.setLong(1, userId)
                                        answerStatement.setInt(2, count)
                                        answerStatement.setString(3, original)
                                        answerStatement.addBatch()
                                    }
                                }
                            }
                        }

                        wordStatement.executeBatch()
                        answerStatement.executeBatch()
                    }
                }
            }
            true
        } catch (e: Exception) {
            println("Ошибка добавления слов из файла: ${e.message}")
            false
        }
    }

    override fun saveImageFileId(word: Word, fileId: String) {
        Database.getConnection().use { connection ->
            connection.prepareStatement(
                """
                UPDATE words
                SET image_file_id = ?
                WHERE text = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, fileId)
                statement.setString(2, word.original)
                statement.executeUpdate()
            }
        }
    }

    private fun getWords(
        whereClause: String,
        thresholdParam: Int
    ): List<Word> {
        val words = mutableListOf<Word>()

        Database.getConnection().use { connection ->
            connection.prepareStatement(
                """
                SELECT w.text, w.translate,
                       COALESCE(ua.correct_answer_count, 0),
                       w.image_path,
                       w.image_file_id
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
                                correctAnswersCount = resultSet.getInt(3),
                                imagePath = resultSet.getString(4),
                                imageFileId = resultSet.getString(5)
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

            // 3. Возвращаем id созданного
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
