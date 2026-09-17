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

        if (
            correctAnswer.imageFileId != null
        ) {

            telegramBotService.sendPhotoByFileId(
                chatId = chatId,
                fileId = correctAnswer.imageFileId!!
            )

        } else if (
            correctAnswer.imagePath != null
        ) {

            val fileId =
                telegramBotService.sendPhoto(
                    chatId = chatId,
                    imagePath = correctAnswer.imagePath
                )

            if (fileId != null) {

                trainer.saveImageFileId(
                    word = correctAnswer,
                    fileId = fileId
                )
            }
        }

        telegramBotService.sendQuestion(
            chatId = chatId,
            question = question
        )
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

                            val statistics =
                                trainer.getStatistics()

                            telegramBotService.sendMessage(
                                chatId.toString(),
                                "Изучено слов: " +
                                        "${statistics.learnedCount} из " +
                                        "${statistics.totalCount}"
                            )
                        }


                        callbackData ==
                                CALLBACK_RESET_STATISTICS -> {

                            trainer.resetStatistics()

                            telegramBotService.sendMessage(
                                chatId.toString(),
                                "Статистика сброшена!"
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

                                telegramBotService.sendMessage(
                                    chatId.toString(),
                                    "Правильно!"
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