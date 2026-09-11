package org.example.KTB.lesson_1

import java.io.File

data class Word(
    val original: String,
    val translate: String,
    var correctAnswersCount: Int = 0
) {
    fun isLearned(answersCountToLearn: Int): Boolean {
        return correctAnswersCount >= answersCountToLearn
    }
}

data class Statistics(
    val totalCount: Int,
    val learnedCount: Int
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word
)

class LearnWordsTrainer(
    private val answersCountToLearn: Int = 3,
    private val numberOfQuestionWords: Int = 4
) {
    private var question: Question? = null
    private val dictionary = loadDictionary()

    fun getStatistics(): Statistics {
        val learnedCount = dictionary.count {
            it.isLearned(answersCountToLearn)
        }
        val totalCount = dictionary.size

        return Statistics(
            totalCount = totalCount,
            learnedCount = learnedCount
        )
    }

    fun getNextQuestion(): Question? {
        val notLearnedWords = dictionary.filterNot {
            it.isLearned(answersCountToLearn)
        }

        if (notLearnedWords.isEmpty()) {
            return null
        }

        val correctAnswer = notLearnedWords.random()

        val otherVariants = dictionary
            .filter { it !== correctAnswer }
            .shuffled()
            .take((numberOfQuestionWords - 1).coerceAtLeast(0))

        val variants = (otherVariants + correctAnswer).shuffled()

        question = Question(
            variants = variants,
            correctAnswer = correctAnswer
        )

        return question
    }

    fun checkAnswer(userAnswerIndex: Int?): Boolean {
        return question?.let { currentQuestion ->
            val correctAnswerIndex =
                currentQuestion.variants.indexOf(currentQuestion.correctAnswer)

            if (correctAnswerIndex == userAnswerIndex) {
                currentQuestion.correctAnswer.correctAnswersCount++
                saveDictionary(dictionary)
                true
            } else {
                false
            }
        } ?: false
    }

    private fun loadDictionary(): List<Word> {
        val dictionary = mutableListOf<Word>()
        val wordsFile = File("words.txt")

        try {
            if (!wordsFile.exists()) {
                wordsFile.createNewFile()
                wordsFile.writeText("hello|привет|0\n")
                wordsFile.appendText("dog|собака|0\n")
                wordsFile.appendText("cat|кошка|0\n")
                wordsFile.appendText("apple|яблоко|0\n")
                wordsFile.appendText("book|книга|0\n")
                wordsFile.appendText("sun|солнце|0\n")
                wordsFile.appendText("moon|луна|0\n")
            }

            wordsFile.readLines().forEach { line ->
                if (line.isNotBlank()) {
                    val splitLine = line.split("|")
                    val original = splitLine.getOrNull(0) ?: ""
                    val translate = splitLine.getOrNull(1) ?: ""
                    val correctAnswersCount =
                        splitLine.getOrNull(2)?.toIntOrNull() ?: 0

                    if (original.isNotBlank() && translate.isNotBlank()) {
                        dictionary.add(
                            Word(
                                original = original,
                                translate = translate,
                                correctAnswersCount = correctAnswersCount
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            println("Ошибка при загрузке словаря: ${e.message}")
        }

        return dictionary
    }

    private fun saveDictionary(words: List<Word>) {
        val wordsFile = File("words.txt")

        try {
            wordsFile.writeText("")

            for (word in words) {
                wordsFile.appendText(
                    "${word.original}|${word.translate}|" +
                            "${word.correctAnswersCount}\n"
                )
            }
        } catch (e: Exception) {
            println("Ошибка при сохранении словаря: ${e.message}")
        }
    }
}