package org.example.KTB.lesson_1

data class SavedMessage(
    val text: String,
    val replyMarkup: InlineKeyboardMarkup? = null
)

class DynamicMessage {

    var messageId: Long? = null

    private val messageHistory =
        mutableListOf<SavedMessage>()


    fun addMessage(
        text: String,
        replyMarkup: InlineKeyboardMarkup? = null
    ) {

        messageHistory.add(
            SavedMessage(
                text = text,
                replyMarkup = replyMarkup
            )
        )
    }


    fun getPreviousMessage(): SavedMessage? {

        if (messageHistory.isEmpty()) {
            return null
        }

        return messageHistory.removeLast()
    }

    fun setMessageId(
        id: Long
    ) {

        messageId = id
    }
}