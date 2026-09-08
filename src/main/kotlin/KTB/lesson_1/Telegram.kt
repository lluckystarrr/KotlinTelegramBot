package org.example.KTB.lesson_1

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_API_BASE = "https://api.telegram.org/bot"

fun main(args: Array<String>) {
    val botToken = args[0]
    var updateId = 0

    val client: HttpClient = HttpClient.newBuilder().build()

    val updateIdRegex = "\"update_id\":\\s*(\\d+)".toRegex()
    val messageTextRegex = "\"text\":\\s*\"(.*?)\"".toRegex()

    while (true) {
        Thread.sleep(2000)

        val updates = getUpdates(client, botToken, updateId)
        println(updates)

        var lastUpdateMatch: MatchResult? = null
        var startPos = 0
        while (true) {
            val match = updateIdRegex.find(updates, startPos) ?: break
            lastUpdateMatch = match
            startPos = match.range.last + 1
        }

        if (lastUpdateMatch == null) continue

        val updateIdString = lastUpdateMatch.groups[1]?.value ?: continue
        println(updateIdString)
        updateId = updateIdString.toInt() + 1

        var lastTextMatch: MatchResult? = null
        startPos = 0
        while (true) {
            val match = messageTextRegex.find(updates, startPos) ?: break
            lastTextMatch = match
            startPos = match.range.last + 1
        }

        val text = lastTextMatch?.groups?.get(1)?.value
        if (text != null) {
            println(text)
        }
    }
}

fun getUpdates(client: HttpClient, botToken: String, updateId: Int): String {
    val urlGetUpdates = "$TELEGRAM_API_BASE$botToken/getUpdates?offset=$updateId"

    val request = HttpRequest.newBuilder()
        .uri(URI.create(urlGetUpdates))
        .build()

    val response = client.send(
        request,
        HttpResponse.BodyHandlers.ofString()
    )

    return response.body()
}
