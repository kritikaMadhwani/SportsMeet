package com.madhwanikritika.sportsmeet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.madhwanikritika.sportsmeet.ui.viewmodel.AuthViewModel
import com.madhwanikritika.sportsmeet.ui.viewmodel.WalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsScreen(
    modifier: Modifier = Modifier,
    walletViewModel: WalletViewModel,
    authViewModel: AuthViewModel
) {
    val config by walletViewModel.appConfig.collectAsState()
    val admin by authViewModel.currentUser.collectAsState()
    var threshold by remember { mutableStateOf(config.lowBalanceThreshold.toString()) }
    var interac by remember { mutableStateOf(config.adminInteracEmail) }
    var adminEmail by remember { mutableStateOf(config.adminEmail) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Low balance threshold (CAD)",
                style = MaterialTheme.typography.labelLarge
            )
            OutlinedTextField(
                value = threshold,
                onValueChange = { threshold = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                "Your Interac e-Transfer email (shown to players)",
                style = MaterialTheme.typography.labelLarge
            )
            OutlinedTextField(
                value = interac,
                onValueChange = { interac = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("Admin contact email (optional)", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = adminEmail,
                onValueChange = { adminEmail = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = {
                    val t = threshold.toDoubleOrNull() ?: 20.0
                    if (admin != null) {
                        walletViewModel.saveSettings(
                            lowBalance = t,
                            interacEmail = interac,
                            adminEmail = adminEmail,
                            adminUid = admin!!.userId
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
