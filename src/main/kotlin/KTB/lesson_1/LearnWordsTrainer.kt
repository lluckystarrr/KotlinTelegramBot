package org.example.KTB.lesson_1

import java.io.File

data class Word(
    val original: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
    val imagePath: String? = null,
    var imageFileId: String? = null
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
    private val chatId: Long? = null,
    private val answersCountToLearn: Int = 3,
    private val numberOfQuestionWords: Int = 4
) {
    private var question: Question? = null

    private val dictionary =
        loadDictionary().toMutableList()

    fun getStatistics(): Statistics {
        val totalCount = dictionary.size
        val learnedCount =
            dictionary.count {
                it.isLearned(answersCountToLearn)
            }

        return Statistics(
            totalCount = totalCount,
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
            dictionary.filter {
                !it.isLearned(answersCountToLearn)
            }

        if (notLearnedWords.isEmpty()) {
            question = null
            return null
        }

        val correctAnswer =
            notLearnedWords.random()

        val variants =
            dictionary
                .filter {
                    it.translate != correctAnswer.translate
                }
                .shuffled()
                .take(numberOfQuestionWords - 1)
                .toMutableList()

        variants.add(correctAnswer)
        variants.shuffle()

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

    fun checkAnswer(userAnswerIndex: Int?): Boolean {
        return question?.let { currentQuestion ->
            val correctAnswerIndex =
                currentQuestion.variants.indexOf(
                    currentQuestion.correctAnswer
                )

            if (correctAnswerIndex == userAnswerIndex) {
                currentQuestion
                    .correctAnswer
                    .correctAnswersCount++

                saveDictionary(dictionary)

                true
            } else {
                false
            }
        } ?: false
    }

    fun saveImageFileId(
        word: Word,
        fileId: String
    ) {
        val dictionaryWord =
            dictionary.find { currentWord ->
                currentWord.original == word.original &&
                        currentWord.translate == word.translate
            }

        if (dictionaryWord != null) {
            dictionaryWord.imageFileId = fileId

            saveDictionary(dictionary)

            println(
                "Сохранён file_id для слова: " +
                        "${dictionaryWord.original}"
            )
        }
    }

    fun addWordsFromFile(fileName: String): Boolean {
        val file = File(fileName)

        if (!file.exists()) {
            println("Файл не найден: $fileName")
            return false
        }

        return try {
            val newWords =
                file.readLines()
                    .mapNotNull { line ->
                        val splitLine =
                            line.split("|")

                        val original =
                            splitLine
                                .getOrNull(0)
                                ?.trim()
                                ?: ""

                        val translate =
                            splitLine
                                .getOrNull(1)
                                ?.trim()
                                ?: ""

                        if (
                            original.isBlank() ||
                            translate.isBlank()
                        ) {
                            null
                        } else {
                            Word(
                                original = original,
                                translate = translate
                            )
                        }
                    }

            var addedWordsCount = 0

            newWords.forEach { newWord ->
                val alreadyExists =
                    dictionary.any { existingWord ->
                        existingWord.original.equals(
                            newWord.original,
                            ignoreCase = true
                        ) &&
                                existingWord.translate.equals(
                                    newWord.translate,
                                    ignoreCase = true
                                )
                    }

                if (!alreadyExists) {
                    dictionary.add(newWord)
                    addedWordsCount++
                }
            }

            if (addedWordsCount > 0) {
                saveDictionary(dictionary)
            }

            println(
                "Добавлено новых слов: $addedWordsCount"
            )

            true
        } catch (e: Exception) {
            println(
                "Ошибка добавления слов из файла: " +
                        "${e.message}"
            )
            false
        }
    }

    private fun loadDictionary(): List<Word> {
        val dictionary = mutableListOf<Word>()

        val wordsFile =
            if (chatId != null) {
                File("words_$chatId.txt")
            } else {
                File("words.txt")
            }

        try {
            if (!wordsFile.exists()) {
                wordsFile.createNewFile()

                wordsFile.writeText(
                    "hello|привет|0|images/hello.jpg|\n" +
                            "dog|собака|0|images/dog.jpg|\n" +
                            "cat|кошка|0|images/cat.jpg|\n" +
                            "apple|яблоко|0\n" +
                            "book|книга|0\n" +
                            "sun|солнце|0\n" +
                            "moon|луна|0\n"
                )
            }

            val baseDictionary =
                if (chatId != null) {
                    loadBaseDictionary()
                } else {
                    emptyMap()
                }

            wordsFile.readLines()
                .forEach { line ->
                    if (line.isNotBlank()) {
                        val splitLine =
                            line.split("|")

                        val original =
                            splitLine
                                .getOrNull(0)
                                ?.trim()
                                ?: ""

                        val translate =
                            splitLine
                                .getOrNull(1)
                                ?.trim()
                                ?: ""

                        val count =
                            splitLine
                                .getOrNull(2)
                                ?.toIntOrNull()
                                ?: 0

                        var imagePath =
                            splitLine
                                .getOrNull(3)
                                ?.trim()
                                ?.takeIf {
                                    it.isNotBlank()
                                }

                        var imageFileId =
                            splitLine
                                .getOrNull(4)
                                ?.trim()
                                ?.takeIf {
                                    it.isNotBlank()
                                }

                        /*
                         * Если это пользовательский словарь
                         * и в нём ещё нет информации о картинке,
                         * берём её из основного words.txt.
                         */
                        if (
                            chatId != null &&
                            original.isNotBlank() &&
                            translate.isNotBlank()
                        ) {
                            val baseWord =
                                baseDictionary[
                                    "${original.lowercase()}|" +
                                            translate.lowercase()
                                ]

                            if (imagePath == null) {
                                imagePath =
                                    baseWord?.imagePath
                            }

                            if (imageFileId == null) {
                                imageFileId =
                                    baseWord?.imageFileId
                            }
                        }

                        if (
                            original.isNotBlank() &&
                            translate.isNotBlank()
                        ) {
                            dictionary.add(
                                Word(
                                    original = original,
                                    translate = translate,
                                    correctAnswersCount = count,
                                    imagePath = imagePath,
                                    imageFileId = imageFileId
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

    private fun loadBaseDictionary(): Map<String, Word> {
        val baseFile = File("words.txt")

        if (!baseFile.exists()) {
            return emptyMap()
        }

        val result = mutableMapOf<String, Word>()

        try {
            baseFile.readLines()
                .forEach { line ->
                    if (line.isNotBlank()) {
                        val splitLine =
                            line.split("|")

                        val original =
                            splitLine
                                .getOrNull(0)
                                ?.trim()
                                ?: ""

                        val translate =
                            splitLine
                                .getOrNull(1)
                                ?.trim()
                                ?: ""

                        val imagePath =
                            splitLine
                                .getOrNull(3)
                                ?.trim()
                                ?.takeIf {
                                    it.isNotBlank()
                                }

                        val imageFileId =
                            splitLine
                                .getOrNull(4)
                                ?.trim()
                                ?.takeIf {
                                    it.isNotBlank()
                                }

                        if (
                            original.isNotBlank() &&
                            translate.isNotBlank()
                        ) {
                            val key =
                                "${original.lowercase()}|" +
                                        translate.lowercase()

                            result[key] =
                                Word(
                                    original = original,
                                    translate = translate,
                                    imagePath = imagePath,
                                    imageFileId = imageFileId
                                )
                        }
                    }
                }
        } catch (e: Exception) {
            println(
                "Ошибка загрузки основного словаря: " +
                        "${e.message}"
            )
        }

        return result
    }

    private fun saveDictionary(words: List<Word>) {
        val wordsFile =
            if (chatId != null) {
                File("words_$chatId.txt")
            } else {
                File("words.txt")
            }

        try {
            wordsFile.writeText("")

            words.forEach { word ->
                wordsFile.appendText(
                    "${word.original}|" +
                            "${word.translate}|" +
                            "${word.correctAnswersCount}|" +
                            "${word.imagePath ?: ""}|" +
                            "${word.imageFileId ?: ""}\n"
                )
            }
        } catch (e: Exception) {
            println(
                "Ошибка сохранения словаря: ${e.message}"
            )
        }
    }
}