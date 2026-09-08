package com.hayhak.currencyconverter.ui.converter

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hayhak.currencyconverter.R
import kotlinx.coroutines.delay
import java.text.DecimalFormatSymbols

@Composable
fun NumberPalette(
    amount: String,
    onKey: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val decimalSep = remember {
        DecimalFormatSymbols.getInstance().decimalSeparator.toString()
    }
    val keys = listOf(
        listOf("1", "2", "3", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("7", "8", "9", "-"),
        listOf(decimalSep, "0", "%", "+"),
        listOf("DEL", "=", "C")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = amount,
            transitionSpec = {
                fadeIn(tween(120)) togetherWith fadeOut(tween(80))
            },
            label = "amountDigits"
        ) { value ->
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 4.dp)
            )
        }

        TextButton(onClick = { onKey("C") }) {
            Text(stringResource(R.string.converter_clear))
        }

        Spacer(Modifier.height(8.dp))

        keys.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEachIndexed { colIndex, key ->
                    NumberKey(
                        label = key,
                        index = rowIndex * 4 + colIndex,
                        modifier = Modifier.weight(if (key == "DEL" || key == "=" || key == "C") 1f else 1f),
                        onClick = { onKey(key) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        FilledTonalButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.converter_done), fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun NumberKey(
    label: String,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val appear = remember { Animatable(0f) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )

    LaunchedEffect(Unit) {
        delay(index * 35L)
        appear.animateTo(
            1f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val isAction = label == "DEL" || label == "=" || label == "C" ||
        label == "+" || label == "-" || label == "×" || label == "÷" || label == "%"
    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier
            .aspectRatio(1.55f)
            .graphicsLayer {
                val t = appear.value
                scaleX = 0.55f + 0.45f * t
                scaleY = 0.55f + 0.45f * t
                alpha = t
                translationY = (1f - t) * 36f
            }
            .scale(pressScale),
        shape = CircleShape,
        color = if (isAction) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = 2.dp,
        interactionSource = interaction
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isAction) {
                when (label) {
                    "DEL" -> Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(R.string.cd_backspace),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    else -> Text(
                        text = label,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp
                    )
                }
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 26.sp
                )
            }
        }
    }
}
