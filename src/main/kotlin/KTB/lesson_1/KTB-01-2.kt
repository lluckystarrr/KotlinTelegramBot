package org.example.KTB.lesson_1

import java.io.File
import java.io.IOException

fun main() {
    val wordsFile = File("words.txt")

    try {
        if (!wordsFile.exists()) {
            wordsFile.createNewFile()
            wordsFile.writeText("hello привет\n")
            wordsFile.appendText("dog собака\n")
            wordsFile.appendText("cat кошка\n")
        }

        wordsFile.readLines().forEach { line ->
            if (line.isNotBlank()) println(line)
        }
    } catch (e: IOException) {
        println(e.message)
    }
}
