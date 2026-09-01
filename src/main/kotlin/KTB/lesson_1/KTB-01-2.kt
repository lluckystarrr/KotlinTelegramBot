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
            "1" -> println("Вы выбрали пункт 'Учить слова'")
            "2" -> println("Вы выбрали пункт 'Статистика'")
            "0" -> {
                println("До свидания!")
                break
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
            wordsFile.writeText("hello привет")
            wordsFile.appendText("\n")
            wordsFile.appendText("dog собака")
            wordsFile.appendText("\n")
            wordsFile.appendText("cat кошка")
            wordsFile.appendText("\n")
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
