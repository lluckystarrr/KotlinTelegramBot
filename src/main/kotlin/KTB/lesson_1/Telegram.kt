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

    while (true) {
        Thread.sleep(2000)
        val updates: String = getUpdates(client, botToken, updateId)
        println(updates)

        val startUpdateId = updates.indexOf("update_id")
        if (startUpdateId == -1) continue

        var endUpdateId = startUpdateId + 11
        while (endUpdateId < updates.length && updates[endUpdateId].isDigit()) {
            endUpdateId++
        }
        if (endUpdateId == startUpdateId + 11) continue

        val updateIdString = updates.substring(startUpdateId + 11, endUpdateId)
        println(updateIdString)

        updateId = updateIdString.toInt() + 1

        val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
        val matchResult: MatchResult? = messageTextRegex.find(updates)
        val groups = matchResult?.groups
        val text = groups?.get(1)?.value
        println(text)
    }
}

fun getUpdates(client: HttpClient, botToken: String, updateId: Int): String {
    val urlGetUpdates = "$TELEGRAM_API_BASE$botToken/getUpdates?offset=$updateId"
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}
