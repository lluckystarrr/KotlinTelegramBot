package org.example.KTB.lesson_1

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_API_BASE = "https://api.telegram.org/bot"

fun main(args: Array<String>) {
    val botToken = args[0]

    val urlGetMe = "$TELEGRAM_API_BASE$botToken/getMe"
    val urlGetUpdates = "$TELEGRAM_API_BASE$botToken/getUpdates"

    val client: HttpClient = HttpClient.newBuilder().build()

    val requestGetUpdates: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()

    val requestGetMe: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetMe)).build()

    val responseUpdates = client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())
    val responseMe = client.send(requestGetMe, HttpResponse.BodyHandlers.ofString())

    println(responseUpdates.body())
    println(responseMe.body())
}
