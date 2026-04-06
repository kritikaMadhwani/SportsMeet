package com.madhwanikritika.sportsmeet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.madhwanikritika.sportsmeet.data.model.User
import com.madhwanikritika.sportsmeet.ui.util.formatUsd
import com.madhwanikritika.sportsmeet.ui.viewmodel.AuthViewModel
import com.madhwanikritika.sportsmeet.ui.viewmodel.CreditUiState
import com.madhwanikritika.sportsmeet.ui.viewmodel.WalletViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWalletManagerScreen(
    modifier: Modifier = Modifier,
    walletViewModel: WalletViewModel,
    authViewModel: AuthViewModel
) {
    val users by walletViewModel.allUsers.collectAsState()
    val config by walletViewModel.appConfig.collectAsState()
    val creditState by walletViewModel.creditState.collectAsState()
    val selected by walletViewModel.selectedUser.collectAsState()
    val creditAmount by walletViewModel.creditAmount.collectAsState()
    var query by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var sheetOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val admin = authViewModel.currentUser.value

    LaunchedEffect(creditState) {
        if (creditState is CreditUiState.Success && selected != null) {
            snackbar.showSnackbar("Wallet credited. Push notification sent to ${selected!!.name}.")
            walletViewModel.clearCreditState()
            sheetOpen = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Players & wallets") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search by name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                val filtered = users.filter { it.name.contains(query, ignoreCase = true) }
                items(filtered, key = { it.userId }) { u ->
                    val low = u.walletBalance < config.lowBalanceThreshold
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(u.name, style = MaterialTheme.typography.titleMedium)
                                    if (low) {
                                        Icon(
                                            Icons.Default.WarningAmber,
                                            contentDescription = "Low balance",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                                Text(u.email, style = MaterialTheme.typography.bodySmall)
                                Text(formatUsd(u.walletBalance), style = MaterialTheme.typography.bodyLarge)
                            }
                            Button(onClick = {
                                walletViewModel.selectUser(u)
                                sheetOpen = true
                            }) {
                                Text("Top Up")
                            }
                        }
                    }
                }
            }
        }
    }

    if (sheetOpen && selected != null) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CreditSheet(
                user = selected!!,
                creditAmount = creditAmount,
                onAmountChange = { walletViewModel.setCreditAmount(it) },
                description = desc,
                onDescriptionChange = { desc = it },
                onPreset = { walletViewModel.setCreditAmount(it.toString()) },
                onConfirm = {
                    val amt = creditAmount.toDoubleOrNull() ?: 0.0
                    if (admin != null && amt > 0) {
                        walletViewModel.creditWallet(
                            adminUid = admin.userId,
                            targetUid = selected!!.userId,
                            amount = amt,
                            description = desc
                        )
                    }
                },
                onDismiss = { sheetOpen = false },
                loading = creditState is CreditUiState.Loading
            )
        }
    }
}

@Composable
private fun CreditSheet(
    user: User,
    creditAmount: String,
    onAmountChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    onPreset: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    loading: Boolean
) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(user.name, style = MaterialTheme.typography.titleLarge)
        Text("Balance: ${formatUsd(user.walletBalance)}")
        OutlinedTextField(
            value = creditAmount,
            onValueChange = onAmountChange,
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(20, 40, 60, 100).forEach { v ->
                FilterChip(
                    selected = false,
                    onClick = { onPreset(v) },
                    label = { Text("$$v") }
                )
            }
        }
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Button(onClick = onConfirm, enabled = !loading) {
                Text(if (loading) "…" else "Confirm Credit")
            }
        }
    }
}
