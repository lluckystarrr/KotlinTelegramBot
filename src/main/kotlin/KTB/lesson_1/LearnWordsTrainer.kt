package org.example.KTB.lesson_1

import java.io.File


data class Word(
    val original: String,
    val translate: String,
    var correctAnswersCount: Int = 0
) {

    fun isLearned(
        answersCountToLearn: Int
    ): Boolean {

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
    private val chatId: Long? = null,
    private val answersCountToLearn: Int = 3,
    private val numberOfQuestionWords: Int = 4
) {

    private var question: Question? = null

    private val dictionary =
        loadDictionary().toMutableList()


    fun getStatistics(): Statistics {

        val learnedCount =
            dictionary.count {
                it.isLearned(
                    answersCountToLearn
                )
            }

        return Statistics(
            totalCount = dictionary.size,
            learnedCount = learnedCount
        )
    }


    fun resetStatistics() {

        dictionary.forEach {
            it.correctAnswersCount = 0
        }

        saveDictionary(dictionary)
    }


    fun getNextQuestion(): Question? {

        val notLearnedWords =
            dictionary.filterNot {
                it.isLearned(
                    answersCountToLearn
                )
            }

        if (notLearnedWords.isEmpty()) {
            return null
        }

        val correctAnswer =
            notLearnedWords.random()

        val otherVariants =
            dictionary
                .filter {
                    it !== correctAnswer
                }
                .shuffled()
                .take(
                    (numberOfQuestionWords - 1)
                        .coerceAtLeast(0)
                )

        val variants =
            (otherVariants + correctAnswer)
                .shuffled()

        question =
            Question(
                variants = variants,
                correctAnswer = correctAnswer
            )

        return question
    }


    fun getCurrentQuestion(): Question? {
        return question
    }


    fun checkAnswer(
        userAnswerIndex: Int?
    ): Boolean {

        return question?.let { currentQuestion ->

            val correctAnswerIndex =
                currentQuestion.variants
                    .indexOf(
                        currentQuestion.correctAnswer
                    )

            if (
                correctAnswerIndex ==
                userAnswerIndex
            ) {

                currentQuestion.correctAnswer
                    .correctAnswersCount++

                saveDictionary(
                    dictionary
                )

                true

            } else {

                false
            }

        } ?: false
    }


    fun addWordsFromFile(
        fileName: String
    ) {

        val file =
            File(fileName)

        if (!file.exists()) {

            println(
                "Файл не найден: $fileName"
            )

            return
        }

        val newWords =
            file.readLines()
                .mapNotNull { line ->

                    val parts =
                        line.split("|")

                    if (
                        parts.size >= 2 &&
                        parts[0].isNotBlank() &&
                        parts[1].isNotBlank()
                    ) {

                        Word(
                            original = parts[0].trim(),
                            translate = parts[1].trim()
                        )

                    } else {

                        null
                    }
                }


        var addedWordsCount = 0


        newWords.forEach { newWord ->

            val wordAlreadyExists =
                dictionary.any { existingWord ->

                    existingWord.original
                        .trim()
                        .equals(
                            newWord.original.trim(),
                            ignoreCase = true
                        ) &&
                            existingWord.translate
                                .trim()
                                .equals(
                                    newWord.translate.trim(),
                                    ignoreCase = true
                                )
                }


            if (!wordAlreadyExists) {

                dictionary.add(newWord)

                addedWordsCount++
            }
        }


        saveDictionary(
            dictionary
        )


        println(
            "Добавлено новых слов: $addedWordsCount"
        )
    }


    private fun loadDictionary(): List<Word> {

        val dictionary =
            mutableListOf<Word>()

        val wordsFile =
            if (chatId != null) {

                File(
                    "words_$chatId.txt"
                )

            } else {

                File(
                    "words.txt"
                )
            }

        try {

            if (!wordsFile.exists()) {

                wordsFile.createNewFile()

                wordsFile.writeText(
                    "hello|привет|0\n" +
                            "dog|собака|0\n" +
                            "cat|кошка|0\n" +
                            "apple|яблоко|0\n" +
                            "book|книга|0\n" +
                            "sun|солнце|0\n" +
                            "moon|луна|0\n"
                )
            }

            wordsFile.readLines()
                .forEach { line ->

                    if (line.isNotBlank()) {

                        val splitLine =
                            line.split("|")

                        val original =
                            splitLine.getOrNull(0)
                                ?: ""

                        val translate =
                            splitLine.getOrNull(1)
                                ?: ""

                        val count =
                            splitLine
                                .getOrNull(2)
                                ?.toIntOrNull()
                                ?: 0

                        if (
                            original.isNotBlank() &&
                            translate.isNotBlank()
                        ) {

                            dictionary.add(
                                Word(
                                    original = original,
                                    translate = translate,
                                    correctAnswersCount = count
                                )
                            )
                        }
                    }
                }

        } catch (e: Exception) {

            println(
                "Ошибка загрузки словаря: ${e.message}"
            )
        }

        return dictionary
    }


    private fun saveDictionary(
        words: List<Word>
    ) {

        val wordsFile =
            if (chatId != null) {

                File(
                    "words_$chatId.txt"
                )

            } else {

                File(
                    "words.txt"
                )
            }

        try {

            wordsFile.writeText("")

            words.forEach { word ->

                wordsFile.appendText(
                    "${word.original}|" +
                            "${word.translate}|" +
                            "${word.correctAnswersCount}\n"
                )
            }

        } catch (e: Exception) {

            println(
                "Ошибка сохранения словаря: ${e.message}"
            )
        }
    }
}
