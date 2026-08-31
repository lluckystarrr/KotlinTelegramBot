package org.example.KTB.lesson_1

import java.io.File

fun main() {

    val wordsFile = File("words.txt")
    wordsFile.createNewFile()
    wordsFile.writeText("hello привет\n")
    wordsFile.appendText("dog собака\n")
    wordsFile.appendText("cat кошка\n")
    wordsFile.readLines().forEach { println(it) }
}
