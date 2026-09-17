package org.example.KTB.lesson_1

class DynamicMessage {

    var messageId: Long? = null
        private set

    private val messageHistory =
        mutableListOf<String>()

    fun setMessageId(messageId: Long) {
        this.messageId = messageId
    }

    fun addMessage(message: String) {
        messageHistory.add(message)
    }

    fun getPreviousMessage(): String? {
        if (messageHistory.size < 2) {
            return null
        }

        messageHistory.removeLast()

        return messageHistory.last()
    }
}
