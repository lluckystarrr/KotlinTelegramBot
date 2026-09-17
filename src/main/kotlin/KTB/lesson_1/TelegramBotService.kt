package org.example.KTB.lesson_1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files

const val CALLBACK_LEARN_WORDS = "learn_words_clicked"
const val CALLBACK_STATISTICS = "statistics_clicked"
const val CALLBACK_RESET_STATISTICS = "reset_statistics_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
const val TELEGRAM_FILE_URL = "https://api.telegram.org/file/bot"

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: String,
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: InlineKeyboardMarkup? = null
)

@Serializable
data class InlineKeyboardMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboardButton>>
)

@Serializable
data class InlineKeyboardButton(
    val text: String,
    @SerialName("callback_data")
    val callbackData: String
)

@Serializable
data class AnswerCallbackQueryRequest(
    @SerialName("callback_query_id")
    val callbackQueryId: String
)

@Serializable
data class GetFileRequest(
    @SerialName("file_id")
    val fileId: String
)

@Serializable
data class GetFileResponse(
    val ok: Boolean,
    val result: TelegramFile? = null
)

@Serializable
data class TelegramFile(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long? = null,
    @SerialName("file_path")
    val filePath: String
)

@Serializable
data class SendPhotoResponse(
    val ok: Boolean,
    val result: TelegramPhotoMessage? = null
)

@Serializable
data class TelegramPhotoMessage(
    @SerialName("message_id")
    val messageId: Int? = null,
    val photo: List<TelegramPhotoSize> = emptyList()
)

@Serializable
data class TelegramPhotoSize(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    val width: Int,
    val height: Int,
    @SerialName("file_size")
    val fileSize: Long? = null
)

private fun File.toMultipartBody(
    chatId: String,
    hasSpoiler: Boolean,
    boundary: String
): MultipartBody {
    val mimeType =
        Files.probeContentType(toPath())
            ?: "application/octet-stream"

    val requestBody =
        asRequestBody(
            mimeType.toMediaType()
        )

    return MultipartBody.Builder(boundary)
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            "chat_id",
            chatId
        )
        .addFormDataPart(
            "photo",
            name,
            requestBody
        )
        .addFormDataPart(
            "has_spoiler",
            hasSpoiler.toString()
        )
        .build()
}

class TelegramBotService(
    private val botToken: String
) {

    private val client = OkHttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun getUpdates(updateId: Int): String {

        val url =
            "${TELEGRAM_API_BASE}${botToken}/getUpdates?offset=$updateId"

        val request =
            Request.Builder()
                .url(url)
                .build()

        return try {

            client.newCall(request)
                .execute()
                .use { response ->
                    response.body?.string() ?: ""
                }

        } catch (e: Exception) {

            println(
                "Ошибка при получении обновлений: ${e.message}"
            )

            ""
        }
    }

    fun getFile(fileId: String): String {

        val url =
            "${TELEGRAM_API_BASE}${botToken}/getFile"

        val requestBody =
            GetFileRequest(
                fileId = fileId
            )

        val jsonString =
            json.encodeToString(requestBody)

        val request =
            Request.Builder()
                .url(url)
                .post(
                    jsonString.toRequestBody(
                        "application/json".toMediaType()
                    )
                )
                .build()

        return try {

            client.newCall(request)
                .execute()
                .use { response ->
                    response.body?.string() ?: ""
                }

        } catch (e: Exception) {

            println(
                "Ошибка получения файла: ${e.message}"
            )

            ""
        }
    }

    fun downloadFile(
        filePath: String,
        fileName: String
    ): Boolean {

        val url =
            "$TELEGRAM_FILE_URL$botToken/$filePath"

        println(url)

        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .GET()
                .build()

        return try {

            val response: HttpResponse<InputStream> =
                HttpClient
                    .newHttpClient()
                    .send(
                        request,
                        HttpResponse.BodyHandlers.ofInputStream()
                    )

            println(
                "status code: ${response.statusCode()}"
            )

            if (response.statusCode() !in 200..299) {

                response.body().close()

                false

            } else {

                response.body().use { input ->

                    File(fileName)
                        .outputStream()
                        .use { output ->

                            input.copyTo(output)
                        }
                }

                true
            }

        } catch (e: Exception) {

            println(
                "Ошибка скачивания файла: ${e.message}"
            )

            false
        }
    }

    fun sendMessage(
        chatId: String,
        text: String,
        replyMarkup: InlineKeyboardMarkup? = null
    ) {

        val requestBody =
            SendMessageRequest(
                chatId = chatId,
                text = text,
                replyMarkup = replyMarkup
            )

        val jsonString =
            json.encodeToString(requestBody)

        sendPostRequest(
            "sendMessage",
            jsonString
        )
    }

    fun sendPhoto(
        chatId: String,
        imagePath: String,
        hasSpoiler: Boolean = false
    ): String? {

        val imageFile =
            File(imagePath)

        if (!imageFile.exists()) {

            println(
                "Файл изображения не найден: $imagePath"
            )

            return null
        }

        val boundary =
            "----TelegramBotBoundary${System.nanoTime()}"

        val multipartBody =
            imageFile.toMultipartBody(
                chatId = chatId,
                hasSpoiler = hasSpoiler,
                boundary = boundary
            )

        val url =
            "${TELEGRAM_API_BASE}${botToken}/sendPhoto"

        val request =
            Request.Builder()
                .url(url)
                .post(multipartBody)
                .build()

        return try {

            client.newCall(request)
                .execute()
                .use { response ->

                    val responseBody =
                        response.body?.string()

                    println(
                        "sendPhoto response: $responseBody"
                    )

                    if (
                        responseBody == null
                    ) {
                        null
                    } else {

                        val photoResponse =
                            json.decodeFromString<SendPhotoResponse>(
                                responseBody
                            )

                        photoResponse
                            .result
                            ?.photo
                            ?.maxByOrNull {
                                it.width * it.height
                            }
                            ?.fileId
                    }
                }

        } catch (e: Exception) {

            println(
                "Ошибка отправки изображения: ${e.message}"
            )

            null
        }
    }

    fun sendPhotoByFileId(
        chatId: String,
        fileId: String
    ): Boolean {

        @Serializable
        data class SendPhotoRequest(
            @SerialName("chat_id")
            val chatId: String,
            val photo: String
        )

        val requestBody =
            SendPhotoRequest(
                chatId = chatId,
                photo = fileId
            )

        val jsonString =
            json.encodeToString(requestBody)

        val url =
            "${TELEGRAM_API_BASE}${botToken}/sendPhoto"

        val body =
            jsonString.toRequestBody(
                "application/json; charset=utf-8"
                    .toMediaType()
            )

        val request =
            Request.Builder()
                .url(url)
                .post(body)
                .build()

        return try {

            client.newCall(request)
                .execute()
                .use { response ->

                    val responseBody =
                        response.body?.string()

                    println(
                        "sendPhoto by file_id response: " +
                                responseBody
                    )

                    response.isSuccessful
                }

        } catch (e: Exception) {

            println(
                "Ошибка отправки изображения по file_id: " +
                        "${e.message}"
            )

            false
        }
    }

    fun sendMenu(chatId: String) {

        val replyMarkup =
            InlineKeyboardMarkup(
                inlineKeyboard = listOf(

                    listOf(
                        InlineKeyboardButton(
                            text = "Учить слова",
                            callbackData =
                                CALLBACK_LEARN_WORDS
                        )
                    ),

                    listOf(
                        InlineKeyboardButton(
                            text = "Статистика",
                            callbackData =
                                CALLBACK_STATISTICS
                        )
                    ),

                    listOf(
                        InlineKeyboardButton(
                            text = "Сбросить статистику",
                            callbackData =
                                CALLBACK_RESET_STATISTICS
                        )
                    )
                )
            )

        sendMessage(
            chatId,
            "Главное меню:",
            replyMarkup
        )
    }

    fun sendQuestion(
        chatId: String,
        question: Question
    ) {

        val buttons =
            question.variants.mapIndexed { index, word ->

                InlineKeyboardButton(
                    text = word.translate,
                    callbackData =
                        "$CALLBACK_DATA_ANSWER_PREFIX$index"
                )
            }

        val replyMarkup =
            InlineKeyboardMarkup(
                inlineKeyboard =
                    listOf(buttons)
            )

        sendMessage(
            chatId,
            "Как переводится слово «${question.correctAnswer.original}»?",
            replyMarkup
        )
    }

    fun answerCallbackQuery(
        callbackQueryId: String
    ) {

        val requestBody =
            AnswerCallbackQueryRequest(
                callbackQueryId = callbackQueryId
            )

        val jsonString =
            json.encodeToString(requestBody)

        sendPostRequest(
            "answerCallbackQuery",
            jsonString
        )
    }

    private fun sendPostRequest(
        method: String,
        json: String
    ) {

        val url =
            "${TELEGRAM_API_BASE}${botToken}/$method"

        val body =
            json.toRequestBody(
                "application/json; charset=utf-8"
                    .toMediaType()
            )

        val request =
            Request.Builder()
                .url(url)
                .post(body)
                .build()

        try {

            client.newCall(request)
                .execute()
                .use { response ->

                    println(
                        response.body?.string()
                    )
                }

        } catch (e: Exception) {

            println(
                "Ошибка при отправке запроса: ${e.message}"
            )
        }
    }
}