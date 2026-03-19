package com.example.cashia.ui.reusedElements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.unit.dp
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.PesaActions

@Composable
fun SelectableBox(
    hapticFeedback: HapticFeedback,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundModifier = if (isSelected) {
        Modifier.background(primaryGradient())
    } else {
        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(backgroundModifier)
            .clickable {
                PesaActions.playClick(hapticFeedback)
                onClick()
            }
            .padding(10.dp, 10.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}