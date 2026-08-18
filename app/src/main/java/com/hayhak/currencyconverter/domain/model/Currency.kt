package com.hayhak.currencyconverter.domain.model

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String
)

private fun flagOf(countryCode: String): String =
    countryCode.uppercase()
        .map { char -> Character.codePointAt(char.toString(), 0) - 0x41 + 0x1F1E6 }
        .map { Character.toChars(it) }
        .joinToString("") { String(it) }

fun getFlagEmoji(currencyCode: String): String {
    if (currencyCode.length != 3) return "🌐"
    return CATALOG[currencyCode.uppercase()]?.flag ?: run {
        val cc = currencyCode.take(2)
        flagOf(cc)
    }
}

fun currencyByCode(code: String): CurrencyInfo? = CATALOG[code.uppercase()]

/**
 * Frankfurter v2 active currencies (84 central banks, ~165 codes).
 * @see https://api.frankfurter.dev/v2/currencies
 */
val SUPPORTED_CURRENCIES = listOf(
    CurrencyInfo("AED", "United Arab Emirates Dirham", "د.إ", flagOf("AE")),
    CurrencyInfo("AFN", "Afghan Afghani", "؋", flagOf("AF")),
    CurrencyInfo("ALL", "Albanian Lek", "L", flagOf("AL")),
    CurrencyInfo("AMD", "Armenian Dram", "֏", flagOf("AM")),
    CurrencyInfo("ANG", "Netherlands Antillean Gulden", "ƒ", flagOf("CW")),
    CurrencyInfo("AOA", "Angolan Kwanza", "Kz", flagOf("AO")),
    CurrencyInfo("ARS", "Argentine Peso", "\$", flagOf("AR")),
    CurrencyInfo("AUD", "Australian Dollar", "\$", flagOf("AU")),
    CurrencyInfo("AWG", "Aruban Florin", "ƒ", flagOf("AW")),
    CurrencyInfo("AZN", "Azerbaijani Manat", "₼", flagOf("AZ")),
    CurrencyInfo("BAM", "Bosnia and Herzegovina Convertible Mark", "КМ", flagOf("BA")),
    CurrencyInfo("BBD", "Barbadian Dollar", "\$", flagOf("BB")),
    CurrencyInfo("BDT", "Bangladeshi Taka", "৳", flagOf("BD")),
    CurrencyInfo("BHD", "Bahraini Dinar", "د.ب", flagOf("BH")),
    CurrencyInfo("BIF", "Burundian Franc", "Fr", flagOf("BI")),
    CurrencyInfo("BMD", "Bermudian Dollar", "\$", flagOf("BM")),
    CurrencyInfo("BND", "Brunei Dollar", "\$", flagOf("BN")),
    CurrencyInfo("BOB", "Bolivian Boliviano", "Bs.", flagOf("BO")),
    CurrencyInfo("BRL", "Brazilian Real", "R\$", flagOf("BR")),
    CurrencyInfo("BSD", "Bahamian Dollar", "\$", flagOf("BS")),
    CurrencyInfo("BTN", "Bhutanese Ngultrum", "Nu.", flagOf("BT")),
    CurrencyInfo("BWP", "Botswana Pula", "P", flagOf("BW")),
    CurrencyInfo("BYN", "Belarusian Ruble", "Br", flagOf("BY")),
    CurrencyInfo("BZD", "Belize Dollar", "\$", flagOf("BZ")),
    CurrencyInfo("CAD", "Canadian Dollar", "\$", flagOf("CA")),
    CurrencyInfo("CDF", "Congolese Franc", "Fr", flagOf("CD")),
    CurrencyInfo("CHF", "Swiss Franc", "CHF", flagOf("CH")),
    CurrencyInfo("CLP", "Chilean Peso", "\$", flagOf("CL")),
    CurrencyInfo("CNH", "Chinese Renminbi Yuan Offshore", "¥", flagOf("CN")),
    CurrencyInfo("CNY", "Chinese Renminbi Yuan", "¥", flagOf("CN")),
    CurrencyInfo("COP", "Colombian Peso", "\$", flagOf("CO")),
    CurrencyInfo("CRC", "Costa Rican Colón", "₡", flagOf("CR")),
    CurrencyInfo("CUP", "Cuban Peso", "\$", flagOf("CU")),
    CurrencyInfo("CVE", "Cape Verdean Escudo", "\$", flagOf("CV")),
    CurrencyInfo("CZK", "Czech Koruna", "Kč", flagOf("CZ")),
    CurrencyInfo("DJF", "Djiboutian Franc", "Fdj", flagOf("DJ")),
    CurrencyInfo("DKK", "Danish Krone", "kr.", flagOf("DK")),
    CurrencyInfo("DOP", "Dominican Peso", "\$", flagOf("DO")),
    CurrencyInfo("DZD", "Algerian Dinar", "د.ج", flagOf("DZ")),
    CurrencyInfo("EGP", "Egyptian Pound", "ج.م", flagOf("EG")),
    CurrencyInfo("ERN", "Eritrean Nakfa", "Nfk", flagOf("ER")),
    CurrencyInfo("ETB", "Ethiopian Birr", "Br", flagOf("ET")),
    CurrencyInfo("EUR", "Euro", "€", flagOf("EU")),
    CurrencyInfo("FJD", "Fijian Dollar", "\$", flagOf("FJ")),
    CurrencyInfo("FKP", "Falkland Pound", "£", flagOf("FK")),
    CurrencyInfo("GBP", "British Pound", "£", flagOf("GB")),
    CurrencyInfo("GEL", "Georgian Lari", "₾", flagOf("GE")),
    CurrencyInfo("GGP", "Guernsey Pound", "£", flagOf("GG")),
    CurrencyInfo("GHS", "Ghanaian Cedi", "₵", flagOf("GH")),
    CurrencyInfo("GIP", "Gibraltar Pound", "£", flagOf("GI")),
    CurrencyInfo("GMD", "Gambian Dalasi", "D", flagOf("GM")),
    CurrencyInfo("GNF", "Guinean Franc", "Fr", flagOf("GN")),
    CurrencyInfo("GTQ", "Guatemalan Quetzal", "Q", flagOf("GT")),
    CurrencyInfo("GYD", "Guyanese Dollar", "\$", flagOf("GY")),
    CurrencyInfo("HKD", "Hong Kong Dollar", "\$", flagOf("HK")),
    CurrencyInfo("HNL", "Honduran Lempira", "L", flagOf("HN")),
    CurrencyInfo("HTG", "Haitian Gourde", "G", flagOf("HT")),
    CurrencyInfo("HUF", "Hungarian Forint", "Ft", flagOf("HU")),
    CurrencyInfo("IDR", "Indonesian Rupiah", "Rp", flagOf("ID")),
    CurrencyInfo("ILS", "Israeli New Shekel", "₪", flagOf("IL")),
    CurrencyInfo("IMP", "Isle of Man Pound", "£", flagOf("IM")),
    CurrencyInfo("INR", "Indian Rupee", "₹", flagOf("IN")),
    CurrencyInfo("IQD", "Iraqi Dinar", "ع.د", flagOf("IQ")),
    CurrencyInfo("IRR", "Iranian Rial", "﷼", flagOf("IR")),
    CurrencyInfo("ISK", "Icelandic Króna", "kr.", flagOf("IS")),
    CurrencyInfo("JEP", "Jersey Pound", "£", flagOf("JE")),
    CurrencyInfo("JMD", "Jamaican Dollar", "\$", flagOf("JM")),
    CurrencyInfo("JOD", "Jordanian Dinar", "د.ا", flagOf("JO")),
    CurrencyInfo("JPY", "Japanese Yen", "¥", flagOf("JP")),
    CurrencyInfo("KES", "Kenyan Shilling", "KSh", flagOf("KE")),
    CurrencyInfo("KGS", "Kyrgyzstani Som", "som", flagOf("KG")),
    CurrencyInfo("KHR", "Cambodian Riel", "៛", flagOf("KH")),
    CurrencyInfo("KMF", "Comorian Franc", "Fr", flagOf("KM")),
    CurrencyInfo("KPW", "North Korean Won", "₩", flagOf("KP")),
    CurrencyInfo("KRW", "South Korean Won", "₩", flagOf("KR")),
    CurrencyInfo("KWD", "Kuwaiti Dinar", "د.ك", flagOf("KW")),
    CurrencyInfo("KYD", "Cayman Islands Dollar", "\$", flagOf("KY")),
    CurrencyInfo("KZT", "Kazakhstani Tenge", "₸", flagOf("KZ")),
    CurrencyInfo("LAK", "Lao Kip", "₭", flagOf("LA")),
    CurrencyInfo("LBP", "Lebanese Pound", "ل.ل", flagOf("LB")),
    CurrencyInfo("LKR", "Sri Lankan Rupee", "₨", flagOf("LK")),
    CurrencyInfo("LRD", "Liberian Dollar", "\$", flagOf("LR")),
    CurrencyInfo("LSL", "Lesotho Loti", "L", flagOf("LS")),
    CurrencyInfo("LYD", "Libyan Dinar", "ل.د", flagOf("LY")),
    CurrencyInfo("MAD", "Moroccan Dirham", "د.م.", flagOf("MA")),
    CurrencyInfo("MDL", "Moldovan Leu", "L", flagOf("MD")),
    CurrencyInfo("MGA", "Malagasy Ariary", "Ar", flagOf("MG")),
    CurrencyInfo("MKD", "Macedonian Denar", "ден", flagOf("MK")),
    CurrencyInfo("MMK", "Myanmar Kyat", "K", flagOf("MM")),
    CurrencyInfo("MNT", "Mongolian Tögrög", "₮", flagOf("MN")),
    CurrencyInfo("MOP", "Macanese Pataca", "P", flagOf("MO")),
    CurrencyInfo("MRO", "Mauritanian Ouguiya", "UM", flagOf("MR")),
    CurrencyInfo("MRU", "Mauritanian Ouguiya", "UM", flagOf("MR")),
    CurrencyInfo("MUR", "Mauritian Rupee", "₨", flagOf("MU")),
    CurrencyInfo("MVR", "Maldivian Rufiyaa", "MVR", flagOf("MV")),
    CurrencyInfo("MWK", "Malawian Kwacha", "MK", flagOf("MW")),
    CurrencyInfo("MXN", "Mexican Peso", "\$", flagOf("MX")),
    CurrencyInfo("MYR", "Malaysian Ringgit", "RM", flagOf("MY")),
    CurrencyInfo("MZN", "Mozambican Metical", "MTn", flagOf("MZ")),
    CurrencyInfo("NAD", "Namibian Dollar", "\$", flagOf("NA")),
    CurrencyInfo("NGN", "Nigerian Naira", "₦", flagOf("NG")),
    CurrencyInfo("NIO", "Nicaraguan Córdoba", "C\$", flagOf("NI")),
    CurrencyInfo("NOK", "Norwegian Krone", "kr", flagOf("NO")),
    CurrencyInfo("NPR", "Nepalese Rupee", "Rs.", flagOf("NP")),
    CurrencyInfo("NZD", "New Zealand Dollar", "\$", flagOf("NZ")),
    CurrencyInfo("OMR", "Omani Rial", "ر.ع.", flagOf("OM")),
    CurrencyInfo("PAB", "Panamanian Balboa", "B/.", flagOf("PA")),
    CurrencyInfo("PEN", "Peruvian Sol", "S/", flagOf("PE")),
    CurrencyInfo("PGK", "Papua New Guinean Kina", "K", flagOf("PG")),
    CurrencyInfo("PHP", "Philippine Peso", "₱", flagOf("PH")),
    CurrencyInfo("PKR", "Pakistani Rupee", "₨", flagOf("PK")),
    CurrencyInfo("PLN", "Polish Złoty", "zł", flagOf("PL")),
    CurrencyInfo("PYG", "Paraguayan Guaraní", "₲", flagOf("PY")),
    CurrencyInfo("QAR", "Qatari Riyal", "ر.ق", flagOf("QA")),
    CurrencyInfo("RON", "Romanian Leu", "Lei", flagOf("RO")),
    CurrencyInfo("RSD", "Serbian Dinar", "RSD", flagOf("RS")),
    CurrencyInfo("RUB", "Russian Ruble", "₽", flagOf("RU")),
    CurrencyInfo("RWF", "Rwandan Franc", "FRw", flagOf("RW")),
    CurrencyInfo("SAR", "Saudi Riyal", "ر.س", flagOf("SA")),
    CurrencyInfo("SBD", "Solomon Islands Dollar", "\$", flagOf("SB")),
    CurrencyInfo("SCR", "Seychellois Rupee", "₨", flagOf("SC")),
    CurrencyInfo("SDG", "Sudanese Pound", "£", flagOf("SD")),
    CurrencyInfo("SEK", "Swedish Krona", "kr", flagOf("SE")),
    CurrencyInfo("SGD", "Singapore Dollar", "\$", flagOf("SG")),
    CurrencyInfo("SHP", "Saint Helenian Pound", "£", flagOf("SH")),
    CurrencyInfo("SLE", "New Leone", "Le", flagOf("SL")),
    CurrencyInfo("SOS", "Somali Shilling", "Sh", flagOf("SO")),
    CurrencyInfo("SRD", "Surinamese Dollar", "\$", flagOf("SR")),
    CurrencyInfo("SSP", "South Sudanese Pound", "£", flagOf("SS")),
    CurrencyInfo("STN", "São Tomé and Príncipe Second Dobra", "Db", flagOf("ST")),
    CurrencyInfo("SVC", "Salvadoran Colón", "₡", flagOf("SV")),
    CurrencyInfo("SYP", "Syrian Pound", "£S", flagOf("SY")),
    CurrencyInfo("SZL", "Swazi Lilangeni", "E", flagOf("SZ")),
    CurrencyInfo("THB", "Thai Baht", "฿", flagOf("TH")),
    CurrencyInfo("TJS", "Tajikistani Somoni", "ЅМ", flagOf("TJ")),
    CurrencyInfo("TMT", "Turkmenistani Manat", "m", flagOf("TM")),
    CurrencyInfo("TND", "Tunisian Dinar", "د.ت", flagOf("TN")),
    CurrencyInfo("TOP", "Tongan Paʻanga", "T\$", flagOf("TO")),
    CurrencyInfo("TRY", "Turkish Lira", "₺", flagOf("TR")),
    CurrencyInfo("TTD", "Trinidad and Tobago Dollar", "\$", flagOf("TT")),
    CurrencyInfo("TWD", "New Taiwan Dollar", "\$", flagOf("TW")),
    CurrencyInfo("TZS", "Tanzanian Shilling", "Sh", flagOf("TZ")),
    CurrencyInfo("UAH", "Ukrainian Hryvnia", "₴", flagOf("UA")),
    CurrencyInfo("UGX", "Ugandan Shilling", "USh", flagOf("UG")),
    CurrencyInfo("USD", "United States Dollar", "\$", flagOf("US")),
    CurrencyInfo("UYU", "Uruguayan Peso", "\$U", flagOf("UY")),
    CurrencyInfo("UZS", "Uzbekistan Som", "so'm", flagOf("UZ")),
    CurrencyInfo("VES", "Venezuelan Bolívar Soberano", "Bs", flagOf("VE")),
    CurrencyInfo("VND", "Vietnamese Đồng", "₫", flagOf("VN")),
    CurrencyInfo("VUV", "Vanuatu Vatu", "Vt", flagOf("VU")),
    CurrencyInfo("WST", "Samoan Tala", "T", flagOf("WS")),
    CurrencyInfo("XAF", "Central African CFA Franc", "CFA", flagOf("CM")),
    CurrencyInfo("XAG", "Silver (Troy Ounce)", "oz t", "🌐"),
    CurrencyInfo("XAU", "Gold (Troy Ounce)", "oz t", "🌐"),
    CurrencyInfo("XCD", "East Caribbean Dollar", "\$", flagOf("AG")),
    CurrencyInfo("XCG", "Caribbean Guilder", "Cg", flagOf("CW")),
    CurrencyInfo("XDR", "Special Drawing Rights", "SDR", "🌐"),
    CurrencyInfo("XOF", "West African CFA Franc", "Fr", flagOf("SN")),
    CurrencyInfo("XPD", "Palladium", "oz t", "🌐"),
    CurrencyInfo("XPF", "CFP Franc", "Fr", flagOf("PF")),
    CurrencyInfo("XPT", "Platinum", "oz t", "🌐"),
    CurrencyInfo("YER", "Yemeni Rial", "﷼", flagOf("YE")),
    CurrencyInfo("ZAR", "South African Rand", "R", flagOf("ZA")),
    CurrencyInfo("ZMW", "Zambian Kwacha", "K", flagOf("ZM")),
    CurrencyInfo("ZWG", "Zimbabwe Gold", "ZiG", flagOf("ZW"))
)

private val CATALOG = SUPPORTED_CURRENCIES.associateBy { it.code }

val DASHBOARD_CURRENCIES = listOf(
    "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY", "BRL", "INR", "TRY", "ZAR"
)

/** Pinned at the top of converter currency pickers. */
val PRIORITY_CURRENCIES = listOf("USD", "GBP", "CHF", "EUR")

fun prioritizedCurrencies(query: String = ""): List<CurrencyInfo> {
    val q = query.trim()
    val matches = if (q.isBlank()) {
        SUPPORTED_CURRENCIES
    } else {
        SUPPORTED_CURRENCIES.filter {
            it.code.contains(q, ignoreCase = true) ||
                it.name.contains(q, ignoreCase = true)
        }
    }
    val priority = PRIORITY_CURRENCIES.mapNotNull { code ->
        matches.find { it.code == code }
    }
    val rest = matches.filter { it.code !in PRIORITY_CURRENCIES }
    return priority + rest
}
