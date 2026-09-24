package com.example.cashia.ui.screens.allTransactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.ResolvedCategory
import com.example.cashia.models.UnmatchedSms
import com.example.cashia.ui.screens.category.CategoryPickerSheet
import com.example.cashia.utility.TransactionCorrection
import kotlinx.coroutines.launch

@Composable
fun UnmatchedSmsModal(
    sms: UnmatchedSms,
    pendingCount: Int,
    allCategories: List<ResolvedCategory>,
    categoryViewModel: CategoryViewModel,
    onSave: suspend (TransactionCorrection) -> String?,
    onSkip: () -> Unit,
    onIgnore: () -> Unit,
    onDismiss: () -> Unit
) {
    val initial = remember(sms.id) { TransactionCorrection.fromSms(sms) }
    var amount by rememberSaveable(sms.id) { mutableStateOf(initial.amount) }
    var recipient by rememberSaveable(sms.id) { mutableStateOf(initial.recipient) }
    var date by rememberSaveable(sms.id) { mutableStateOf(initial.date) }
    var time by rememberSaveable(sms.id) { mutableStateOf(initial.time) }
    var fees by rememberSaveable(sms.id) { mutableStateOf(initial.fees) }
    var balance by rememberSaveable(sms.id) { mutableStateOf(initial.balance) }
    var reference by rememberSaveable(sms.id) { mutableStateOf(initial.reference) }
    var type by rememberSaveable(sms.id) { mutableStateOf(initial.type) }
    var category by rememberSaveable(sms.id) { mutableStateOf(initial.category) }
    var picker by remember { mutableStateOf(false) }
    var ignoreConfirm by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (picker) CategoryPickerSheet(
        allCategories = allCategories, currentCategoryId = category,
        onPick = { category = it; picker = false }, onDismiss = { picker = false },
        categoryViewModel = categoryViewModel
    )
    if (ignoreConfirm) AlertDialog(
        onDismissRequest = { ignoreConfirm = false }, title = { Text("Ignore this message?") },
        text = { Text("Messages with the same content will be hidden from review.") },
        confirmButton = { TextButton(onClick = onIgnore) { Text("Ignore") } },
        dismissButton = { TextButton(onClick = { ignoreConfirm = false }) { Text("Cancel") } }
    )
    Dialog(onDismissRequest = { if (!saving) onDismiss() }) {
        Surface(shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Correct transaction", style = MaterialTheme.typography.titleLarge)
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("$pendingCount messages need review", style = MaterialTheme.typography.bodySmall)
                    Text(sms.rawBody, style = MaterialTheme.typography.bodySmall)
                    Text("Amount, recipient, date and direction are required. Leave other values blank if unknown.")
                    OutlinedTextField(amount, { amount = it }, label = { Text("Amount (KES) *") }, enabled = !saving, singleLine = true)
                    OutlinedTextField(recipient, { recipient = it }, label = { Text("Recipient / sender *") }, enabled = !saving, singleLine = true)
                    OutlinedTextField(date, { date = it }, label = { Text("Date (YYYY-MM-DD) *") }, enabled = !saving, singleLine = true)
                    OutlinedTextField(time, { time = it }, label = { Text("Time (HH:mm, 24-hour)") }, enabled = !saving, singleLine = true)
                    OutlinedTextField(fees, { fees = it }, label = { Text("Fees (KES)") }, enabled = !saving, singleLine = true)
                    OutlinedTextField(balance, { balance = it }, label = { Text("Balance after (KES)") }, enabled = !saving, singleLine = true)
                    OutlinedTextField(reference, { reference = it }, label = { Text("M-PESA reference") }, enabled = !saving, singleLine = true)
                    Text("Direction *")
                    for ((value, label) in listOf("expense" to "Money out", "income" to "Money in", "neutral" to "No money movement")) {
                        FilterChip(selected = type == value, onClick = { type = value }, label = { Text(label) }, enabled = !saving)
                    }
                    OutlinedButton(onClick = { picker = true }, enabled = !saving) {
                        Text("Category: ${allCategories.find { it.id == category }?.label ?: category}")
                    }
                    Row {
                        TextButton(onClick = onSkip, enabled = !saving) { Text("Skip") }
                        TextButton(onClick = { ignoreConfirm = true }, enabled = !saving) { Text("Ignore") }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
                    Button(enabled = !saving, onClick = {
                        saving = true
                        error = null
                        scope.launch {
                            try {
                                error = onSave(TransactionCorrection(amount, recipient, date, time, fees, balance, reference, type, category))
                                if (error == null) onDismiss()
                            } finally { saving = false }
                        }
                    }) { Text(if (saving) "Saving..." else "Save transaction") }
                }
            }
        }
    }
}
