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
                var currentQuestion = question ?: continue

                if (question == null) {
                    println("Все слова в словаре выучены!")
                    continue
                }

                while (true) {
                    // Используем extension-функцию для вывода вопроса
                    println(currentQuestion.asConsoleString())
                    print("Ваш выбор: ")

                    val userInput = readlnOrNull()?.toIntOrNull()
                    val variantsCount = currentQuestion.variants.size

                    if (userInput == null) {
                        println("Введите число от 0 до $variantsCount")
                        continue
                    }

                    when (userInput) {
                        0 -> {
                            println("Возврат в главное меню...")
                            break
                        }
                        in 1..variantsCount -> {
                            val userAnswerIndex = userInput - 1
                            val isCorrect = trainer.checkAnswer(userAnswerIndex)

                            if (isCorrect) {
                                println("Правильно!")
                                if (currentQuestion.correctAnswer.isLearned()) {
                                    println("Слово выучено!")
                                }
                            } else {
                                println("Неправильно! ${currentQuestion.correctAnswer.original} – это ${currentQuestion.correctAnswer.translate}")
                            }

                            val nextQuestion = trainer.getNextQuestion()
                            if (nextQuestion == null) {
                                println("\nПоздравляем! Все слова выучены!")
                                break
                            }
                            question = nextQuestion
                            currentQuestion = question ?: continue
                            println("\nПродолжаем обучение")
                        }
                        else -> {
                            println("Введите число от 0 до $variantsCount")
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

fun Question.asConsoleString(): String {
    val variantsString = variants.mapIndexed { index, word ->
        " ${index + 1} - ${word.translate}"
    }.joinToString("\n")

    return "\n${correctAnswer.original}:\n$variantsString\n ----------\n 0 - Меню"
}
