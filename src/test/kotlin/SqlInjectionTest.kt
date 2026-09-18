import org.example.KTB.lesson_1.*
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlInjectionTest {

    private val maliciousInputs = listOf(
        "'; DROP TABLE words; --",
        "admin' OR '1'='1",
        "' UNION SELECT * FROM users --",
        "test'; DELETE FROM words WHERE 1=1; --",
        "\" OR \"\"=\"",
        "word/*comment*/",
        "word--comment"
    )

    private val testFiles = mutableListOf<File>()

    @AfterTest
    fun cleanUp() {
        testFiles.forEach { it.delete() }
        testFiles.clear()

        Database.getConnection().use { connection ->
            maliciousInputs.forEach { input ->
                connection.prepareStatement(
                    "DELETE FROM words WHERE text = ?"
                ).use { statement ->
                    statement.setString(1, input)
                    statement.executeUpdate()
                }
            }
        }
    }

    @Test
    fun `updateDictionary ignores sql injection attempts`() {
        val file = File("test_injection_update.txt")
        testFiles.add(file)

        file.writeText(
            maliciousInputs.joinToString("\n") { "$it|перевод|0" }
        )

        updateDictionary(file)

        Database.getConnection().use { connection ->
            connection.prepareStatement(
                "SELECT COUNT(*) FROM words WHERE text = ?"
            ).use { statement ->
                maliciousInputs.forEach { input ->
                    statement.setString(1, input)
                    statement.executeQuery().use { rs ->
                        rs.next()
                        assertEquals(
                            0,
                            rs.getInt(1),
                            "Инъекция '$input' не должна попасть в таблицу words"
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `updateDictionary does not drop the words table`() {
        val file = File("test_injection_drop.txt")
        testFiles.add(file)

        file.writeText("'; DROP TABLE words; --|перевод|0")

        updateDictionary(file)

        Database.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT COUNT(*) FROM words").use { rs ->
                    assertTrue(rs.next())
                }
            }
        }
    }

    @Test
    fun `addWordsFromFile ignores sql injection attempts`() {
        val chatId = 999_001L
        val file = File("test_injection_add.txt")
        testFiles.add(file)

        file.writeText(
            maliciousInputs.joinToString("\n") { "$it|перевод|0" }
        )

        val dictionary = DatabaseUserDictionary(chatId)
        val result = dictionary.addWordsFromFile(file.name)

        assertTrue(result)

        Database.getConnection().use { connection ->
            connection.prepareStatement(
                "SELECT COUNT(*) FROM words WHERE text = ?"
            ).use { statement ->
                maliciousInputs.forEach { input ->
                    statement.setString(1, input)
                    statement.executeQuery().use { rs ->
                        rs.next()
                        assertEquals(
                            0,
                            rs.getInt(1),
                            "Инъекция '$input' не должна попасть в таблицу words"
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `validateWordInput rejects malicious strings`() {
        maliciousInputs.forEach { input ->
            var rejected = false
            try {
                validateWordInput("test", input)
            } catch (e: IllegalArgumentException) {
                rejected = true
            }
            assertTrue(rejected, "Инъекция '$input' должна быть отклонена валидацией")
        }
    }

    @Test
    fun `validateWordInput accepts clean words`() {
        val clean = listOf("hello", "привет", "well-known", "word 123")
        clean.forEach { word ->
            val result = validateWordInput("test", word)
            assertEquals(word, result)
        }
    }

    @Test
    fun `logSuspiciousInput does not throw`() {
        maliciousInputs.forEach { input ->
            logSuspiciousInput("test", input)
        }
        assertTrue(true)
    }
}