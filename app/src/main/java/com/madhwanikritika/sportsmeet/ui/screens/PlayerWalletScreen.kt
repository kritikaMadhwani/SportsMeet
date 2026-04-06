package com.madhwanikritika.sportsmeet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.madhwanikritika.sportsmeet.data.model.WalletTransaction
import com.madhwanikritika.sportsmeet.ui.util.formatUsd
import com.madhwanikritika.sportsmeet.ui.viewmodel.AuthViewModel
import com.madhwanikritika.sportsmeet.ui.viewmodel.PlayerWalletViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerWalletScreen(
    modifier: Modifier = Modifier,
    playerWalletViewModel: PlayerWalletViewModel,
    authViewModel: AuthViewModel
) {
    val user by authViewModel.currentUser.collectAsState()
    val balance by playerWalletViewModel.balance.collectAsState()
    val txs by playerWalletViewModel.transactions.collectAsState()
    val config by playerWalletViewModel.appConfig.collectAsState()

    LaunchedEffect(user?.userId) {
        user?.userId?.let { playerWalletViewModel.setUserId(it) }
    }

    val low = balance < config.lowBalanceThreshold
    val fmt = androidx.compose.runtime.remember {
        SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Wallet") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    formatUsd(balance),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (low) {
                item {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Text(
                            "Balance is running low. Send funds to ${config.adminInteracEmail.ifBlank { "your admin's Interac email" }} via Interac and ask the admin to top up.",
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            item { Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(txs, key = { it.transactionId }) { tx ->
                TxRow(tx, fmt)
            }
        }
    }
}

@Composable
private fun TxRow(tx: WalletTransaction, fmt: SimpleDateFormat) {
    val ts = tx.timestamp?.toDate()?.time ?: 0L
    val dateStr = if (ts > 0) fmt.format(Date(ts)) else ""
    val isCredit = tx.type == "credit"
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                if (isCredit) "↑ Topped up" else "↓ ${tx.description}",
                color = if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${if (isCredit) "+" else "-"}${formatUsd(tx.amount)} · $dateStr",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
