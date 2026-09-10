package org.example.KTB.lesson_1

const val TELEGRAM_API_BASE = "https://api.telegram.org/bot"

fun main(args: Array<String>) {
    val botToken = args[0]
    var updateId = 0
    val telegramBotService = TelegramBotService(botToken)
    val trainer = LearnWordsTrainer()
    val updateIdRegex = "\"update_id\"\\s*:\\s*(\\d+)".toRegex()
    val messageTextRegex = "\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"".toRegex()
    val chatIdRegex = "\"chat\"\\s*:\\s*\\{[^}]*\"id\"\\s*:\\s*(-?\\d+)".toRegex()
    val callbackQueryIdRegex = "\"callback_query\"\\s*:\\s*\\{.*?\"id\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"".toRegex()
    val callbackDataRegex = "\"callback_query\"\\s*:\\s*\\{.*?\"data\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"".toRegex()

    while (true) {
        Thread.sleep(2000)
        val updates = telegramBotService.getUpdates(updateId)
        println(updates)
        var startPos = 0

        while (true) {
            val updateMatch = updateIdRegex.find(updates, startPos) ?: break
            val updateIdString = updateMatch.groups?.get(1)?.value ?: break
            updateId = updateIdString.toInt() + 1

            val nextUpdateMatch = updateIdRegex.find(updates, updateMatch.range.last + 1)
            val endPos = nextUpdateMatch?.range?.first ?: updates.length
            val update = updates.substring(updateMatch.range.first, endPos)
            val callbackDataMatch = callbackDataRegex.find(update)

            if (callbackDataMatch != null) {
                val callbackData = callbackDataMatch.groups?.get(1)?.value?.let(::unescapeJson)
                val callbackQueryId = callbackQueryIdRegex.find(update)?.groups?.get(1)?.value?.let(::unescapeJson)
                val chatId = chatIdRegex.find(update)?.groups?.get(1)?.value

                println("callback_data = $callbackData")

                if (callbackQueryId != null) {
                    telegramBotService.answerCallbackQuery(callbackQueryId)
                }

                if (chatId != null && callbackData != null) {
                    when (callbackData) {
                        CALLBACK_LEARN_WORDS -> {
                            val question = trainer.getNextQuestion()

                            if (question == null) {
                                telegramBotService.sendMessage(chatId = chatId, text = "Все слова уже выучены!")
                            } else {
                                val variants = question.variants.mapIndexed { index, word -> "${index + 1}. ${word.translate}" }.joinToString("\n")
                                telegramBotService.sendMessage(chatId = chatId, text = "Как переводится слово «${question.correctAnswer.original}»?\n\n$variants")
                            }
                        }

                        CALLBACK_STATISTICS -> {
                            val statistics = trainer.getStatistics()
                            telegramBotService.sendMessage(chatId = chatId, text = "Изучено слов: ${statistics.learned} из ${statistics.total} (${statistics.percent}%)")
                        }
                    }
                }

                startPos = updateMatch.range.last + 1
                continue
            }

            val textMatch = messageTextRegex.find(update)
            val text = textMatch?.groups?.get(1)?.value?.let(::unescapeJson)
            val chatId = chatIdRegex.find(update)?.groups?.get(1)?.value

            if (text != null) {
                println(text)
            }

            if (chatId != null) {
                println(chatId)
            }

            if (text != null && chatId != null) {
                when (text) {
                    "/start" -> {
                        telegramBotService.sendMenu(chatId = chatId)
                    }

                    else -> {
                        telegramBotService.sendMessage(chatId = chatId, text = text)
                    }
                }
            }

            startPos = updateMatch.range.last + 1
        }
    }
}

private fun unescapeJson(value: String): String {
    return value.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t")
}
