package org.example.KTB.lesson_1

import java.io.File
import java.io.IOException

fun main() {
    val wordsFile = File("words.txt")

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

        wordsFile.readLines().forEach { line ->
            if (line.isNotBlank()) println(line)
        }
    } catch (e: IOException) {
        println(e.message)
    }
}
