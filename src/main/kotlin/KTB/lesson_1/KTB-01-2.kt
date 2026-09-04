package org.example.KTB.lesson_1

fun main() {
    val trainer = LearnWordsTrainer()

    while (true) {
        println("\nМеню:")
        println("1. Учить слова")
        println("2. Статистика")
        println("0. Выход")
        print("Выберите пункт: ")

        val input = readlnOrNull() ?: ""

        when (input) {
            "1" -> {
                var question = trainer.getNextQuestion()

                if (question == null) {
                    println("Все слова в словаре выучены!")
                    continue
                }

                while (true) {
                    val currentQuestion = question  // сохраняем в локальную переменную

                    println("\n${currentQuestion?.correctAnswer?.original}:")
                    currentQuestion?.variants?.forEachIndexed { index, word ->
                        println(" ${index + 1} - ${word.translate}")
                    }
                    println(" ----------")
                    println(" 0 - Меню")
                    print("Ваш выбор: ")

                    val userInput = readlnOrNull()?.toIntOrNull()

                    if (userInput == null) {
                        println("Введите число от 0 до ${currentQuestion?.variants?.size}")
                        continue
                    }

                    when (userInput) {
                        0 -> {
                            println("Возврат в главное меню...")
                            break
                        }
                        in 1..currentQuestion?.variants?.size!! -> {
                            val userAnswerIndex = userInput - 1
                            val isCorrect = trainer.checkAnswer(userAnswerIndex)

                            if (isCorrect) {
                                println("Правильно!")
                                if (currentQuestion?.correctAnswer!!.isLearned()!!) {
                                    println("Слово выучено!")
                                }
                            } else {
                                println("Неправильно! ${currentQuestion?.correctAnswer?.original} – это ${currentQuestion?.correctAnswer?.translate}")
                            }

                            val nextQuestion = trainer.getNextQuestion()
                            if (nextQuestion == null) {
                                println("\nПоздравляем! Все слова выучены!")
                                break
                            }
                            question = nextQuestion
                            println("\nПродолжаем обучение")
                        }
                        else -> {
                            println("Введите число от 0 до ${currentQuestion.variants.size}")
                        }
                    }
                }
            }
            "2" -> {
                val statistics = trainer.getStatistics()
                println("Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percent}%")
            }
            "0" -> {
                println("До свидания!")
                return
            }
            else -> {
                println("Введите число 1, 2 или 0")
            }
        }
    }
}
