package org.example.KTB.lesson_1

const val TELEGRAM_API_BASE = "https://api.telegram.org/bot"

fun main(args: Array<String>) {
    val botToken = args[0]
    var updateId = 0

    val telegramBotService = TelegramBotService(botToken)

    val updateIdRegex = "\"update_id\":\\s*(\\d+)".toRegex()
    val messageTextRegex = "\"text\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"".toRegex()
    val chatIdRegex = "\"chat\":\\s*\\{[^}]*\"id\":\\s*(-?\\d+)".toRegex()

    while (true) {
        Thread.sleep(2000)

        val updates = telegramBotService.getUpdates(updateId)
        println(updates)

        var lastUpdateMatch: MatchResult? = null
        var startPos = 0

        while (true) {
            val match = updateIdRegex.find(updates, startPos) ?: break
            lastUpdateMatch = match
            startPos = match.range.last + 1
        }

        if (lastUpdateMatch == null) continue
        val updateIdString =
            lastUpdateMatch.groups[1]?.value ?: continue

        println(updateIdString)
        updateId = updateIdString.toInt() + 1
        val textMatch = messageTextRegex.find(updates, lastUpdateMatch.range.first)
        val text = textMatch?.groups?.get(1)?.value
        val chatIdMatch = chatIdRegex.find(updates, lastUpdateMatch.range.first)
        val chatId = chatIdMatch?.groups?.get(1)?.value
        if (text != null) {
            println(text)
        }

        if (chatId != null) {
            println(chatId)
        }

        if (text != null && chatId != null) {
            telegramBotService.sendMessage(chatId, text)
        }
    }
}
