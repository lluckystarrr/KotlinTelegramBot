package org.example.KTB.lesson_1

import java.io.File

data class Word(
    val original: String,
    val translate: String,
    var correctAnswersCount: Int = 0
) {
    fun isLearned(): Boolean = correctAnswersCount >= 3
}

data class Statistics(
    val learned: Int,
    val total: Int,
    val percent: Int
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word
)

class LearnWordsTrainer {
    private var question: Question? = null
    private val dictionary = loadDictionary()

    fun getStatistics(): Statistics {
        val learned = dictionary.filter { it.correctAnswersCount >= 3 }.size
        val total = dictionary.size
        val percent = if (total > 0) learned * 100 / total else 0
        return Statistics(learned, total, percent)
    }

    fun getNextQuestion(): Question? {
        val notLearnedList = dictionary.filter { it.correctAnswersCount < 3 }
        if (notLearnedList.isEmpty()) return null

        val wordsToTake = if (notLearnedList.size >= 4) 4 else notLearnedList.size
        val questionWords = notLearnedList.shuffled().take(wordsToTake)
        val correctAnswer = questionWords.random()

        question = Question(
            variants = questionWords.shuffled(),
            correctAnswer = correctAnswer
        )

        return question
    }

    fun checkAnswer(userAnswerIndex: Int?): Boolean {
        return question?.let {
            val correctAnswerId = it.variants.indexOf(it.correctAnswer)
            if (correctAnswerId == userAnswerIndex) {
                it.correctAnswer.correctAnswersCount++
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
                    val correctAnswersCount = splitLine.getOrNull(2)?.toIntOrNull() ?: 0

                    if (original.isNotBlank() && translate.isNotBlank()) {
                        dictionary.add(Word(original, translate, correctAnswersCount))
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
                wordsFile.appendText("${word.original}|${word.translate}|${word.correctAnswersCount}\n")
            }
        } catch (e: Exception) {
            println("Ошибка при сохранении словаря: ${e.message}")
        }
    }
}
