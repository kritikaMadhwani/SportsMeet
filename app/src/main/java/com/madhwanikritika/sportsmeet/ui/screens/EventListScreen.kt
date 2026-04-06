package com.madhwanikritika.sportsmeet.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ripple
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.madhwanikritika.sportsmeet.data.model.Event
import com.madhwanikritika.sportsmeet.ui.motion.pressTilt3D
import com.madhwanikritika.sportsmeet.ui.util.formatUsd
import com.madhwanikritika.sportsmeet.ui.viewmodel.AuthViewModel
import com.madhwanikritika.sportsmeet.ui.viewmodel.EventViewModel
import com.madhwanikritika.sportsmeet.ui.viewmodel.PlayerWalletViewModel
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    modifier: Modifier = Modifier,
    eventViewModel: EventViewModel,
    authViewModel: AuthViewModel,
    onOpenEvent: (String) -> Unit,
    onWallet: () -> Unit
) {
    val activity = LocalContext.current as ComponentActivity
    val playerWalletVm: PlayerWalletViewModel = hiltViewModel(activity)
    val events by eventViewModel.events.collectAsState()
    val filter by eventViewModel.sportFilter.collectAsState()
    val user by authViewModel.currentUser.collectAsState()
    val balance by playerWalletVm.balance.collectAsState()

    LaunchedEffect(user?.userId) {
        user?.userId?.let { playerWalletVm.setUserId(it) }
    }

    val sports = listOf(null, "Badminton", "Volleyball", "Tennis", "Basketball")
    val fmt = remember { SimpleDateFormat("EEE MMM d", Locale.getDefault()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Explore") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    Text(
                        formatUsd(balance),
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable { onWallet() },
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sports, key = { it ?: "all" }) { s ->
                    val label = s ?: "All"
                    val selected = filter == s
                    FilterChip(
                        selected = selected,
                        onClick = { eventViewModel.setSportFilter(s) },
                        label = { Text(label) }
                    )
                }
            }
            LazyColumn(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(events, key = { _, ev -> ev.eventId }) { index, ev ->
                    StaggeredEventRow(index) {
                        EventCard(ev, fmt, onClick = { onOpenEvent(ev.eventId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StaggeredEventRow(
    index: Int,
    content: @Composable () -> Unit
) {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay((index * 42).toLong().coerceAtMost(360))
        ready = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (ready) 1f else 0f,
        animationSpec = tween(380),
        label = "staggerAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (ready) 0f else 28f,
        animationSpec = tween(380),
        label = "staggerY"
    )
    Box(
        Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY
        }
    ) {
        content()
    }
}

@Composable
private fun EventCard(event: Event, fmt: SimpleDateFormat, onClick: () -> Unit) {
    val filled = if (event.totalSeats > 0) {
        event.filledSeats.toFloat() / event.totalSeats.toFloat()
    } else {
        0f
    }
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressTilt3D(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                event.sport,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${fmt.format(Date(event.dateMillis))} · ${event.location}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatUsd(event.price), style = MaterialTheme.typography.bodyMedium)
            }
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
