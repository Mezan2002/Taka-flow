package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CURRENCY
import com.example.data.model.Category
import com.example.data.model.LoanStatus
import com.example.data.model.PlanKind
import com.example.data.model.PurchasePlan
import com.example.data.model.TransactionType
import com.example.data.model.Wallet
import com.example.domain.CurrencyFormat
import com.example.domain.DateUtils
import com.example.domain.DayRange
import com.example.domain.LoanCalculator
import com.example.ui.components.CircleActionBadge
import com.example.ui.components.DateRangePickerDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassSecondaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.PillTag
import com.example.ui.components.SwipeableGlassRow
import com.example.ui.components.contrastInkFor
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainUiState
import java.util.UUID

@Composable
fun PlansLoansScreen(
    state: MainUiState,
    onSavePlan: (PurchasePlan) -> Unit,
    onDeletePlan: (PurchasePlan) -> Unit,
    onCompleteItemPlan: (PurchasePlan, Double, String) -> Unit,
    onReopenPlan: (PurchasePlan) -> Unit,
    onAddRepayment: (PurchasePlan, Double, String) -> Unit,
    onRemoveRepayment: (PurchasePlan, String) -> Unit,
    onSettleLoan: (PurchasePlan) -> Unit,
) {
    val P = LocalGlassPalette.current
    var selectedKind by remember { mutableStateOf(PlanKind.ITEM) }
    var editingPlan by remember { mutableStateOf<PurchasePlan?>(null) }
    var showCreateModal by remember { mutableStateOf(false) }
    var completingPlan by remember { mutableStateOf<PurchasePlan?>(null) }
    var repayingLoan by remember { mutableStateOf<PurchasePlan?>(null) }
    var detailsLoanId by remember { mutableStateOf<String?>(null) }

    val (owedToYou, youOwe) = remember(state.plans) { LoanCalculator.loanTotals(state.plans) }
    val displayed = remember(state.plans, selectedKind) {
        state.plans.filter { it.kind == selectedKind }
    }
    val detailsLoan = detailsLoanId?.let { id -> state.plans.firstOrNull { it.id == id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Plans & Loans", style = MaterialTheme.typography.headlineMedium, color = P.ink)
                GlassPrimaryButton(
                    text = "ADD",
                    icon = Icons.Filled.Add,
                    onClick = {
                        editingPlan = null
                        showCreateModal = true
                    },
                    modifier = Modifier.width(108.dp).testTag("plans_add_button"),
                )
            }
        }

        // Bold Acid Lime & Coral Crimson Loan Summary Blocks
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassCard(
                    tint = P.acidLime,
                    cornerRadius = 24.dp,
                    padding = PaddingValues(16.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("OWED TO YOU", style = MaterialTheme.typography.labelSmall, color = P.obsidian.copy(alpha = 0.72f))
                    Spacer(Modifier.height(6.dp))
                    Text(CurrencyFormat.formatAmount(owedToYou), style = MaterialTheme.typography.titleLarge, color = P.obsidian)
                }
                GlassCard(
                    tint = P.coralRed,
                    cornerRadius = 24.dp,
                    padding = PaddingValues(16.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("YOU OWE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(6.dp))
                    Text(CurrencyFormat.formatAmount(youOwe), style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }
        }

        // Filter tabs: ITEMS | LENT | BORROWED
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip(
                    label = "ITEMS",
                    selected = selectedKind == PlanKind.ITEM,
                    accent = P.heroYellow,
                    onClick = { selectedKind = PlanKind.ITEM },
                    modifier = Modifier.weight(1f).testTag("plans_tab_items"),
                )
                GlassChip(
                    label = "LENT",
                    selected = selectedKind == PlanKind.LENT,
                    accent = P.acidLime,
                    onClick = { selectedKind = PlanKind.LENT },
                    modifier = Modifier.weight(1f).testTag("plans_tab_lent"),
                )
                GlassChip(
                    label = "BORROWED",
                    selected = selectedKind == PlanKind.BORROWED,
                    accent = P.coralRed,
                    onClick = { selectedKind = PlanKind.BORROWED },
                    modifier = Modifier.weight(1f).testTag("plans_tab_borrowed"),
                )
            }
        }

        if (displayed.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    val emptyMsg = when (selectedKind) {
                        PlanKind.ITEM -> "No purchase plans yet. Add items you plan to buy with an optional target date."
                        PlanKind.LENT -> "No money lent yet. Track who owes you with due dates and partial repayments."
                        PlanKind.BORROWED -> "No borrowed loans yet. Keep track of repayments without touching wallet balances."
                    }
                    Text(emptyMsg, style = MaterialTheme.typography.bodyMedium, color = P.subInk)
                }
            }
        } else {
            val stageColors = listOf(P.electricViolet, P.marigold, P.coralRed, P.acidLime)
            itemsIndexed(displayed, key = { _, p -> p.id }) { idx, plan ->
                val blockTint = stageColors[idx % stageColors.size]
                SwipeableGlassRow(
                    onEdit = {
                        editingPlan = plan
                        showCreateModal = true
                    },
                    onDelete = { onDeletePlan(plan) },
                ) {
                    if (plan.kind == PlanKind.ITEM) {
                        ItemPlanCard(
                            plan = plan,
                            blockTint = if (plan.completed) P.acidLime else blockTint,
                            onToggleBought = {
                                if (plan.completed) {
                                    onReopenPlan(plan)
                                } else {
                                    completingPlan = plan
                                }
                            },
                            onEdit = {
                                editingPlan = plan
                                showCreateModal = true
                            },
                        )
                    } else {
                        LoanCard(
                            plan = plan,
                            onAddPayment = { repayingLoan = plan },
                            onOpenDetails = { detailsLoanId = plan.id },
                        )
                    }
                }
            }
        }
    }

    if (showCreateModal) {
        PlanLoanFormDialog(
            initialKind = editingPlan?.kind ?: selectedKind,
            editing = editingPlan,
            categories = state.categories.filter { it.type == TransactionType.EXPENSE },
            existingPeople = LoanCalculator.personNames(state.plans),
            onDismiss = { showCreateModal = false },
            onSave = { plan ->
                onSavePlan(plan)
                showCreateModal = false
            },
        )
    }

    completingPlan?.let { plan ->
        CompletePlanDialog(
            plan = plan,
            wallets = state.wallets,
            onDismiss = { completingPlan = null },
            onConfirm = { actualAmt, walletId ->
                onCompleteItemPlan(plan, actualAmt, walletId)
                completingPlan = null
            },
        )
    }

    repayingLoan?.let { loan ->
        LoanRepaymentDialog(
            loan = loan,
            onDismiss = { repayingLoan = null },
            onSave = { amount, note ->
                onAddRepayment(loan, amount, note)
                repayingLoan = null
            },
        )
    }

    detailsLoan?.let { loan ->
        LoanDetailsDialog(
            loan = loan,
            onDismiss = { detailsLoanId = null },
            onEdit = {
                detailsLoanId = null
                editingPlan = loan
                showCreateModal = true
            },
            onAddPayment = {
                detailsLoanId = null
                repayingLoan = loan
            },
            onDeleteRepayment = { repId -> onRemoveRepayment(loan, repId) },
            onSettleAll = { onSettleLoan(loan) },
        )
    }
}

@Composable
private fun ItemPlanCard(
    plan: PurchasePlan,
    blockTint: Color,
    onToggleBought: () -> Unit,
    onEdit: () -> Unit,
) {
    val P = LocalGlassPalette.current
    val ink = contrastInkFor(blockTint)
    GlassCard(
        tint = blockTint,
        cornerRadius = 26.dp,
        padding = PaddingValues(18.dp),
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PillTag(
                text = if (plan.completed) "BOUGHT" else plan.category.uppercase(),
                containerColor = P.pureWhite,
                contentColor = P.obsidian,
            )
            CircleActionBadge(
                icon = if (plan.completed) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                contentDescription = if (plan.completed) "Reopen plan" else "Mark bought",
                containerColor = P.obsidian,
                iconTint = P.pureWhite,
                size = 38.dp,
                onClick = onToggleBought,
                modifier = Modifier.testTag("plan_check_${plan.id}"),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(plan.name, style = MaterialTheme.typography.headlineSmall, color = ink)
        Spacer(Modifier.height(4.dp))
        val amt = if (plan.completed && plan.actualAmount != null) plan.actualAmount else plan.expectedAmount
        val dateStr = plan.targetDate?.let { " · Target ${DateUtils.formatShortDate(it)}" } ?: ""
        Text(
            text = "${CurrencyFormat.formatAmount(amt)}$dateStr",
            style = MaterialTheme.typography.titleMedium,
            color = ink.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun LoanCard(
    plan: PurchasePlan,
    onAddPayment: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    val P = LocalGlassPalette.current
    val status = LoanCalculator.loanStatus(plan)
    val dueLabel = LoanCalculator.dueLabel(plan)
    val paid = LoanCalculator.loanPaid(plan)
    val remaining = LoanCalculator.loanRemaining(plan)
    val progress = LoanCalculator.loanProgress(plan)

    // Image-2-right-screen Stage card colors per status
    val cardTint = when (status) {
        LoanStatus.SETTLED -> P.acidLime
        LoanStatus.OVERDUE -> P.coralRed
        LoanStatus.DUE_SOON -> P.marigold
        LoanStatus.ON_TRACK -> P.electricViolet
    }
    val ink = contrastInkFor(cardTint)

    GlassCard(
        tint = cardTint,
        cornerRadius = 26.dp,
        padding = PaddingValues(18.dp),
        onClick = onOpenDetails,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PillTag(
                text = dueLabel,
                containerColor = P.pureWhite,
                contentColor = P.obsidian,
            )
            CircleActionBadge(
                icon = Icons.Filled.NorthEast,
                contentDescription = "Open loan details",
                containerColor = P.obsidian,
                iconTint = P.pureWhite,
                size = 38.dp,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(plan.personName.ifBlank { plan.name }, style = MaterialTheme.typography.headlineSmall, color = ink)
        if (plan.note.isNotBlank()) {
            Text(plan.note, style = MaterialTheme.typography.bodyMedium, color = ink.copy(alpha = 0.82f))
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = ink,
            trackColor = ink.copy(alpha = 0.22f),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Paid ${CurrencyFormat.formatAmount(paid)} / ${CurrencyFormat.formatAmount(plan.expectedAmount)}",
                style = MaterialTheme.typography.labelMedium,
                color = ink.copy(alpha = 0.9f),
            )
            if (!plan.completed && remaining > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(P.pureWhite)
                        .clickable(onClick = onAddPayment)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("loan_add_payment_${plan.id}"),
                ) {
                    Text("ADD PAYMENT", style = MaterialTheme.typography.labelSmall, color = P.obsidian, fontWeight = FontWeight.Bold)
                }
            } else {
                PillTag(text = "SETTLED", containerColor = P.obsidian, contentColor = P.acidLime)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanLoanFormDialog(
    initialKind: PlanKind,
    editing: PurchasePlan?,
    categories: List<Category>,
    existingPeople: List<String>,
    onDismiss: () -> Unit,
    onSave: (PurchasePlan) -> Unit,
) {
    val P = LocalGlassPalette.current
    var kind by remember(editing, initialKind) { mutableStateOf(editing?.kind ?: initialKind) }
    var name by remember(editing) { mutableStateOf(editing?.name ?: "") }
    var person by remember(editing) { mutableStateOf(editing?.personName ?: "") }
    var amountText by remember(editing) {
        mutableStateOf(editing?.expectedAmount?.takeIf { it > 0 }?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    var category by remember(editing) { mutableStateOf(editing?.category ?: "Shopping") }
    var note by remember(editing) { mutableStateOf(editing?.note ?: "") }
    var selectedDate by remember(editing) {
        mutableStateOf(if ((editing?.kind ?: initialKind) == PlanKind.ITEM) editing?.targetDate else editing?.dueDate)
    }
    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GlassCard(
            tint = P.glassModal,
            cornerRadius = 26.dp,
            padding = PaddingValues(18.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Text(
                text = if (editing != null) "Edit Entry" else "New Plan or Loan",
                style = MaterialTheme.typography.titleLarge,
                color = P.ink,
            )
            Spacer(Modifier.height(10.dp))
            if (editing == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlanKind.entries.forEach { k ->
                        GlassChip(label = k.name, selected = kind == k, accent = P.heroYellow, onClick = { kind = k }, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (kind == PlanKind.ITEM) {
                GlassTextField(value = name, onValueChange = { name = it }, label = "Item Name", placeholder = "e.g., Smart Watch")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.take(6).forEach { cat ->
                        GlassChip(label = cat.name, selected = category == cat.name, onClick = { category = cat.name })
                    }
                }
            } else {
                GlassTextField(value = person, onValueChange = { person = it }, label = "Person Name", placeholder = "e.g., Rahim")
                if (existingPeople.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        existingPeople.take(5).forEach { p ->
                            GlassChip(label = p, selected = person == p, onClick = { person = p })
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            GlassTextField(
                value = amountText,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) amountText = it },
                label = if (kind == PlanKind.ITEM) "Expected Amount ($CURRENCY)" else "Principal ($CURRENCY)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Spacer(Modifier.height(10.dp))
            GlassTextField(value = note, onValueChange = { note = it }, label = "Note (optional)")
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(P.glassChip)
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = P.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = selectedDate?.let { DateUtils.formatMediumDate(it) }
                            ?: if (kind == PlanKind.ITEM) "Target Date (Optional)" else "Due Date (Optional)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = P.ink,
                    )
                }
                if (selectedDate != null) {
                    IconButton(onClick = { selectedDate = null }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear date", tint = P.subInk, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            val amt = amountText.toDoubleOrNull() ?: 0.0
            val valid = if (kind == PlanKind.ITEM) name.isNotBlank() else person.isNotBlank() && amt > 0.0
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("CANCEL", color = P.subInk) }
                Spacer(Modifier.width(8.dp))
                GlassPrimaryButton(
                    text = "SAVE",
                    enabled = valid,
                    onClick = {
                        onSave(
                            PurchasePlan(
                                id = editing?.id ?: "plan_${UUID.randomUUID()}",
                                kind = kind,
                                name = if (kind == PlanKind.ITEM) name.trim() else person.trim(),
                                personName = if (kind == PlanKind.ITEM) "" else person.trim(),
                                expectedAmount = CurrencyFormat.round2(amt),
                                category = category,
                                note = note.trim(),
                                targetDate = if (kind == PlanKind.ITEM) selectedDate else null,
                                dueDate = if (kind == PlanKind.ITEM) null else selectedDate,
                                completed = editing?.completed ?: false,
                                completedAt = editing?.completedAt,
                                actualAmount = editing?.actualAmount,
                                walletId = editing?.walletId,
                                createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                                repayments = editing?.repayments ?: emptyList(),
                            )
                        )
                    },
                    modifier = Modifier.width(110.dp),
                )
            }
        }
    }

    if (showDatePicker) {
        val base = selectedDate ?: System.currentTimeMillis()
        DateRangePickerDialog(
            initialRange = DayRange(DateUtils.startOfDay(base), DateUtils.endOfDay(base)),
            allowRangeMode = false,
            onDismiss = { showDatePicker = false },
            onApply = { range ->
                selectedDate = range.startMillis
                showDatePicker = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompletePlanDialog(
    plan: PurchasePlan,
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit,
) {
    val P = LocalGlassPalette.current
    var amountText by remember {
        mutableStateOf(if (plan.expectedAmount > 0) plan.expectedAmount.toInt().toString() else "")
    }
    var walletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: "wallet_cash") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark \"${plan.name}\" Bought") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassTextField(
                    value = amountText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) amountText = it },
                    label = "Actual Amount ($CURRENCY)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text("DEDUCT FROM WALLET", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    wallets.forEach { w ->
                        GlassChip(label = w.name, selected = walletId == w.id, onClick = { walletId = w.id })
                    }
                }
            }
        },
        confirmButton = {
            val amt = amountText.toDoubleOrNull() ?: 0.0
            TextButton(onClick = { onConfirm(amt, walletId) }) { Text("CONFIRM") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
    )
}

@Composable
private fun LoanRepaymentDialog(
    loan: PurchasePlan,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit,
) {
    val P = LocalGlassPalette.current
    val remaining = LoanCalculator.loanRemaining(loan)
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val parsed = amountText.toDoubleOrNull() ?: 0.0
    val isOverpay = parsed > remaining + 0.001
    val canSave = parsed > 0.0 && !isOverpay

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment (${loan.personName})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Remaining: ${CurrencyFormat.formatAmount(remaining)}", style = MaterialTheme.typography.bodyMedium, color = P.primary)
                GlassTextField(
                    value = amountText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) amountText = it },
                    label = "Payment Amount ($CURRENCY)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                GlassSecondaryButton(
                    text = "FULL REMAINING (${CurrencyFormat.formatAmount(remaining)})",
                    onClick = {
                        amountText = if (remaining % 1.0 == 0.0) remaining.toInt().toString() else remaining.toString()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("loan_full_remaining_button"),
                )
                if (isOverpay) {
                    Text(
                        "Cannot exceed remaining balance (${CurrencyFormat.formatAmount(remaining)}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = P.expense,
                    )
                }
                GlassTextField(value = note, onValueChange = { note = it }, label = "Note (optional)")
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onSave(parsed, note) },
                modifier = Modifier.testTag("loan_repayment_save"),
            ) { Text("SAVE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
    )
}

@Composable
private fun LoanDetailsDialog(
    loan: PurchasePlan,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onAddPayment: () -> Unit,
    onDeleteRepayment: (String) -> Unit,
    onSettleAll: () -> Unit,
) {
    val P = LocalGlassPalette.current
    var armedRepaymentId by remember { mutableStateOf<String?>(null) }
    var armedSettleAll by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GlassCard(
            tint = P.glassModal,
            cornerRadius = 26.dp,
            padding = PaddingValues(18.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(loan.kind.name, style = MaterialTheme.typography.labelSmall, color = P.primary)
                    Text(loan.personName, style = MaterialTheme.typography.titleLarge, color = P.ink)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit loan", tint = P.primary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Principal: ${CurrencyFormat.formatAmount(loan.expectedAmount)} · Remaining: ${CurrencyFormat.formatAmount(LoanCalculator.loanRemaining(loan))}",
                style = MaterialTheme.typography.bodyMedium,
                color = P.subInk,
            )
            Spacer(Modifier.height(12.dp))
            Text("PAYMENTS (${loan.repayments.size})", style = MaterialTheme.typography.labelSmall, color = P.subInk)
            Spacer(Modifier.height(6.dp))
            if (loan.repayments.isEmpty()) {
                Text("No payments recorded yet.", style = MaterialTheme.typography.bodyMedium, color = P.mutedInk)
            } else {
                loan.repayments.forEach { rep ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(CurrencyFormat.formatAmount(rep.amount), style = MaterialTheme.typography.titleMedium, color = P.income)
                            Text(
                                "${DateUtils.formatMediumDate(rep.paidAt)}${if (rep.note.isNotBlank()) " · ${rep.note}" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = P.subInk,
                            )
                        }
                        if (armedRepaymentId == rep.id) {
                            TextButton(onClick = {
                                armedRepaymentId = null
                                onDeleteRepayment(rep.id)
                            }) {
                                Text("CONFIRM DELETE", color = P.expense, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            IconButton(onClick = { armedRepaymentId = rep.id }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete payment", tint = P.expense)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            if (!loan.completed) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassSecondaryButton(
                        text = if (armedSettleAll) "TAP TO CONFIRM" else "SETTLE ALL",
                        tint = P.income,
                        onClick = {
                            if (armedSettleAll) {
                                armedSettleAll = false
                                onSettleAll()
                            } else {
                                armedSettleAll = true
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("loan_settle_all_button"),
                    )
                    GlassPrimaryButton(
                        text = "ADD PAYMENT",
                        onClick = onAddPayment,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("CLOSE", color = P.subInk) }
            }
        }
    }
}
