package com.example.cashia.ui.screens.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.SelectableBox
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.TimeScope
import com.example.cashia.utility.showDatePicker
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Composable
fun PeriodSelectorCard(
    haptic: HapticFeedback,
    selectedScope: TimeScope,
    startDate: LocalDate,
    endDate: LocalDate,
    onScopeChange: (TimeScope) -> Unit,
    onStartDateSelected: (LocalDate) -> Unit,
    onEndDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.92f else 0.78f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surface.copy(alpha = glassAlpha),
        border = BorderStroke(0.6.dp, colorScheme.onSurface.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionLabel("PERIOD")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    TimeScope.WEEK to "Week",
                    TimeScope.MONTH to "Month",
                    TimeScope.YEAR to "Year",
                ).forEach { (scope, label) ->
                    val isSelected = selectedScope == scope
                    SelectableBox(
                        haptic,
                        isSelected,
                        { onScopeChange(scope) },
                        Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White
                            else colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedScope == TimeScope.CUSTOM)
                        colorScheme.primary.copy(alpha = 0.08f)
                    else colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(
                        0.5.dp,
                        if (selectedScope == TimeScope.CUSTOM) colorScheme.primary.copy(alpha = 0.3f)
                        else colorScheme.onSurface.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            PesaActions.playClick(haptic)
                            showDatePicker(context, startDate, endDate, true, onStartDateSelected)
                        }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Subtitles("FROM")
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = startDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                brush = primaryGradient()
                            ),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedScope == TimeScope.CUSTOM)
                        colorScheme.primary.copy(alpha = 0.08f)
                    else colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(
                        0.5.dp,
                        if (selectedScope == TimeScope.CUSTOM) colorScheme.primary.copy(alpha = 0.3f)
                        else colorScheme.onSurface.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            PesaActions.playClick(haptic)
                            showDatePicker(context, startDate, endDate, false, onEndDateSelected)
                        }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Subtitles("TO")
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                brush = primaryGradient()
                            ),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            if (selectedScope == TimeScope.CUSTOM) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Custom range active — tap a chip above to reset",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}