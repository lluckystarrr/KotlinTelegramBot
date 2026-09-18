package org.example.KTB.lesson_1

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.io.File

class LearnWordsTrainerTest {

    private fun createTestFile(
        chatId: Long,
        content: String
    ) {
        File("words_$chatId.txt").writeText(
            content.trimIndent()
        )
    }

    private fun deleteTestFile(chatId: Long) {
        File("words_$chatId.txt").delete()
    }

    private fun createFileTrainer(
        chatId: Long,
        numberOfQuestionWords: Int = 4,
        answersCountToLearn: Int = 3
    ): LearnWordsTrainer =
        LearnWordsTrainer(
            chatId = chatId,
            answersCountToLearn = answersCountToLearn,
            numberOfQuestionWords = numberOfQuestionWords,
            dictionary = FileUserDictionary(
                chatId = chatId,
                answersCountToLearn = answersCountToLearn
            )
        )


    @Test
    fun `test statistics with 4 words of 7`() {
        val chatId = 1001L

        createTestFile(
            chatId,
            """
            define|определять|0
            solve|решать|0
            serve|служить|0
            empty|пустой|3
            full|полный|3
            narrow|узкий|3
            wide|широкий|3
            """
        )

        val trainer = createFileTrainer(chatId)

        assertEquals(
            Statistics(
                totalCount = 7,
                learnedCount = 4
            ),
            trainer.getStatistics()
        )

        deleteTestFile(chatId)
    }


    @Test
    fun `test statistics with corrupted file`() {
        val chatId = 1002L

        createTestFile(
            chatId,
            """
            define|определять|0
            wrong line
            |нет слова|0
            cat|кошка|abc
            """
        )

        val trainer = createFileTrainer(chatId)

        assertEquals(
            Statistics(
                totalCount = 2,
                learnedCount = 0
            ),
            trainer.getStatistics()
        )

        deleteTestFile(chatId)
    }


    @Test
    fun `test getNextQuestion with 5 unlearned words`() {
        val chatId = 1003L

        createTestFile(
            chatId,
            """
            one|один|0
            two|два|0
            three|три|0
            four|четыре|0
            five|пять|0
            """
        )

        val trainer = createFileTrainer(
            chatId = chatId,
            numberOfQuestionWords = 4
        )

        val question = trainer.getNextQuestion()

        assertNotNull(question)

        assertEquals(
            4,
            question.variants.size
        )

        assertTrue(
            question.variants.contains(question.correctAnswer)
        )

        deleteTestFile(chatId)
    }


    @Test
    fun `test getNextQuestion with 1 unlearned word`() {
        val chatId = 1004L

        createTestFile(
            chatId,
            """
            hello|привет|0
            dog|собака|3
            cat|кошка|3
            """
        )

        val trainer = createFileTrainer(chatId)

        val question = trainer.getNextQuestion()

        assertNotNull(question)

        assertEquals(
            "hello",
            question.correctAnswer.original
        )

        deleteTestFile(chatId)
    }


    @Test
    fun `test getNextQuestion with all words learned`() {
        val chatId = 1005L

        createTestFile(
            chatId,
            """
            hello|привет|3
            dog|собака|3
            """
        )

        val trainer = createFileTrainer(chatId)

        assertNull(
            trainer.getNextQuestion()
        )

        deleteTestFile(chatId)
    }


    @Test
    fun `test checkAnswer with true`() {
        val chatId = 1006L

        createTestFile(
            chatId,
            """
            cat|кошка|0
            dog|собака|0
            """
        )

        val trainer = createFileTrainer(chatId)

        val question = trainer.getNextQuestion()

        assertNotNull(question)

        val correctIndex =
            question.variants.indexOf(question.correctAnswer)

        assertTrue(
            trainer.checkAnswer(correctIndex)
        )

        deleteTestFile(chatId)
    }


    @Test
    fun `test checkAnswer with false`() {
        val chatId = 1007L

        createTestFile(
            chatId,
            """
            cat|кошка|0
            dog|собака|0
            """
        )

        val trainer = createFileTrainer(chatId)

        val question = trainer.getNextQuestion()

        assertNotNull(question)

        val wrongIndex =
            question.variants.indexOfFirst {
                it != question.correctAnswer
            }

        assertFalse(
            trainer.checkAnswer(wrongIndex)
        )

        deleteTestFile(chatId)
    }


    @Test
    fun `test resetProgress with 2 words in dictionary`() {
        val chatId = 1008L

        createTestFile(
            chatId,
            """
            cat|кошка|3
            dog|собака|3
            """
        )

        val trainer = createFileTrainer(chatId)

        assertEquals(
            Statistics(
                totalCount = 2,
                learnedCount = 2
            ),
            trainer.getStatistics()
        )

        trainer.resetStatistics()

        assertEquals(
            Statistics(
                totalCount = 2,
                learnedCount = 0
            ),
            trainer.getStatistics()
        )

        deleteTestFile(chatId)
    }
}