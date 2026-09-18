package org.example.KTB.lesson_1

import java.io.File

class FileUserDictionary(
    private val chatId: Long? = null,
    private val answersCountToLearn: Int = 3
) : IUserDictionaryExtended {

    private val dictionary = loadDictionary().toMutableList()

    override fun getNumOfLearnedWords(): Int =
        dictionary.count { it.isLearned(answersCountToLearn) }

    override fun getSize(): Int = dictionary.size

    override fun getLearnedWords(): List<Word> =
        dictionary.filter { it.isLearned(answersCountToLearn) }

    override fun getUnlearnedWords(): List<Word> =
        dictionary.filter { !it.isLearned(answersCountToLearn) }

    override fun setCorrectAnswersCount(
        word: String,
        correctAnswersCount: Int
    ) {
        dictionary.find { it.original == word }?.let {
            it.correctAnswersCount = correctAnswersCount
            saveDictionary()
        }
    }

    override fun resetUserProgress() {
        dictionary.forEach { it.correctAnswersCount = 0 }
        saveDictionary()
    }

    override fun addWordsFromFile(fileName: String): Boolean {
        val file = File(fileName)
        if (!file.exists()) {
            println("Файл не найден: $fileName")
            return false
        }

        return try {
            var addedWordsCount = 0

            file.readLines().forEach { line ->
                val parts = line.split("|")
                val original = parts.getOrNull(0)?.trim().orEmpty()
                val translate = parts.getOrNull(1)?.trim().orEmpty()

                if (original.isNotBlank() && translate.isNotBlank()) {
                    val exists = dictionary.any {
                        it.original.equals(original, ignoreCase = true) &&
                                it.translate.equals(translate, ignoreCase = true)
                    }

                    if (!exists) {
                        dictionary.add(
                            Word(
                                original = original,
                                translate = translate,
                                imagePath = parts.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() },
                                imageFileId = parts.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() }
                            )
                        )
                        addedWordsCount++
                    }
                }
            }

            if (addedWordsCount > 0) saveDictionary()
            println("Добавлено новых слов: $addedWordsCount")
            true
        } catch (e: Exception) {
            println("Ошибка добавления слов из файла: ${e.message}")
            false
        }
    }

    override fun saveImageFileId(word: Word, fileId: String) {
        dictionary.find {
            it.original == word.original && it.translate == word.translate
        }?.let {
            it.imageFileId = fileId
            saveDictionary()
        }
    }

    private fun loadDictionary(): List<Word> {
        val dictionary = mutableListOf<Word>()
        val wordsFile = if (chatId != null) File("words_$chatId.txt") else File("words.txt")

        try {
            if (!wordsFile.exists()) {
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

            wordsFile.readLines().forEach { line ->
                if (line.isNotBlank()) {
                    val parts = line.split("|")
                    val original = parts.getOrNull(0)?.trim().orEmpty()
                    val translate = parts.getOrNull(1)?.trim().orEmpty()
                    val count = parts.getOrNull(2)?.trim()?.toIntOrNull() ?: 0
                    val imagePath = parts.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() }
                    val imageFileId = parts.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() }

                    if (original.isNotBlank() && translate.isNotBlank()) {
                        dictionary.add(
                            Word(original, translate, count, imagePath, imageFileId)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            println("Ошибка загрузки словаря: ${e.message}")
        }

        return dictionary
    }

    private fun saveDictionary() {
        val wordsFile = if (chatId != null) File("words_$chatId.txt") else File("words.txt")
        try {
            wordsFile.writeText(
                dictionary.joinToString(separator = "", postfix = "") {
                    "${it.original}|${it.translate}|${it.correctAnswersCount}|${it.imagePath ?: ""}|${it.imageFileId ?: ""}\n"
                }
            )
        } catch (e: Exception) {
            println("Ошибка сохранения словаря: ${e.message}")
        }
    }
}
