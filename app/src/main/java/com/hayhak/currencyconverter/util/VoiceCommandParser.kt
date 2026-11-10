package com.hayhak.currencyconverter.util

import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import java.util.Locale

data class VoiceConversionResult(
    val amount: String?,
    val fromCode: String?,
    val toCode: String?
)

object VoiceCommandParser {

    private val currencyMap: Map<String, String> = buildMap {
        put("dolar", "USD")
        put("dollar", "USD")
        put("dollars", "USD")
        put("usd", "USD")
        put("euro", "EUR")
        put("yuro", "EUR")
        put("eur", "EUR")
        put("tl", "TRY")
        put("lira", "TRY")
        put("türk lirası", "TRY")
        put("turk lirasi", "TRY")
        put("try", "TRY")
        put("sterlin", "GBP")
        put("pound", "GBP")
        put("gbp", "GBP")
        put("frank", "CHF")
        put("franc", "CHF")
        put("chf", "CHF")
        put("yen", "JPY")
        put("jpy", "JPY")
        put("yuan", "CNY")
        put("cny", "CNY")
        put("ruble", "RUB")
        put("rub", "RUB")
        put("kron", "SEK")
        put("sek", "SEK")
        put("nok", "NOK")
        put("dkk", "DKK")
        put("dinar", "KWD")
        put("dirhem", "AED")
        put("dirham", "AED")
        put("riyal", "SAR")
        put("rupi", "INR")
        put("inr", "INR")
        put("won", "KRW")
        put("krw", "KRW")
        put("kanada doları", "CAD")
        put("cad", "CAD")
        put("avustralya doları", "AUD")
        put("aud", "AUD")

        SUPPORTED_CURRENCIES.forEach {
            put(it.code.lowercase(Locale.US), it.code)
        }
    }

    fun parse(spokenText: String): VoiceConversionResult {
        val text = spokenText.lowercase(Locale.getDefault()).trim()
        if (text.isEmpty()) return VoiceConversionResult(null, null, null)

        // Extract amount (number)
        val numberMatch = Regex("\\d+[.,]?\\d*").find(text)
        val amount = numberMatch?.value?.replace(",", ".")

        val matchesWithPos = mutableListOf<Pair<Int, String>>()
        val sortedKeys = currencyMap.keys.sortedByDescending { it.length }
        var searchArea = text

        for (key in sortedKeys) {
            val idx = searchArea.indexOf(key)
            if (idx >= 0) {
                val code = currencyMap[key]!!
                matchesWithPos.add(idx to code)
                searchArea = searchArea.replaceRange(idx, idx + key.length, " ".repeat(key.length))
            }
        }

        val matchedCodes = matchesWithPos
            .sortedBy { it.first }
            .map { it.second }
            .distinct()

        val fromCode = matchedCodes.getOrNull(0)
        val toCode = matchedCodes.getOrNull(1)

        return VoiceConversionResult(
            amount = amount,
            fromCode = fromCode,
            toCode = toCode
        )
    }
}
