package com.example.cashia.ui.screens.Analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cashia.Utility.PesaActions
import java.time.LocalDate


@Composable
fun GlassyPeriodSelector(
    hapticFeedback: HapticFeedback,
    startDate: LocalDate,
    endDate: LocalDate,
    onStartDateSelected: (LocalDate) -> Unit,
    onEndDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassySurface {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateColumn(
                    label = "From",
                    date = startDate,
                    onClick = {
                        PesaActions.playClick(hapticFeedback)
                        showDatePicker(
                            context,
                            startDate,
                            endDate,
                            true,
                            onStartDateSelected
                        )
                    }
                )

                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                DateColumn(
                    label = "To",
                    date = endDate,
                    onClick = {
                        PesaActions.playClick(hapticFeedback)
                        showDatePicker(
                            context,
                            startDate,
                            endDate,
                            false,
                            onEndDateSelected
                        )
                    }
                )
            }
        }
    }
}