package org.example.KTB.lesson_1

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class TelegramBotService(
    private val botToken: String
) {
    private val client = OkHttpClient()
    fun getUpdates(updateId: Int): String {
        val url = "$TELEGRAM_API_BASE$botToken/getUpdates?offset=$updateId"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).execute().use { response ->
            return response.body?.string() ?: ""
        }
    }

    fun sendMessage(
        chatId: String,
        text: String,
        replyMarkup: String? = null
    ) {
        val json = if (replyMarkup == null) {
            """
            {
                "chat_id": $chatId,
                "text": "${escapeJson(text)}"
            }
            """.trimIndent()
        } else {
            """
            {
                "chat_id": $chatId,
                "text": "${escapeJson(text)}",
                "reply_markup": $replyMarkup
            }
            """.trimIndent()
        }

        sendPostRequest(
            method = "sendMessage",
            json = json
        )
    }

    fun sendMenu(chatId: String) {
        val replyMarkup = """
            {
                "inline_keyboard": [
                    [
                        {
                            "text": "Учить слова",
                            "callback_data": "learn_words"
                        }
                    ],
                    [
                        {
                            "text": "Статистика",
                            "callback_data": "statistics"
                        }
                    ]
                ]
            }
        """.trimIndent()

        sendMessage(
            chatId = chatId,
            text = "Главное меню:",
            replyMarkup = replyMarkup
        )
    }

    fun answerCallbackQuery(callbackQueryId: String) {
        val json = """
            {
                "callback_query_id": "${escapeJson(callbackQueryId)}"
            }
        """.trimIndent()

        sendPostRequest(
            method = "answerCallbackQuery",
            json = json
        )
    }

    private fun sendPostRequest(
        method: String,
        json: String
    ) {
        val url = "$TELEGRAM_API_BASE$botToken/$method"

        val mediaType = "application/json; charset=utf-8".toMediaType()

        val body = json.toRequestBody(mediaType)

        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).execute().use { response ->
            println(response.body?.string())
        }
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
