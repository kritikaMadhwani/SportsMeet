package com.madhwanikritika.sportsmeet.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.madhwanikritika.sportsmeet.data.model.Event
import com.madhwanikritika.sportsmeet.data.model.PlayerRegistrationLine
import com.madhwanikritika.sportsmeet.data.model.User
import com.madhwanikritika.sportsmeet.ui.util.formatUsd
import com.madhwanikritika.sportsmeet.ui.viewmodel.AuthViewModel
import com.madhwanikritika.sportsmeet.ui.viewmodel.EventViewModel
import com.madhwanikritika.sportsmeet.ui.viewmodel.WalletViewModel

private data class SportOption(val name: String, val icon: String)

private data class RegistrationDraftRow(
    val selectedUser: User? = null,
    val priceText: String
)

private val sports = listOf(
    SportOption("Badminton", "🏸"),
    SportOption("Volleyball", "🏐"),
    SportOption("Tennis", "🎾"),
    SportOption("Basketball", "🏀")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEventScreen(
    authViewModel: AuthViewModel,
    eventViewModel: EventViewModel,
    walletViewModel: WalletViewModel,
    onBack: () -> Unit,
    onCreated: () -> Unit
) {
    val admin = authViewModel.currentUser.value
    val allUsers by walletViewModel.allUsers.collectAsState()
    var sport by remember { mutableStateOf(sports.first()) }
    var title by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf<Long?>(null) }
    var time by remember { mutableStateOf("18:00") }
    var location by remember { mutableStateOf("") }
    var seats by remember { mutableStateOf("20") }
    var defaultPrice by remember { mutableStateOf("10") }
    var showDate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var registrationRows by remember { mutableStateOf<List<RegistrationDraftRow>>(emptyList()) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)

    fun availableForRow(rowIndex: Int): List<User> {
        val takenElsewhere = registrationRows.mapIndexedNotNull { i, r ->
            if (i != rowIndex) r.selectedUser?.userId else null
        }.toSet()
        return allUsers.filter { u ->
            u.role == "user" && u.userId !in takenElsewhere
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Create event") },
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Sport", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sports.forEach { s ->
                    FilterChip(
                        selected = sport == s,
                        onClick = { sport = s },
                        label = { Text("${s.icon} ${s.name}") }
                    )
                }
            }
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = dateMillis?.let { java.text.SimpleDateFormat("EEE MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = { showDate = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Choose date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures { showDate = true }
                    }
            )
            if (showDate) {
                DatePickerDialog(
                    onDismissRequest = { showDate = false },
                    confirmButton = {
                        TextButton(onClick = {
                            dateState.selectedDateMillis?.let { dateMillis = it }
                            showDate = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDate = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = dateState)
                }
            }
            OutlinedTextField(time, { time = it }, label = { Text("Time") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(location, { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(seats, { seats = it }, label = { Text("Max seats") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                defaultPrice,
                { defaultPrice = it },
                label = { Text("Default price (self-registration)") },
                supportingText = {
                    Text("Used when players sign up themselves. Per-player prices below override this for admin-added rows.")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Add players now (optional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Choose a player and the amount to charge their wallet for this event. You can set a different price for each person.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            registrationRows.forEachIndexed { index, row ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Player ${index + 1}", style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = {
                            registrationRows = registrationRows.toMutableList().also { it.removeAt(index) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                    val options = availableForRow(index)
                    if (options.isEmpty()) {
                        Text("No players available.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEach { u ->
                                FilterChip(
                                    selected = row.selectedUser?.userId == u.userId,
                                    onClick = {
                                        val list = registrationRows.toMutableList()
                                        list[index] = row.copy(selectedUser = u)
                                        registrationRows = list
                                    },
                                    label = { Text("${u.name} (${formatUsd(u.walletBalance)})") }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = row.priceText,
                        onValueChange = {
                            val list = registrationRows.toMutableList()
                            list[index] = row.copy(priceText = it)
                            registrationRows = list
                        },
                        label = { Text("Price for this player") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            TextButton(
                onClick = {
                    registrationRows = registrationRows + RegistrationDraftRow(
                        selectedUser = null,
                        priceText = defaultPrice
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Add player row")
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    val dm = dateMillis
                    val ts = seats.toIntOrNull()
                    val defaultPr = defaultPrice.toDoubleOrNull()
                    if (dm == null || ts == null || ts <= 0 || defaultPr == null || defaultPr < 0 || title.isBlank() || location.isBlank()) {
                        error = "Fill all fields with valid values."
                        return@Button
                    }
                    if (admin == null) return@Button
                    val lines = mutableListOf<PlayerRegistrationLine>()
                    for (row in registrationRows) {
                        val u = row.selectedUser
                        if (u == null) {
                            error = "Select a player for each added row, or remove empty rows."
                            return@Button
                        }
                        val p = row.priceText.toDoubleOrNull()
                        if (p == null || p < 0) {
                            error = "Each player needs a valid price (≥ 0)."
                            return@Button
                        }
                        lines.add(PlayerRegistrationLine(userId = u.userId, userName = u.name, price = p))
                    }
                    if (lines.size > ts) {
                        error = "You added more players than seats ($ts)."
                        return@Button
                    }
                    error = null
                    val ev = Event(
                        sport = sport.name,
                        icon = sport.icon,
                        title = title.trim(),
                        description = "",
                        dateMillis = dm,
                        time = time.trim(),
                        location = location.trim(),
                        totalSeats = ts,
                        filledSeats = 0,
                        price = defaultPr,
                        createdBy = admin.userId
                    )
                    eventViewModel.createEventWithRegistrations(ev, admin.userId, lines) { err ->
                        if (err == null) onCreated() else error = err.message
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create event")
            }
        }
    }
}
