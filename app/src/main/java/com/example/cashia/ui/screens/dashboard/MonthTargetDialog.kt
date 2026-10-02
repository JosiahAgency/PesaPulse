package com.example.cashia.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cashia.ui.reusedElements.PesaDialog
import com.example.cashia.utility.PesaTimeConfiguration

/**
 * Lets the user set (or replace) the spending target that drives the dashboard's
 * "Pace this month" row. [previousMonthSpend] powers the one-tap "match last month" shortcut
 * and is hidden when last month has no recorded spend to copy. Styled with [PesaDialog] so it
 * shares the same rounded-surface chrome as the rest of the app's custom dialogs.
 */
@Composable
fun MonthTargetDialog(
    currentTarget: Double?,
    previousMonthSpend: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onUseLastMonthSpend: () -> Unit,
) {
    var input by remember {
        mutableStateOf(currentTarget?.takeIf { it > 0.0 }?.let { formatPlainAmount(it) } ?: "")
    }
    val parsed = input.trim().toDoubleOrNull()
    val isValid = parsed != null && parsed.isFinite() && parsed > 0.0

    PesaDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Monthly Spending Target",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            "Set a target to track your spending pace for this month.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { new -> input = new.filter { it.isDigit() || it == '.' } },
            label = { Text("Target (${PesaTimeConfiguration.CURRENCY})") },
            singleLine = true,
            isError = input.isNotBlank() && !isValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        if (previousMonthSpend > 0.0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Last month you spent " +
                        PesaTimeConfiguration.formatCurrency(previousMonthSpend),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onUseLastMonthSpend) { Text("Match last month") }
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            TextButton(
                onClick = { parsed?.let(onSave) },
                enabled = isValid
            ) { Text("Save") }
        }
    }
}

private fun formatPlainAmount(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
