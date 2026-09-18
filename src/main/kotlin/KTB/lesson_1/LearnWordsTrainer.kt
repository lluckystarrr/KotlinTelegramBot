package org.example.KTB.lesson_1

data class Word(
    val original: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
    val imagePath: String? = null,
    var imageFileId: String? = null
) {
    fun isLearned(answersCountToLearn: Int): Boolean {
        return correctAnswersCount >= answersCountToLearn
    }
}

data class Statistics(
    val totalCount: Int,
    val learnedCount: Int
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word
)

class LearnWordsTrainer(
    chatId: Long? = null,
    private val answersCountToLearn: Int = 3,
    private val numberOfQuestionWords: Int = 4,
    private val dictionary: IUserDictionary = createDefaultDictionary(
        chatId = chatId,
        answersCountToLearn = answersCountToLearn
    )
) {
    private var question: Question? = null

    fun getStatistics(): Statistics {
        return Statistics(
            totalCount = dictionary.getSize(),
            learnedCount = dictionary.getNumOfLearnedWords()
        )
    }

    fun resetStatistics() {
        dictionary.resetUserProgress()
    }

    fun getNextQuestion(): Question? {
        val notLearnedWords = dictionary.getUnlearnedWords()

        if (notLearnedWords.isEmpty()) {
            question = null
            return null
        }

        val correctAnswer = notLearnedWords.random()

        val variants = notLearnedWords
            .filter { it.translate != correctAnswer.translate }
            .shuffled()
            .take(numberOfQuestionWords - 1)
            .toMutableList()

        if (variants.size < numberOfQuestionWords - 1) {
            variants += dictionary
                .getLearnedWords()
                .filter { it.translate != correctAnswer.translate }
                .shuffled()
                .take(numberOfQuestionWords - 1 - variants.size)
        }

        variants.add(correctAnswer)
        variants.shuffle()

        question = Question(
            variants = variants,
            correctAnswer = correctAnswer
        )

        return question
    }

    fun getCurrentQuestion(): Question? = question

    fun checkAnswer(userAnswerIndex: Int?): Boolean {
        return question?.let { currentQuestion ->
            val correctAnswerIndex = currentQuestion.variants.indexOf(
                currentQuestion.correctAnswer
            )

            if (correctAnswerIndex == userAnswerIndex) {
                val newCount = currentQuestion.correctAnswer.correctAnswersCount + 1

                dictionary.setCorrectAnswersCount(
                    word = currentQuestion.correctAnswer.original,
                    correctAnswersCount = newCount
                )

                currentQuestion.correctAnswer.correctAnswersCount = newCount
                true
            } else {
                false
            }
        } ?: false
    }

    fun saveImageFileId(word: Word, fileId: String) {
        (dictionary as? IUserDictionaryExtended)?.saveImageFileId(word, fileId)
    }

    fun addWordsFromFile(fileName: String): Boolean {
        return (dictionary as? IUserDictionaryExtended)?.addWordsFromFile(fileName)
            ?: false
    }
}

private fun createDefaultDictionary(
    chatId: Long?,
    answersCountToLearn: Int
): IUserDictionary {
    return if (chatId == null) {
        FileUserDictionary(answersCountToLearn = answersCountToLearn)
    } else {
        DatabaseUserDictionary(
            chatId = chatId,
            answersCountToLearn = answersCountToLearn
        )
    }
}
