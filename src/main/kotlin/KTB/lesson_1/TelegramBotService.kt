package org.example.KTB.lesson_1

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

const val CALLBACK_LEARN_WORDS = "learn_words"
const val CALLBACK_STATISTICS = "statistics"
const val ANSWER_PREFIX = "answer_"

class TelegramBotService(private val botToken: String) {

    private val client = OkHttpClient()

    fun getUpdates(updateId: Int): String {
        val url = "${TELEGRAM_API_BASE}${botToken}/getUpdates?offset=$updateId"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            return response.body?.string() ?: ""
        }
    }

    fun sendMessage(chatId: String, text: String, replyMarkup: String? = null) {
        val escapedText = escapeJson(text)

        val json = if (replyMarkup == null) {
            """
            {
                "chat_id": "$chatId",
                "text": "$escapedText"
            }
            """.trimIndent()
        } else {
            """
            {
                "chat_id": "$chatId",
                "text": "$escapedText",
                "reply_markup": $replyMarkup
            }
            """.trimIndent()
        }

        sendPostRequest("sendMessage", json)
    }

    fun sendMenu(chatId: String) {
        val replyMarkup = """
            {
                "inline_keyboard": [
                    [{"text": "Учить слова", "callback_data": "$CALLBACK_LEARN_WORDS"}],
                    [{"text": "Статистика", "callback_data": "$CALLBACK_STATISTICS"}]
                ]
            }
        """.trimIndent()

        sendMessage(chatId, "Главное меню:", replyMarkup)
    }

    fun sendQuestion(chatId: String, question: Question) {
        val buttons = question.variants.mapIndexed { index, word -> """{"text": "${escapeJson(word.translate)}", "callback_data": "$ANSWER_PREFIX$index"}""" }.joinToString(",")
        val replyMarkup = """{"inline_keyboard": [[$buttons]]}"""

        sendMessage(chatId, "Как переводится слово «${question.correctAnswer.original}»?", replyMarkup)
    }

    fun answerCallbackQuery(callbackQueryId: String) {
        val json = """
            {
                "callback_query_id": "$callbackQueryId"
            }
        """.trimIndent()

        sendPostRequest("answerCallbackQuery", json)
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

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
    }
}
