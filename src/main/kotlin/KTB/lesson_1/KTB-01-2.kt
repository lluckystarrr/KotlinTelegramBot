package org.example.KTB.lesson_1

import java.io.File

fun main() {

    val wordsFile = File("words.txt")
    wordsFile.createNewFile()
    wordsFile.writeText("hello привет")
    wordsFile.appendText("dog собака")
    wordsFile.appendText("cat кошка")
    wordsFile.readLines().forEach { println(it) }
}
