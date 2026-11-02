package com.hayhak.currencyconverter.util

import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES

data class ParsedConversion(
    val amount: Double?,
    val from: String?,
    val to: String?
)

private val knownCodes = SUPPORTED_CURRENCIES.map { it.code }.toSet()
private val codePattern = Regex("[A-Za-z]{3}")
private val numberPattern = Regex("\\d[\\d.,]*")

fun parseProcessText(raw: String): ParsedConversion {
    val codes = codePattern.findAll(raw)
        .map { it.value.uppercase() }
        .filter { it in knownCodes }
        .distinct()
        .toList()
    val amount = numberPattern.find(raw)?.value?.let(::normalizeAmount)
    return ParsedConversion(
        amount = amount,
        from = codes.getOrNull(0),
        to = codes.getOrNull(1)
    )
}

/** Accepts 1200, 1,200.50 and 1.200,50. A group of exactly three digits is treated as thousands. */
fun normalizeAmount(raw: String): Double? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    val lastDot = text.lastIndexOf('.')
    val lastComma = text.lastIndexOf(',')
    val decimal: Char? = when {
        lastDot >= 0 && lastComma >= 0 -> if (lastDot > lastComma) '.' else ','
        lastComma >= 0 && text.length - lastComma - 1 != 3 -> ','
        lastDot >= 0 && text.length - lastDot - 1 != 3 -> '.'
        else -> null
    }
    val cleaned = when (decimal) {
        null -> text.filter { it.isDigit() }
        else -> {
            val grouping = if (decimal == '.') ',' else '.'
            text.filter { it != grouping }.replace(decimal, '.')
        }
    }
    return cleaned.toDoubleOrNull()?.takeIf { it.isFinite() }
}
