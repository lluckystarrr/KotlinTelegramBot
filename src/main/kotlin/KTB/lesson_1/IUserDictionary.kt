package org.example.KTB.lesson_1

interface IUserDictionary {
    fun getNumOfLearnedWords(): Int
    fun getSize(): Int
    fun getLearnedWords(): List<Word>
    fun getUnlearnedWords(): List<Word>
    fun setCorrectAnswersCount(word: String, correctAnswersCount: Int)
    fun resetUserProgress()
}

interface IUserDictionaryExtended : IUserDictionary {
    fun addWordsFromFile(fileName: String): Boolean
    fun saveImageFileId(word: Word, fileId: String)
}
