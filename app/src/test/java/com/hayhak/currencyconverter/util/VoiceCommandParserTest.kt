package com.hayhak.currencyconverter.util

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandParserTest {

    @Test
    fun `parses turkish spoken commands correctly`() {
        val res1 = VoiceCommandParser.parse("100 Dolar kaç TL")
        assertEquals("100", res1.amount)
        assertEquals("USD", res1.fromCode)
        assertEquals("TRY", res1.toCode)

        val res2 = VoiceCommandParser.parse("50 Euro kaç Sterlin")
        assertEquals("50", res2.amount)
        assertEquals("EUR", res2.fromCode)
        assertEquals("GBP", res2.toCode)
    }

    @Test
    fun `parses english spoken commands correctly`() {
        val res = VoiceCommandParser.parse("250 USD to EUR")
        assertEquals("250", res.amount)
        assertEquals("USD", res.fromCode)
        assertEquals("EUR", res.toCode)
    }
}
