package com.hayhak.currencyconverter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hayhak.currencyconverter.R

/**
 * Above/below selector for rate alerts. A flow layout keeps each label on one line and moves the
 * second chip below the first when the translation or the font size leaves no room beside it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmDirectionChips(
    isAbove: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    aboveRes: Int = R.string.alarm_when_above,
    belowRes: Int = R.string.alarm_when_below
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = isAbove,
            onClick = { onChange(true) },
            label = { Text(stringResource(aboveRes)) }
        )
        FilterChip(
            selected = !isAbove,
            onClick = { onChange(false) },
            label = { Text(stringResource(belowRes)) }
        )
    }
}
