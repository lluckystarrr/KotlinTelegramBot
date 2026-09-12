package org.example.KTB.lesson_1

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val TELEGRAM_API_BASE = "https://api.telegram.org/bot"

@Serializable
data class TelegramResponse(
    val ok: Boolean,
    val result: List<TelegramUpdate>
)

@Serializable
data class TelegramUpdate(
    val update_id: Int,
    val message: TelegramMessage? = null,
    val callback_query: TelegramCallbackQuery? = null
)

@Serializable
data class TelegramMessage(
    val message_id: Int? = null,
    val chat: TelegramChat,
    val text: String? = null
)

@Serializable
data class TelegramChat(
    val id: Long
)

@Serializable
data class TelegramCallbackQuery(
    val id: String,
    val data: String? = null,
    val message: TelegramMessage? = null
)

fun checkNextQuestionAndSend(
    trainer: LearnWordsTrainer,
    telegramBotService: TelegramBotService,
    chatId: String
) {
    val question = trainer.getNextQuestion()

    if (question == null) {
        telegramBotService.sendMessage(chatId = chatId, text = "Все слова в словаре выучены")
    } else {
        telegramBotService.sendQuestion(chatId, question)
    }
}

fun main(args: Array<String>) {
    val botToken = args[0]
    var updateId = 0
    val telegramBotService = TelegramBotService(botToken)
    val trainer = LearnWordsTrainer()
    val json = Json {
        ignoreUnknownKeys = true
    }

    while (true) {
        Thread.sleep(2000)

        val updatesJson = telegramBotService.getUpdates(updateId)
        println(updatesJson)

        val updates = json.decodeFromString<TelegramResponse>(updatesJson)

        for (update in updates.result) {
            updateId = update.update_id + 1

            val callbackQuery = update.callback_query

            if (callbackQuery != null) {
                val callbackData = callbackQuery.data
                val chatId = callbackQuery.message?.chat?.id?.toString()

                println("callback_data = $callbackData")

                telegramBotService.answerCallbackQuery(callbackQuery.id)

                if (chatId != null && callbackData != null) {
                    when {
                        callbackData == CALLBACK_LEARN_WORDS -> {
                            checkNextQuestionAndSend(trainer, telegramBotService, chatId)
                        }

                        callbackData == CALLBACK_STATISTICS -> {
                            val statistics = trainer.getStatistics()

                            telegramBotService.sendMessage(
                                chatId = chatId,
                                text = "Изучено слов: ${statistics.learnedCount} из ${statistics.totalCount}"
                            )
                        }

                        callbackData.startsWith(CALLBACK_DATA_ANSWER_PREFIX) -> {
                            val userAnswerIndex = callbackData.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()

                            val question = trainer.getCurrentQuestion()
                            val isCorrect = trainer.checkAnswer(userAnswerIndex)

                            if (isCorrect) {
                                telegramBotService.sendMessage(
                                    chatId = chatId,
                                    text = "Правильно!"
                                )
                            } else {
                                val correctAnswer = question?.correctAnswer

                                telegramBotService.sendMessage(
                                    chatId = chatId,
                                    text = "Неправильно! ${correctAnswer?.original} – это ${correctAnswer?.translate}"
                                )
                            }

                            checkNextQuestionAndSend(trainer, telegramBotService, chatId)
                        }
                    }
                }

                continue
            }

            val message = update.message

            if (message != null) {
                val text = message.text
                val chatId = message.chat.id.toString()

                if (text != null) {
                    println(text)
                }

                println(chatId)

                when (text) {
                    "/start" -> {
                        telegramBotService.sendMenu(chatId)
                    }

                    else -> {
                        if (text != null) {
                            telegramBotService.sendMessage(chatId, text)
                        }
                    }
                }
            }
        }
    }
}
