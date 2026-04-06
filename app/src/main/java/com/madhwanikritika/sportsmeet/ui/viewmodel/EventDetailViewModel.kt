package com.madhwanikritika.sportsmeet.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madhwanikritika.sportsmeet.data.model.Event
import com.madhwanikritika.sportsmeet.data.model.Registrant
import com.madhwanikritika.sportsmeet.data.repository.EventRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.madhwanikritika.sportsmeet.data.firestore.FirestorePaths
import com.madhwanikritika.sportsmeet.data.firestore.toRegistrant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val firestore: FirebaseFirestore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventId: String = savedStateHandle.get<String>("eventId").orEmpty()

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _registrants = MutableStateFlow<List<Registrant>>(emptyList())
    val registrants: StateFlow<List<Registrant>> = _registrants.asStateFlow()

    init {
        viewModelScope.launch {
            _event.value = eventRepository.getEvent(eventId)
            val qs = firestore.collection(FirestorePaths.EVENTS).document(eventId)
                .collection(FirestorePaths.REGISTRANTS)
                .orderBy("registeredAt", Query.Direction.DESCENDING)
                .get().await()
            _registrants.value = qs.documents.mapNotNull { it.toRegistrant() }
        }
    }

    suspend fun refreshEvent() {
        _event.value = eventRepository.getEvent(eventId)
    }

    fun reloadEventAndRegistrants() {
        viewModelScope.launch {
            _event.value = eventRepository.getEvent(eventId)
            val qs = firestore.collection(FirestorePaths.EVENTS).document(eventId)
                .collection(FirestorePaths.REGISTRANTS)
                .orderBy("registeredAt", Query.Direction.DESCENDING)
                .get().await()
            _registrants.value = qs.documents.mapNotNull { it.toRegistrant() }
        }
    }
}
