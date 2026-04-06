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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.madhwanikritika.sportsmeet.data.model.Ticket
import com.madhwanikritika.sportsmeet.ui.util.formatUsd
import com.madhwanikritika.sportsmeet.ui.viewmodel.AuthViewModel
import com.madhwanikritika.sportsmeet.ui.viewmodel.EventViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTicketsScreen(
    modifier: Modifier = Modifier,
    eventViewModel: EventViewModel,
    authViewModel: AuthViewModel
) {
    val tickets by eventViewModel.myTickets.collectAsState()
    val user by authViewModel.currentUser.collectAsState()

    LaunchedEffect(user?.userId) {
        user?.userId?.let { eventViewModel.setUserId(it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("My tickets") },
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
            items(tickets, key = { it.ticketId }) { t ->
                TicketCard(t)
            }
        }
    }
}

@Composable
private fun TicketCard(t: Ticket) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t.sport, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(t.eventTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${t.date} · ${t.time}", style = MaterialTheme.typography.bodySmall)
            Text(t.location, style = MaterialTheme.typography.bodySmall)
            Text(formatUsd(t.price), style = MaterialTheme.typography.bodyMedium)
            Text(
                t.ticketCode,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
