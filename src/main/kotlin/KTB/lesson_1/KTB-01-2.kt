package org.example.KTB.lesson_1

import java.io.File

class Word(
    val original: String,
    val translate: String,
    var correctAnswersCount: Int = 0
)

fun main() {
    val wordsFile = File("words.txt")
    val dictionary = mutableListOf<Word>()

    try {
        if (!wordsFile.exists()) {
            wordsFile.createNewFile()
            wordsFile.writeText("hello|привет|0\n")
            wordsFile.appendText("dog|собака|0\n")
            wordsFile.appendText("cat|кошка|0\n")
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
                    println(word)
                }
            }
        }
    } catch (e: Exception) {
        println(e.message)
    }
}