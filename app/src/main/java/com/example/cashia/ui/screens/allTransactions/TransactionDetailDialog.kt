package com.example.cashia.ui.screens.allTransactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.PesaTimeConfiguration

@Composable
fun TransactionDetailDialog(
    tx: MpesaTransactions,
    accentColor: Color,
    onDismiss: () -> Unit,
    hapticFeedback: HapticFeedback
) {
    val isIncome = tx.type == "income"
    val isExpense = tx.type == "expense"
    val colorScheme = MaterialTheme.colorScheme

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    // [OPT] Fixed AnimatedVisibility(visible = true) anti-pattern — same issue as
    //        UnmatchedSmsModal. The enter animation was silently skipped because Compose
    //        only animates false→true transitions; starting at true means it renders
    //        immediately with no animation. LaunchedEffect triggers on the next frame.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.92f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(28.dp),
                color = colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            SectionLabel("transaction details")
                            Subtitles("${tx.displayDate} · ${tx.displayTime}")
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .background(colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = accentColor.copy(alpha = 0.08f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${if (isIncome) "+" else if (isExpense) "-" else ""}${
                                    PesaTimeConfiguration.formatCurrency(tx.amount)
                                }",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = accentColor
                            )
                            Text(
                                tx.recipient,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.07f))
                    Spacer(Modifier.height(12.dp))
                    TransactionDetailFields(
                        tx = tx,
                        compactLabels = false,
                        showPhone = true,
                        spacing = 6.dp
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tx.reference?.let { reference ->
                            QuickActionChip(
                                label = "Reference",
                                icon = Icons.Rounded.ContentCopy,
                                modifier = Modifier.weight(1f)
                            ) {
                                PesaActions.copyToClipboard(
                                    context,
                                    clipboard,
                                    hapticFeedback,
                                    "Reference",
                                    reference
                                )
                            }
                        }

                        tx.recipientPhone?.takeIf { !it.contains("*") }?.let { phone ->
                            QuickActionChip(
                                label = "Phone",
                                icon = Icons.Rounded.Phone,
                                modifier = Modifier.weight(1f)
                            ) {
                                clipboard.setText(AnnotatedString(phone))
                            }
                        }
                    }
                }
            }
        }
    }
}