package org.example.KTB.lesson_1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val TELEGRAM_API_BASE = "https://api.telegram.org/bot"

@Serializable
data class TelegramResponse(
    val ok: Boolean,
    val result: List<TelegramUpdate> = emptyList(),
    val description: String? = null
)

@Serializable
data class TelegramUpdate(
    @SerialName("update_id")
    val updateId: Int,
    val message: TelegramMessage? = null,
    @SerialName("callback_query")
    val callbackQuery: TelegramCallbackQuery? = null
)

@Serializable
data class TelegramMessage(
    @SerialName("message_id")
    val messageId: Int? = null,
    val chat: TelegramChat,
    val text: String? = null,
    val document: Document? = null
)

@Serializable
data class Document(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_name")
    val fileName: String? = null,
    @SerialName("file_size")
    val fileSize: Long? = null
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

    val question =
        trainer.getNextQuestion()

    if (question == null) {

        telegramBotService.sendMessage(
            chatId = chatId,
            text = "Все слова в словаре выучены"
        )

    } else {

        val correctAnswer =
            question.correctAnswer

        correctAnswer.imageFileId?.let { fileId ->

            telegramBotService.sendPhotoByFileId(
                chatId = chatId,
                fileId = fileId
            )

        } ?: correctAnswer.imagePath?.let { imagePath ->

            val fileId =
                telegramBotService.sendPhoto(
                    chatId = chatId,
                    imagePath = imagePath
                )

            fileId?.let {
                trainer.saveImageFileId(
                    word = correctAnswer,
                    fileId = it
                )
            }
        }

        telegramBotService.sendQuestion(
            chatId = chatId,
            question = question
        )
    }
}

fun createStatisticsMessage(
    trainer: LearnWordsTrainer
): String {

    val statistics =
        trainer.getStatistics()

    val learnedCount =
        statistics.learnedCount

    val totalCount =
        statistics.totalCount

    val percent =
        if (totalCount == 0) {
            0
        } else {
            learnedCount * 100 / totalCount
        }

    val progressBarSize = 10

    val filledCount =
        percent * progressBarSize / 100

    val emptyCount =
        progressBarSize - filledCount

    val progressBar =
        "█".repeat(filledCount) +
                "░".repeat(emptyCount)

    return "Изучено слов: $learnedCount из $totalCount\n" +
            "Прогресс: [$progressBar] $percent%"
}

fun updateStatisticsMessage(
    chatId: Long,
    trainer: LearnWordsTrainer,
    telegramBotService: TelegramBotService,
    dynamicMessage: DynamicMessage
) {

    val messageId =
        dynamicMessage.messageId
            ?: return

    val message =
        createStatisticsMessage(trainer)

    if (
        telegramBotService.editMessage(
            chatId = chatId,
            messageId = messageId,
            message = message
        )
    ) {

        dynamicMessage.addMessage(message)
    }
}

fun main(args: Array<String>) {

    val botToken =
        args[0]

    var updateId = 0

    val telegramBotService =
        TelegramBotService(botToken)

    val trainers =
        HashMap<Long, LearnWordsTrainer>()

    val dynamicMessages =
        HashMap<Long, DynamicMessage>()

    val json =
        Json {
            ignoreUnknownKeys = true
        }

    while (true) {

        Thread.sleep(2000)

        val updatesJson =
            telegramBotService.getUpdates(
                updateId
            )

        val updates =
            try {

                json.decodeFromString<TelegramResponse>(
                    updatesJson
                )

            } catch (e: Exception) {

                println(
                    "Ошибка при обработке JSON: ${e.message}"
                )

                continue
            }

        for (update in updates.result) {

            updateId =
                update.updateId + 1

            val callbackQuery =
                update.callbackQuery

            if (callbackQuery != null) {

                val callbackData =
                    callbackQuery.data

                val chatId =
                    callbackQuery.message
                        ?.chat
                        ?.id

                telegramBotService.answerCallbackQuery(
                    callbackQuery.id
                )

                if (
                    chatId != null &&
                    callbackData != null
                ) {

                    val trainer =
                        trainers.getOrPut(chatId) {
                            LearnWordsTrainer(chatId)
                        }

                    val dynamicMessage =
                        dynamicMessages.getOrPut(chatId) {
                            DynamicMessage()
                        }

                    when {

                        callbackData ==
                                CALLBACK_LEARN_WORDS -> {

                            checkNextQuestionAndSend(
                                trainer,
                                telegramBotService,
                                chatId.toString()
                            )
                        }

                        callbackData ==
                                CALLBACK_STATISTICS -> {

                            val message =
                                createStatisticsMessage(
                                    trainer
                                )

                            val messageId =
                                telegramBotService.sendMessage(
                                    chatId = chatId.toString(),
                                    text = message
                                )

                            messageId?.let {
                                dynamicMessage.setMessageId(it)
                                dynamicMessage.addMessage(message)
                            }
                        }

                        callbackData ==
                                CALLBACK_RESET_STATISTICS -> {

                            trainer.resetStatistics()

                            val messageId =
                                dynamicMessage.messageId

                            if (messageId != null) {

                                val message =
                                    createStatisticsMessage(
                                        trainer
                                    )

                                if (
                                    telegramBotService.editMessage(
                                        chatId = chatId,
                                        messageId = messageId,
                                        message = message
                                    )
                                ) {

                                    dynamicMessage.addMessage(
                                        message
                                    )
                                }

                            } else {

                                telegramBotService.sendMessage(
                                    chatId.toString(),
                                    "Статистика сброшена!"
                                )
                            }
                        }

                        callbackData.startsWith(
                            CALLBACK_DATA_ANSWER_PREFIX
                        ) -> {

                            val userAnswerIndex =
                                callbackData
                                    .substringAfter(
                                        CALLBACK_DATA_ANSWER_PREFIX
                                    )
                                    .toInt()

                            val question =
                                trainer.getCurrentQuestion()

                            val isCorrect =
                                trainer.checkAnswer(
                                    userAnswerIndex
                                )

                            if (isCorrect) {

                                telegramBotService.sendMessage(
                                    chatId.toString(),
                                    "Правильно!"
                                )

                                updateStatisticsMessage(
                                    chatId = chatId,
                                    trainer = trainer,
                                    telegramBotService =
                                        telegramBotService,
                                    dynamicMessage =
                                        dynamicMessage
                                )

                            } else {

                                val correctAnswer =
                                    question?.correctAnswer

                                telegramBotService.sendMessage(
                                    chatId.toString(),
                                    "Неправильно! " +
                                            "${correctAnswer?.original} – это " +
                                            "${correctAnswer?.translate}"
                                )
                            }

                            checkNextQuestionAndSend(
                                trainer,
                                telegramBotService,
                                chatId.toString()
                            )
                        }
                    }
                }

                continue
            }

            val message =
                update.message

            if (message != null) {

                val chatId =
                    message.chat.id

                val trainer =
                    trainers.getOrPut(chatId) {
                        LearnWordsTrainer(chatId)
                    }

                val dynamicMessage =
                    dynamicMessages.getOrPut(chatId) {
                        DynamicMessage()
                    }

                if (message.document != null) {

                    val fileId =
                        message.document.fileId

                    val fileJson =
                        telegramBotService.getFile(
                            fileId
                        )

                    val fileResponse =
                        try {

                            json.decodeFromString<GetFileResponse>(
                                fileJson
                            )

                        } catch (e: Exception) {

                            println(
                                "Ошибка при получении информации о файле: " +
                                        "${e.message}"
                            )

                            telegramBotService.sendMessage(
                                chatId.toString(),
                                "Не удалось получить информацию о файле."
                            )

                            continue
                        }

                    val filePath =
                        fileResponse.result?.filePath

                    if (filePath == null) {

                        telegramBotService.sendMessage(
                            chatId.toString(),
                            "Не удалось получить путь к файлу."
                        )

                        continue
                    }

                    val fileName =
                        "download_${message.document.fileUniqueId}.txt"

                    val downloaded =
                        telegramBotService.downloadFile(
                            filePath,
                            fileName
                        )

                    if (!downloaded) {

                        telegramBotService.sendMessage(
                            chatId.toString(),
                            "Не удалось скачать файл."
                        )

                        continue
                    }

                    val wordsAdded =
                        trainer.addWordsFromFile(
                            fileName
                        )

                    if (wordsAdded) {

                        telegramBotService.sendMessage(
                            chatId.toString(),
                            "Файл обработан. Слова добавлены!"
                        )

                    } else {

                        telegramBotService.sendMessage(
                            chatId.toString(),
                            "Не удалось обработать файл."
                        )
                    }

                    continue
                }

                when (message.text) {

                    "/start" -> {

                        telegramBotService.sendMenu(
                            chatId.toString()
                        )
                    }

                    "/undo" -> {

                        val messageId =
                            dynamicMessage.messageId

                        val previousMessage =
                            dynamicMessage.getPreviousMessage()

                        if (
                            messageId != null &&
                            previousMessage != null
                        ) {

                            telegramBotService.editMessage(
                                chatId = chatId,
                                messageId = messageId,
                                message = previousMessage
                            )

                        } else {

                            telegramBotService.sendMessage(
                                chatId.toString(),
                                "Нет предыдущего сообщения для отмены."
                            )
                        }
                    }

                    else -> {

                        if (message.text != null) {

                            telegramBotService.sendMessage(
                                chatId.toString(),
                                message.text
                            )
                        }
                    }
                }
            }
        }
    }
}
