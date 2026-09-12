package org.example.KTB.lesson_1

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
    val chat_id: String,
    val text: String,
    val reply_markup: InlineKeyboardMarkup? = null
)

@Serializable
data class InlineKeyboardMarkup(
    val inline_keyboard: List<List<InlineKeyboardButton>>
)

@Serializable
data class InlineKeyboardButton(
    val text: String,
    val callback_data: String
)

@Serializable
data class AnswerCallbackQueryRequest(
    val callback_query_id: String
)

class TelegramBotService(private val botToken: String) {

    private val client = OkHttpClient()
    private val json = Json

    fun getUpdates(updateId: Int): String {
        val url = "${TELEGRAM_API_BASE}${botToken}/getUpdates?offset=$updateId"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            return response.body?.string() ?: ""
        }
    }

    fun sendMessage(chatId: String, text: String, replyMarkup: InlineKeyboardMarkup? = null) {
        val requestBody = SendMessageRequest(
            chat_id = chatId,
            text = text,
            reply_markup = replyMarkup
        )

        val jsonString = json.encodeToString(requestBody)

        sendPostRequest("sendMessage", jsonString)
    }

    fun sendMenu(chatId: String) {
        val replyMarkup = InlineKeyboardMarkup(
            inline_keyboard = listOf(
                listOf(
                    InlineKeyboardButton(
                        text = "Учить слова",
                        callback_data = CALLBACK_LEARN_WORDS
                    )
                ),
                listOf(
                    InlineKeyboardButton(
                        text = "Статистика",
                        callback_data = CALLBACK_STATISTICS
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
                callback_data = "$CALLBACK_DATA_ANSWER_PREFIX$index"
            )
        }

        val replyMarkup = InlineKeyboardMarkup(
            inline_keyboard = listOf(buttons)
        )

        sendMessage(
            chatId,
            "Как переводится слово «${question.correctAnswer.original}»?",
            replyMarkup
        )
    }

    fun answerCallbackQuery(callbackQueryId: String) {
        val requestBody = AnswerCallbackQueryRequest(
            callback_query_id = callbackQueryId
        )

        val jsonString = json.encodeToString(requestBody)

        sendPostRequest("answerCallbackQuery", jsonString)
    }

    private fun sendPostRequest(method: String, json: String) {
        val url = "${TELEGRAM_API_BASE}${botToken}/$method"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).execute().use { response ->
            println(response.body?.string())
        }
    }
}