package org.example.KTB.lesson_1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

const val CALLBACK_LEARN_WORDS = "learn_words_clicked"
const val CALLBACK_STATISTICS = "statistics_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

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

class TelegramBotService(private val botToken: String) {

    private val client = OkHttpClient()
    private val json = Json

    fun getUpdates(updateId: Int): String {
        val url = "${TELEGRAM_API_BASE}${botToken}/getUpdates?offset=$updateId"
        val request = Request.Builder().url(url).build()

        return try {
            client.newCall(request).execute().use { response ->
                response.body?.string() ?: ""
            }
        } catch (e: Exception) {
            println("Ошибка при получении обновлений: ${e.message}")
            ""
        }
    }

    fun sendMessage(chatId: String, text: String, replyMarkup: InlineKeyboardMarkup? = null) {
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = text,
            replyMarkup = replyMarkup
        )

        val jsonString = json.encodeToString(requestBody)

        sendPostRequest("sendMessage", jsonString)
    }

    fun sendMenu(chatId: String) {
        val replyMarkup = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton(
                        text = "Учить слова",
                        callbackData = CALLBACK_LEARN_WORDS
                    )
                ),
                listOf(
                    InlineKeyboardButton(
                        text = "Статистика",
                        callbackData = CALLBACK_STATISTICS
                    )
                )
            )
        )

        sendMessage(chatId, "Главное меню:", replyMarkup)
    }

    fun sendQuestion(chatId: String, question: Question) {
        val buttons = question.variants.mapIndexed { index, word ->
            InlineKeyboardButton(
                text = word.translate,
                callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index"
            )
        }

        val replyMarkup = InlineKeyboardMarkup(
            inlineKeyboard = listOf(buttons)
        )

        sendMessage(
            chatId,
            "Как переводится слово «${question.correctAnswer.original}»?",
            replyMarkup
        )
    }

    fun answerCallbackQuery(callbackQueryId: String) {
        val requestBody = AnswerCallbackQueryRequest(
            callbackQueryId = callbackQueryId
        )

        val jsonString = json.encodeToString(requestBody)

        sendPostRequest("answerCallbackQuery", jsonString)
    }

    private fun sendPostRequest(method: String, json: String) {
        val url = "${TELEGRAM_API_BASE}${botToken}/$method"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(body).build()

        try {
            client.newCall(request).execute().use { response ->
                println(response.body?.string())
            }
        } catch (e: Exception) {
            println("Ошибка при отправке запроса: ${e.message}")
        }
    }
}
