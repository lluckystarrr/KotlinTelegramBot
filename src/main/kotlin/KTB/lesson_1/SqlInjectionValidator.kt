package org.example.KTB.lesson_1

import java.util.logging.Logger

private val logger: Logger = Logger.getLogger("SqlInjectionValidator")

private val ALLOWED_PATTERN = Regex("^[a-zA-Zа-яА-ЯёЁ0-9\\s\\-,.]+$")

private const val MAX_LENGTH = 100

private val SUSPICIOUS_PATTERNS = listOf(
    "'",
    "\"",
    ";",
    "--",
    "/*",
    "*/",
    "union",
    "select",
    "drop",
    "delete",
    "insert",
    "update",
    "exec",
    "script"
)

fun logSuspiciousInput(fieldName: String, value: String) {
    val lowercaseValue = value.lowercase()

    val found = SUSPICIOUS_PATTERNS.filter { lowercaseValue.contains(it) }

    if (found.isNotEmpty()) {
        logger.warning(
            "Подозрительный ввод в поле '$fieldName': " +
                    "значение=\"$value\", " +
                    "найдены паттерны=$found"
        )
    }
}

fun validateWordInput(fieldName: String, value: String): String {
    logSuspiciousInput(fieldName, value)

    val trimmed = value.trim()

    if (trimmed.isEmpty()) {
        throw IllegalArgumentException("Поле '$fieldName' пустое")
    }

    if (trimmed.length > MAX_LENGTH) {
        throw IllegalArgumentException(
            "Поле '$fieldName' слишком длинное (${trimmed.length} > $MAX_LENGTH)"
        )
    }

    val lowercaseTrimmed = trimmed.lowercase()

    val foundSuspicious = SUSPICIOUS_PATTERNS.filter {
        lowercaseTrimmed.contains(it)
    }

    if (foundSuspicious.isNotEmpty()) {
        throw IllegalArgumentException(
            "Поле '$fieldName' содержит подозрительные паттерны: $foundSuspicious"
        )
    }

    if (!ALLOWED_PATTERN.matches(trimmed)) {
        throw IllegalArgumentException(
            "Поле '$fieldName' содержит недопустимые символы: \"$trimmed\""
        )
    }

    return trimmed
}