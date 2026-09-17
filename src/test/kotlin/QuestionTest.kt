import org.example.KTB.lesson_1.Question
import org.example.KTB.lesson_1.Word
import org.example.KTB.lesson_1.asConsoleString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestionTest {

    @Test
    fun `asConsoleString with 4 variants`() {
        val cat = Word("cat", "кошка")
        val question = Question(
            variants = listOf(
                cat,
                Word("dog", "собака"),
                Word("sun", "солнце"),
                Word("moon", "луна")
            ),
            correctAnswer = cat
        )

        val result = question.asConsoleString()

        assertEquals(
            """
            cat:
             1 - кошка
             2 - собака
             3 - солнце
             4 - луна
             ----------
             0 - Меню
            """.trimIndent(),
            result.trim()
        )
    }


    @Test
    fun `asConsoleString with different variants order`() {
        val question = Question(
            variants = listOf(
                Word("moon", "луна"),
                Word("cat", "кошка"),
                Word("dog", "собака")
            ),
            correctAnswer = Word("cat", "кошка")
        )

        val result = question.asConsoleString()

        assertTrue(result.contains("1 - луна"))
        assertTrue(result.contains("2 - кошка"))
        assertTrue(result.contains("3 - собака"))
    }


    @Test
    fun `asConsoleString with empty variants`() {
        val question = Question(
            variants = emptyList(),
            correctAnswer = Word("cat", "кошка")
        )

        val result = question.asConsoleString()

        assertTrue(result.contains("cat:"))
        assertTrue(result.contains("0 - Меню"))
    }


    @Test
    fun `asConsoleString with 10 variants`() {
        val variants = (1..10).map {
            Word("word$it", "перевод$it")
        }

        val question = Question(
            variants = variants,
            correctAnswer = variants.first()
        )

        val result = question.asConsoleString()

        assertTrue(result.contains("10 - перевод10"))
    }


    @Test
    fun `asConsoleString with 200 variants`() {
        val variants = (1..200).map {
            Word("word$it", "перевод$it")
        }

        val question = Question(
            variants = variants,
            correctAnswer = variants.first()
        )

        val result = question.asConsoleString()

        assertTrue(result.contains("200 - перевод200"))
    }


    @Test
    fun `asConsoleString with special characters`() {
        val question = Question(
            variants = listOf(
                Word("test()", "скобки."),
                Word("pipe", "a|b")
            ),
            correctAnswer = Word("test()", "скобки.")
        )

        val result = question.asConsoleString()

        assertTrue(result.contains("скобки."))
        assertTrue(result.contains("a|b"))
    }


    @Test
    fun `asConsoleString with spaces in words`() {
        val question = Question(
            variants = listOf(
                Word("   ", "   ")
            ),
            correctAnswer = Word("   ", "   ")
        )

        val result = question.asConsoleString()

        assertTrue(result.contains("1 -"))
    }
}