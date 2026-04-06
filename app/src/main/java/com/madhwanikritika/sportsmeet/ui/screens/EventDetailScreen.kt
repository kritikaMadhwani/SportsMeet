package com.madhwanikritika.sportsmeet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.madhwanikritika.sportsmeet.data.model.Registrant
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.madhwanikritika.sportsmeet.data.model.Event
import com.madhwanikritika.sportsmeet.data.model.User
import com.madhwanikritika.sportsmeet.ui.util.formatUsd
import com.madhwanikritika.sportsmeet.ui.viewmodel.AuthViewModel
import com.madhwanikritika.sportsmeet.ui.viewmodel.EventDetailViewModel
import com.madhwanikritika.sportsmeet.ui.viewmodel.EventViewModel
import com.madhwanikritika.sportsmeet.ui.viewmodel.PlayerWalletViewModel
import com.madhwanikritika.sportsmeet.ui.viewmodel.RegistrationUiState
import com.madhwanikritika.sportsmeet.ui.viewmodel.WalletViewModel
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    authViewModel: AuthViewModel,
    eventViewModel: EventViewModel,
    onBack: () -> Unit
) {
    val activity = LocalContext.current as ComponentActivity
    val detailVm: EventDetailViewModel = hiltViewModel()
    val playerWalletVm: PlayerWalletViewModel = hiltViewModel(activity)
    val walletVm: WalletViewModel = hiltViewModel(activity)
    val event by detailVm.event.collectAsState()
    val registrants by detailVm.registrants.collectAsState()
    val allUsers by walletVm.allUsers.collectAsState()
    val user by authViewModel.currentUser.collectAsState()
    val balance by playerWalletVm.balance.collectAsState()
    val config by playerWalletVm.appConfig.collectAsState()
    val regState by eventViewModel.registrationState.collectAsState()
    var registered by remember { mutableStateOf(false) }

    LaunchedEffect(user?.userId, event?.eventId) {
        val uid = user?.userId ?: return@LaunchedEffect
        val eid = event?.eventId ?: return@LaunchedEffect
        playerWalletVm.setUserId(uid)
        registered = eventViewModel.isRegistered(eid, uid)
    }

    LaunchedEffect(regState) {
        when (val s = regState) {
            is RegistrationUiState.Success -> {
                detailVm.reloadEventAndRegistrants()
                user?.userId?.let { uid ->
                    event?.eventId?.let { eid ->
                        registered = eventViewModel.isRegistered(eid, uid)
                    }
                }
                eventViewModel.clearRegistrationState()
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Event") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val ev = event
        if (ev == null) {
            Text("Loading…", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        val isAdmin = user?.role == "admin"
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            ev.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            ev.sport,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        DetailRow(
                            "Date",
                            java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.getDefault())
                                .format(java.util.Date(ev.dateMillis))
                        )
                        DetailRow("Time", ev.time)
                        DetailRow("Location", ev.location)
                        DetailRow("Price", formatUsd(ev.price))
                        val left = (ev.totalSeats - ev.filledSeats).coerceAtLeast(0)
                        DetailRow("Seats left", "$left / ${ev.totalSeats}")
                        val filled = if (ev.totalSeats > 0) ev.filledSeats.toFloat() / ev.totalSeats.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { filled.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
            if (isAdmin) {
                item {
                    AdminAddPlayerCard(
                        event = ev,
                        allUsers = allUsers,
                        registrants = registrants,
                        regState = regState,
                        onAddPlayer = { player, price ->
                            eventViewModel.adminRegisterPlayerForEvent(ev, player, price)
                        }
                    )
                }
                item { Text("Registrants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                items(registrants, key = { it.userId }) { r ->
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        val paid = if (r.pricePaid > 0.0) r.pricePaid else ev.price
                        Column(Modifier.padding(12.dp)) {
                            Text("${r.name} · ${r.ticketCode}")
                            Text(
                                "Charged: ${formatUsd(paid)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    user?.let { u ->
                        PlayerRegisterSection(
                            event = ev,
                            user = u,
                            balance = balance,
                            adminInterac = config.adminInteracEmail,
                            registered = registered,
                            regState = regState,
                            onRegister = {
                                eventViewModel.registerForEvent(ev, u)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlayerRegisterSection(
    event: Event,
    user: User,
    balance: Double,
    adminInterac: String,
    registered: Boolean,
    regState: RegistrationUiState,
    onRegister: () -> Unit
) {
    Text("Wallet balance: ${formatUsd(balance)}", style = MaterialTheme.typography.titleSmall)
    val canAfford = balance >= event.price
    val soldOut = event.filledSeats >= event.totalSeats
    val interac = adminInterac.ifBlank {
        // TODO("set adminInteracEmail in Firebase config/app doc")
        ""
    }
    if (registered) {
        Text("You're registered ✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    } else if (soldOut) {
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Sold Out") }
    } else if (!canAfford) {
        Text(
            "Insufficient balance (${formatUsd(balance)}). Please transfer funds to the admin via Interac and ask them to top up your wallet." +
                if (interac.isNotBlank()) " Interac: $interac" else "",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("Register — ${formatUsd(event.price)} deducted from wallet")
        }
    } else {
        if (regState is RegistrationUiState.Error) {
            Text(regState.message, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onRegister,
            enabled = regState !is RegistrationUiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            val label = if (regState is RegistrationUiState.Loading) "Registering…" else "Register — ${formatUsd(event.price)} deducted from wallet"
            Text(label)
        }
    }
}

@Composable
private fun AdminAddPlayerCard(
    event: Event,
    allUsers: List<User>,
    registrants: List<Registrant>,
    regState: RegistrationUiState,
    onAddPlayer: (User, Double) -> Unit
) {
    val eligible = remember(allUsers, registrants) {
        allUsers.filter { u ->
            u.role == "user" &&
                registrants.none { it.userId == u.userId }
        }
    }
    var selected by remember { mutableStateOf<User?>(null) }
    var priceText by remember(event.eventId) { mutableStateOf(event.price.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add player", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Set the amount to charge this player’s wallet (default is the event list price).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (eligible.isEmpty()) {
                Text(
                    "No players left to add (everyone is registered or there are no player accounts).",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    "Select player",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    eligible.forEach { u ->
                        FilterChip(
                            selected = selected?.userId == u.userId,
                            onClick = { selected = u },
                            label = {
                                Text("${u.name} · ${formatUsd(u.walletBalance)}")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Charge amount") },
                modifier = Modifier.fillMaxWidth()
            )
            val charge = priceText.toDoubleOrNull()
            val sel = selected
            val canCharge = sel != null && charge != null && charge >= 0 && sel.walletBalance >= charge
            val soldOut = event.filledSeats >= event.totalSeats
            if (regState is RegistrationUiState.Error) {
                Text(
                    regState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = {
                    val s = sel ?: return@Button
                    val p = charge ?: return@Button
                    onAddPlayer(s, p)
                },
                enabled = sel != null && charge != null && charge >= 0 && canCharge && !soldOut && regState !is RegistrationUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                val label = when {
                    soldOut -> "Sold out"
                    regState is RegistrationUiState.Loading -> "Adding…"
                    charge == null || charge < 0 -> "Enter a valid price"
                    sel != null && !canCharge -> "Insufficient balance (${formatUsd(sel.walletBalance)})"
                    else -> "Add player — ${formatUsd(charge ?: 0.0)} from wallet"
                }
                Text(label)
            }
        }
    }
}
