package com.hayhak.currencyconverter.ui.metals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.core.os.ConfigurationCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.GOLD_KARATS
import com.hayhak.currencyconverter.domain.model.GoldCoin
import com.hayhak.currencyconverter.domain.model.goldCoinValue
import com.hayhak.currencyconverter.domain.model.goldGramValue
import java.text.NumberFormat
import java.util.Locale

/** Gold per gram by karat and the common gold coins, derived from the gold price per troy ounce. */
@Composable
fun GoldJewelrySection(ouncePrice: Double, quote: String, modifier: Modifier = Modifier) {
    if (!ouncePrice.isFinite() || ouncePrice <= 0) return
    val isTurkish = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]?.language == "tr"
    val money = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.gold_jewelry_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            GOLD_KARATS.forEach { karat ->
                ValueRow(
                    label = "${karat}K · ${stringResource(R.string.metal_gram)}",
                    value = "${money.format(goldGramValue(ouncePrice, karat))} $quote"
                )
            }
            // Çeyrek, yarım, tam and Ata are Turkish market coins, so only Turkish users see them.
            if (isTurkish) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    stringResource(R.string.gold_coins_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                GoldCoin.entries.forEach { coin ->
                    ValueRow(
                        label = stringResource(coinLabel(coin)),
                        value = "${money.format(goldCoinValue(ouncePrice, coin))} $quote"
                    )
                }
                Text(
                    stringResource(R.string.gold_coins_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun coinLabel(coin: GoldCoin): Int = when (coin) {
    GoldCoin.QUARTER -> R.string.gold_coin_quarter
    GoldCoin.HALF -> R.string.gold_coin_half
    GoldCoin.FULL -> R.string.gold_coin_full
    GoldCoin.ATA -> R.string.gold_coin_ata
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
