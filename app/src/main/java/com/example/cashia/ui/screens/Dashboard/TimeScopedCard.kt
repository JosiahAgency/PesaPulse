package com.example.cashia.ui.screens.Dashboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.Utility.PesaActions
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.Utility.TimeScope
import com.example.cashia.models.SummaryData
import com.example.cashia.ui.theme.primaryGradient

@Composable
fun TimeScopedSummaryCard(
    scope: TimeScope,
    onScopeChange: (TimeScope) -> Unit,
    summaryData: SummaryData,
    currentBalance: Double,
    isBalanceVisible: Boolean = false,
    onToggleVisibility: () -> Unit,
    hapticFeedback: HapticFeedback
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.92f else 0.78f
    val glassColor = colorScheme.surface.copy(alpha = glassAlpha)
    val glassBorder = colorScheme.onSurface.copy(alpha = 0.12f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = glassColor,
        border = BorderStroke(
            0.6.dp, glassBorder
        ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SegmentedControl(
                scope,
                onScopeChange,
                hapticFeedback
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            PesaActions.playClick(hapticFeedback)
                            onToggleVisibility()
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "TOTAL BALANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Black,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Icon(
                        imageVector = if (isBalanceVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = "Toggle Visibility",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }

                val balanceText = PesaTimeConfiguration.formatCurrency(currentBalance)
                Text(
                    text = if (isBalanceVisible) {
                        balanceText
                    } else {
                        "${PesaTimeConfiguration.CURRENCY} ••••••••"
                    },
                    modifier = Modifier.clickable {
                        if (isBalanceVisible) {
                            PesaActions.copyToClipboard(
                                context,
                                clipboard,
                                hapticFeedback,
                                "Balance",
                                balanceText
                            )
                        }
                    },
                    style =
                        when {
                            balanceText.length > 15 -> MaterialTheme.typography.headlineSmall
                            balanceText.length > 12 -> MaterialTheme.typography.headlineMedium
                            else -> MaterialTheme.typography.displaySmall
                        }.copy(
                            brush = primaryGradient(),
                        ),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape) // Glass outer rim
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            PesaActions.playClick(hapticFeedback)
                        }, contentAlignment = Alignment.Center
                ) {
                    SummaryItem(
                        "Amount Received",
                        summaryData.income,
                        MaterialTheme.colorScheme.tertiary,
                        Icons.Rounded.TrendingUp
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            PesaActions.playClick(hapticFeedback)
                        }, contentAlignment = Alignment.Center
                ) {
                    SummaryItem(
                        "Amount Spent",
                        summaryData.expenses,
                        MaterialTheme.colorScheme.error,
                        Icons.Rounded.TrendingDown
                    )
                }
            }
        }
    }
}


@Composable
fun SegmentedControl(
    selectedScope: TimeScope,
    onScopeChange: (TimeScope) -> Unit,
    hapticFeedback: HapticFeedback
) {
    val items = remember { TimeScope.entries.filter { it != TimeScope.CUSTOM } }
    val selectedIndex = items.indexOf(selectedScope)

    val animateIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillSelection"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp) // Slightly taller for better touch targets
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape) // Glass outer rim
            .padding(4.dp)
    ) {
        val maxWidth = maxWidth
        val itemWidth = maxWidth / items.size

        // The Gradient Sliding Pill
        Box(
            modifier = Modifier
                .offset(x = itemWidth * animateIndex)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary // Adds that "pulse" color transition
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape) // Inner highlight
        )

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, scope ->
                val isSelected = index == selectedIndex

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            PesaActions.playClick(hapticFeedback)
                            onScopeChange(scope)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = scope.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}


@Composable
fun SummaryItem(label: String, amount: Double, color: Color, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = PesaTimeConfiguration.formatCurrency(amount),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}