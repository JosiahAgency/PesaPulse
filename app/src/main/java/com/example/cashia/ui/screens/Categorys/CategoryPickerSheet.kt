package com.example.cashia.ui.screens.Categorys

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.ResolvedCategory


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    allCategories: List<ResolvedCategory>,
    currentCategoryId: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Box(
                Modifier
                    .size(40.dp, 4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        CircleShape
                    )
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Choose Category",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(allCategories, key = { it.id }) { cat ->
                    val isSelected = cat.id == currentCategoryId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPick(cat.id) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (isSelected)
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        else null
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(cat.emoji, fontSize = 22.sp)
                            Text(
                                cat.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}