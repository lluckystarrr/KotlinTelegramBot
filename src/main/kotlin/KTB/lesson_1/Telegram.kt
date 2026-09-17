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


fun sendAndSaveMessage(
    chatId: Long,
    message: String,
    telegramBotService: TelegramBotService,
    dynamicMessage: DynamicMessage,
    replyMarkup: InlineKeyboardMarkup? = null
) {

    val messageId =
        telegramBotService.sendMessage(
            chatId = chatId.toString(),
            text = message,
            replyMarkup = replyMarkup
        )

    messageId?.let {
        dynamicMessage.setMessageId(it)
        dynamicMessage.addMessage(message)
    }
}


fun updateStatisticsMessage(
    chatId: Long,
    trainer: LearnWordsTrainer,
    telegramBotService: TelegramBotService,
    dynamicMessage: DynamicMessage
) {

    val message =
        createStatisticsMessage(trainer)

    val messageId =
        dynamicMessage.messageId

    if (messageId == null) {

        sendAndSaveMessage(
            chatId = chatId,
            message = message,
            telegramBotService = telegramBotService,
            dynamicMessage = dynamicMessage
        )

        return
    }


    val edited =
        telegramBotService.editMessage(
            chatId = chatId,
            messageId = messageId,
            message = message
        )


    if (edited) {

        dynamicMessage.addMessage(message)

    } else {

        sendAndSaveMessage(
            chatId = chatId,
            message = message,
            telegramBotService = telegramBotService,
            dynamicMessage = dynamicMessage
        )
    }
}


fun checkNextQuestionAndSend(
    trainer: LearnWordsTrainer,
    telegramBotService: TelegramBotService,
    chatId: Long,
    dynamicMessage: DynamicMessage
) {

    val question =
        trainer.getNextQuestion()


    if (question == null) {

        sendAndSaveMessage(
            chatId = chatId,
            message = "Все слова в словаре выучены",
            telegramBotService = telegramBotService,
            dynamicMessage = dynamicMessage
        )

        return
    }


    val correctAnswer =
        question.correctAnswer


    correctAnswer.imageFileId?.let { fileId ->

        telegramBotService.sendPhotoByFileId(
            chatId = chatId.toString(),
            fileId = fileId
        )

    } ?: correctAnswer.imagePath?.let { imagePath ->

        val fileId =
            telegramBotService.sendPhoto(
                chatId = chatId.toString(),
                imagePath = imagePath
            )


        fileId?.let {

            trainer.saveImageFileId(
                word = correctAnswer,
                fileId = it
            )
        }
    }


    val messageId =
        telegramBotService.sendQuestion(
            chatId = chatId.toString(),
            question = question
        )


    messageId?.let {

        dynamicMessage.setMessageId(it)
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
                                trainer = trainer,
                                telegramBotService = telegramBotService,
                                chatId = chatId,
                                dynamicMessage = dynamicMessage
                            )
                        }



                        callbackData ==
                                CALLBACK_STATISTICS -> {


                            sendAndSaveMessage(
                                chatId = chatId,
                                message = createStatisticsMessage(
                                    trainer
                                ),
                                telegramBotService = telegramBotService,
                                dynamicMessage = dynamicMessage
                            )
                        }



                        callbackData ==
                                CALLBACK_RESET_STATISTICS -> {


                            trainer.resetStatistics()



                            updateStatisticsMessage(
                                chatId = chatId,
                                trainer = trainer,
                                telegramBotService = telegramBotService,
                                dynamicMessage = dynamicMessage
                            )
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


                                sendAndSaveMessage(
                                    chatId = chatId,
                                    message = "Правильно!",
                                    telegramBotService = telegramBotService,
                                    dynamicMessage = dynamicMessage
                                )



                                updateStatisticsMessage(
                                    chatId = chatId,
                                    trainer = trainer,
                                    telegramBotService = telegramBotService,
                                    dynamicMessage = dynamicMessage
                                )



                            } else {


                                val correctAnswer =
                                    question?.correctAnswer



                                sendAndSaveMessage(
                                    chatId = chatId,
                                    message =
                                        "Неправильно! " +
                                                "${correctAnswer?.original} – это " +
                                                "${correctAnswer?.translate}",
                                    telegramBotService = telegramBotService,
                                    dynamicMessage = dynamicMessage
                                )
                            }



                            checkNextQuestionAndSend(
                                trainer = trainer,
                                telegramBotService = telegramBotService,
                                chatId = chatId,
                                dynamicMessage = dynamicMessage
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

                            sendAndSaveMessage(
                                chatId = chatId,
                                message = "Не удалось получить информацию о файле.",
                                telegramBotService = telegramBotService,
                                dynamicMessage = dynamicMessage
                            )

                            continue
                        }



                    val filePath =
                        fileResponse.result?.filePath



                    if (filePath == null) {


                        sendAndSaveMessage(
                            chatId = chatId,
                            message = "Не удалось получить путь к файлу.",
                            telegramBotService = telegramBotService,
                            dynamicMessage = dynamicMessage
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

                        sendAndSaveMessage(
                            chatId = chatId,
                            message = "Не удалось скачать файл.",
                            telegramBotService = telegramBotService,
                            dynamicMessage = dynamicMessage
                        )

                        continue
                    }



                    val wordsAdded =
                        trainer.addWordsFromFile(
                            fileName
                        )



                    if (wordsAdded) {

                        sendAndSaveMessage(
                            chatId = chatId,
                            message = "Файл обработан. Слова добавлены!",
                            telegramBotService = telegramBotService,
                            dynamicMessage = dynamicMessage
                        )

                    } else {

                        sendAndSaveMessage(
                            chatId = chatId,
                            message = "Не удалось обработать файл.",
                            telegramBotService = telegramBotService,
                            dynamicMessage = dynamicMessage
                        )
                    }


                    continue
                }



                when (message.text) {


                    "/start" -> {


                        val messageId =
                            telegramBotService.sendMenu(
                                chatId.toString()
                            )


                        messageId?.let {

                            dynamicMessage.setMessageId(it)
                        }
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


                            val edited =
                                telegramBotService.editMessage(
                                    chatId = chatId,
                                    messageId = messageId,
                                    message = previousMessage
                                )



                            if (!edited) {


                                sendAndSaveMessage(
                                    chatId = chatId,
                                    message = previousMessage,
                                    telegramBotService = telegramBotService,
                                    dynamicMessage = dynamicMessage
                                )
                            }


                        } else {


                            sendAndSaveMessage(
                                chatId = chatId,
                                message = "Нет предыдущего сообщения для отмены.",
                                telegramBotService = telegramBotService,
                                dynamicMessage = dynamicMessage
                            )
                        }
                    }



                    else -> {


                        message.text?.let {


                            sendAndSaveMessage(
                                chatId = chatId,
                                message = it,
                                telegramBotService = telegramBotService,
                                dynamicMessage = dynamicMessage
                            )
                        }
                    }
                }
            }
        }
    }
}