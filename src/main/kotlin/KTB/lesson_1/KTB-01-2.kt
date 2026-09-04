package org.example.KTB.lesson_1

import java.io.File

data class Word(
    val original: String,
    val translate: String,
    var correctAnswersCount: Int = 0
)

fun main() {
    val dictionary = loadDictionary()

    while (true) {
        println("\nМеню:")
        println("1. Учить слова")
        println("2. Статистика")
        println("0. Выход")
        print("Выберите пункт: ")

        val input = readlnOrNull() ?: ""

        when (input) {
            "1" -> {
                val notLearnedList = dictionary.filter { it.correctAnswersCount < 3 }.toMutableList()

                if (notLearnedList.isEmpty()) {
                    println("Все слова в словаре выучены!")
                    continue
                }

                while (notLearnedList.isNotEmpty()) {
                    val wordsToTake = if (notLearnedList.size >= 4) 4 else notLearnedList.size
                    val questionWords = notLearnedList.shuffled().take(wordsToTake)
                    val correctAnswer = questionWords.random()
                    val variants = questionWords.shuffled()

                    println("\n${correctAnswer.original}:")
                    variants.forEachIndexed { index, word ->
                        println(" ${index + 1} - ${word.translate}")
                    }
                    println(" ----------")
                    println(" 0 - Меню")

                    print("Ваш выбор: ")
                    val userAnswerInput = readlnOrNull()?.toIntOrNull()

                    when (userAnswerInput) {
                        null -> println("Введите число от 0 до ${variants.size}")
                        0 -> {
                            println("Возврат в главное меню...")
                            break
                        }
                        in 1..variants.size -> {
                            val selectedWord = variants[userAnswerInput - 1]
                            if (selectedWord == correctAnswer) {
                                println("Правильно!")
                                correctAnswer.correctAnswersCount++
                                val wordInDictionary = dictionary.find { it.original == correctAnswer.original }
                                wordInDictionary?.correctAnswersCount = correctAnswer.correctAnswersCount
                                saveDictionary(dictionary)

                                if (correctAnswer.correctAnswersCount >= 3) {
                                    notLearnedList.remove(correctAnswer)
                                    println("Слово выучено!")
                                }
                            } else {
                                println("Неправильно! ${correctAnswer.original} – это ${correctAnswer.translate}")
                            }
                        }
                        else -> println("Введите число от 0 до ${variants.size}")
                    }

                    if (notLearnedList.isEmpty()) {
                        println("\nПоздравляем! Все слова выучены!")
                        break
                    }

                    println("\nПродолжаем обучение")
                }
            }
            "2" -> {
                val totalCount = dictionary.size
                val learnedCount = dictionary.filter { it.correctAnswersCount >= 3 }.size
                val percent = if (totalCount > 0) {
                    (learnedCount * 100 / totalCount)
                } else {
                    0
                }

                println("Выучено $learnedCount из $totalCount слов | $percent%")
            }
            "0" -> {
                println("До свидания!")
                return
            }
            else -> println("Введите число 1, 2 или 0")
        }
    }
}

fun loadDictionary(): List<Word> {
    val wordsFile = File("words.txt")
    val dictionary = mutableListOf<Word>()

    try {
        if (!wordsFile.exists()) {
            wordsFile.createNewFile()
            wordsFile.writeText("hello|привет|0")
            wordsFile.appendText("\n")
            wordsFile.appendText("dog|собака|0")
            wordsFile.appendText("\n")
            wordsFile.appendText("cat|кошка|0")
            wordsFile.appendText("\n")
            wordsFile.appendText("apple|яблоко|0")
            wordsFile.appendText("\n")
            wordsFile.appendText("book|книга|0")
            wordsFile.appendText("\n")
            wordsFile.appendText("sun|солнце|0")
            wordsFile.appendText("\n")
            wordsFile.appendText("moon|луна|0")
        }

        val lines: List<String> = wordsFile.readLines()
        for (line in lines) {
            if (line.isNotBlank()) {
                val parts = line.split("|")
                val original = parts.getOrNull(0) ?: ""
                val translate = parts.getOrNull(1) ?: ""
                val correctAnswersCount = parts.getOrNull(2)?.toIntOrNull() ?: 0

                if (original.isNotBlank() && translate.isNotBlank()) {
                    val word = Word(original, translate, correctAnswersCount)
                    dictionary.add(word)
                }
            }
        }
    } catch (e: Exception) {
        println(e.message)
    }

    return dictionary
}

fun saveDictionary(dictionary: List<Word>) {
    val wordsFile = File("words.txt")

    try {
        val content = dictionary.joinToString("\n") { word ->
            "${word.original}|${word.translate}|${word.correctAnswersCount}"
        }
        wordsFile.writeText(content)
    } catch (e: Exception) {
        println("Ошибка при сохранении словаря: ${e.message}")
    }
}
