package com.example.cashia.ui.screens.projections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.SelectableBox
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.ProjectionPeriod


@Composable
fun ProjectionConfigCard(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    selectedPeriod: Int,
    onPeriodChange: (Int) -> Unit,
    balanceOverride: String,
    onBalanceOverrideChange: (String) -> Unit,
    autoBalance: Double,
    autoBalanceDate: String,
    haptic: HapticFeedback
) {
    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.92f else 0.78f
    val overrideValue = balanceOverride.toDoubleOrNull()
    val overrideIsNegative = overrideValue != null && overrideValue < 0
    val overrideIsUnreasonable =
        overrideValue != null && autoBalance > 0 && overrideValue > autoBalance * 10

    val period = ProjectionPeriod.entries[selectedPeriod]

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surface.copy(alpha = glassAlpha),
        border = BorderStroke(0.6.dp, colorScheme.onSurface.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.background(primaryGradient())
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                null,
                                tint = colorScheme.surface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        SectionLabel("projection basis")
                        Text(
                            if (isExpanded) "Tap to collapse"
                            else "${period.label} · ends ${period.endLabel()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    }
                }
                Icon(
                    if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    null, tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(300)) + fadeIn(),
                exit = shrinkVertically(tween(250)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.06f))
                    ConfigSection(
                        label = "FORECAST PERIOD",
                        sublabel = buildString {
                            append("Projecting through ${period.endLabel()}")
                            append(" (${period.daysRemaining()} days remaining).")
                            if (selectedPeriod >= 2)
                                append(" Longer periods have lower accuracy.")
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProjectionPeriod.entries.forEachIndexed { index, p ->
                                val shortLabel = when (p) {
                                    ProjectionPeriod.THIS_WEEK -> "Week"
                                    ProjectionPeriod.THIS_MONTH -> "Month"
                                    ProjectionPeriod.THIS_QUARTER -> "Quarter"
                                    ProjectionPeriod.THIS_YEAR -> "Year"
                                }
                                SelectableBox(
                                    haptic,
                                    selectedPeriod == index,
                                    { onPeriodChange(index) },
                                    Modifier.weight(1f)
                                ) {
                                    Text(
                                        shortLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selectedPeriod == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedPeriod == index) Color.White
                                        else colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    ConfigSection(
                        label = "STARTING BALANCE",
                        sublabel = buildString {
                            append("Detected from your M-Pesa SMS")
                            if (autoBalanceDate.isNotEmpty()) append(" on $autoBalanceDate")
                            append(". Override if your balance has changed.")
                        }
                    ) {
                        OutlinedTextField(
                            value = balanceOverride,
                            onValueChange = onBalanceOverrideChange,
                            placeholder = {
                                Text(
                                    autoBalance.toLong().toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                            },
                            prefix = {
                                Text(
                                    "KES ",
                                    color = if (overrideIsNegative) colorScheme.error
                                    else colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            trailingIcon = {
                                if (balanceOverride.isNotEmpty()) {
                                    IconButton(onClick = { onBalanceOverrideChange("") }) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            "Clear",
                                            modifier = Modifier.size(16.dp),
                                            tint = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            isError = overrideIsNegative || overrideIsUnreasonable,
                            supportingText = when {
                                overrideIsNegative -> {
                                    {
                                        Subtitles(
                                            "Balance can't be negative",
                                            color = colorScheme.error
                                        )
                                    }
                                }

                                overrideIsUnreasonable -> {
                                    {
                                        Subtitles(
                                            "Seems unusually high — double check",
                                            color = colorScheme.error,
                                        )
                                    }
                                }

                                overrideValue != null -> {
                                    {
                                        Subtitles(
                                            "Using custom balance",
                                            color = colorScheme.primary.copy(alpha = 0.7f),
                                        )
                                    }
                                }

                                else -> null
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (overrideIsNegative || overrideIsUnreasonable)
                                    colorScheme.error else colorScheme.primary,
                                unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.15f),
                                focusedContainerColor = colorScheme.primary.copy(alpha = 0.04f),
                                unfocusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                errorBorderColor = colorScheme.error,
                                errorContainerColor = colorScheme.error.copy(alpha = 0.04f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigSection(label: String, sublabel: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    brush = primaryGradient()
                ),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Subtitles(sublabel)
        }
        content()
    }
}