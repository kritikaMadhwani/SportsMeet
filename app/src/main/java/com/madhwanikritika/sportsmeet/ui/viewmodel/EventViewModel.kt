package com.madhwanikritika.sportsmeet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madhwanikritika.sportsmeet.data.model.Event
import com.madhwanikritika.sportsmeet.data.model.PlayerRegistrationLine
import com.madhwanikritika.sportsmeet.data.model.Ticket
import com.madhwanikritika.sportsmeet.data.model.User
import com.madhwanikritika.sportsmeet.data.repository.EventRepository
import com.madhwanikritika.sportsmeet.domain.InsufficientBalanceException
import com.madhwanikritika.sportsmeet.domain.SoldOutException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

sealed interface RegistrationUiState {
    data object Idle : RegistrationUiState
    data object Loading : RegistrationUiState
    data class Success(val ticketCode: String) : RegistrationUiState
    data class Error(val message: String) : RegistrationUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _sportFilter = MutableStateFlow<String?>(null)
    val sportFilter: StateFlow<String?> = _sportFilter.asStateFlow()

    private val _uid = MutableStateFlow<String?>(null)

    fun setUserId(uid: String?) {
        _uid.value = uid
    }

    private val allEventsFromFirestore: StateFlow<List<Event>> = eventRepository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val events: StateFlow<List<Event>> = combine(
        allEventsFromFirestore,
        _sportFilter
    ) { all, filter ->
        if (filter.isNullOrBlank()) all else all.filter { it.sport.equals(filter, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allEvents: StateFlow<List<Event>> = allEventsFromFirestore

    val myTickets: StateFlow<List<Ticket>> = _uid.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else eventRepository.getUserTickets(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _registrationState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val registrationState: StateFlow<RegistrationUiState> = _registrationState.asStateFlow()

    fun setSportFilter(sport: String?) {
        _sportFilter.value = sport
    }

    fun registerForEvent(event: Event, currentUser: User) {
        registerInternal(event, currentUser.userId, currentUser.name, priceOverride = null)
    }

    /** Admin adds a player; [priceOverride] defaults to the event list price when null. */
    fun adminRegisterPlayerForEvent(event: Event, player: User, priceOverride: Double? = null) {
        registerInternal(event, player.userId, player.name, priceOverride)
    }

    private fun registerInternal(
        event: Event,
        userId: String,
        userName: String,
        priceOverride: Double?
    ) {
        viewModelScope.launch {
            _registrationState.value = RegistrationUiState.Loading
            if (event.filledSeats >= event.totalSeats) {
                _registrationState.value = RegistrationUiState.Error("This event is sold out.")
                return@launch
            }
            val result = eventRepository.registerForEvent(
                eventId = event.eventId,
                userId = userId,
                userName = userName,
                priceOverride = priceOverride
            )
            _registrationState.value = result.fold(
                onSuccess = { code -> RegistrationUiState.Success(code) },
                onFailure = { e ->
                    when (e) {
                        is InsufficientBalanceException -> RegistrationUiState.Error(e.message ?: "Insufficient balance")
                        is SoldOutException -> RegistrationUiState.Error(e.message ?: "Sold out")
                        else -> RegistrationUiState.Error(e.message ?: "Registration failed")
                    }
                }
            )
        }
    }

    fun clearRegistrationState() {
        _registrationState.value = RegistrationUiState.Idle
    }

    suspend fun isRegistered(eventId: String, uid: String): Boolean =
        eventRepository.isUserRegistered(eventId, uid)

    suspend fun getEvent(eventId: String): Event? = eventRepository.getEvent(eventId)

    fun createEvent(event: Event, adminUid: String, onDone: (Throwable?) -> Unit) {
        viewModelScope.launch {
            try {
                eventRepository.createEvent(event, adminUid)
                onDone(null)
            } catch (e: Exception) {
                onDone(e)
            }
        }
    }

    /**
     * Creates event and registers players with individual prices in one flow.
     */
    fun createEventWithRegistrations(
        event: Event,
        adminUid: String,
        registrations: List<PlayerRegistrationLine>,
        onDone: (Throwable?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (registrations.isEmpty()) {
                    eventRepository.createEvent(event, adminUid)
                    onDone(null)
                } else {
                    val result = eventRepository.createEventWithRegistrations(event, adminUid, registrations)
                    onDone(result.exceptionOrNull())
                }
            } catch (e: Exception) {
                onDone(e)
            }
        }
    }
}
