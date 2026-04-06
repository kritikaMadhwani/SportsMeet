package com.madhwanikritika.sportsmeet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madhwanikritika.sportsmeet.data.model.User
import com.madhwanikritika.sportsmeet.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val user: User) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun tryRestoreSession(onResult: (User) -> Unit) {
        viewModelScope.launch {
            val uid = authRepository.currentAuthUid ?: return@launch
            val u = authRepository.refreshUser(uid) ?: return@launch
            _currentUser.value = u
            onResult(u)
        }
    }

    /**
     * Waits [delayMs], then restores session if Firebase Auth has a user; otherwise invokes [onNoUser].
     */
    fun completeSplashNavigation(
        delayMs: Long = 3000L,
        onUser: (User) -> Unit,
        onNoUser: () -> Unit
    ) {
        viewModelScope.launch {
            delay(delayMs)
            val uid = authRepository.currentAuthUid
            if (uid == null) {
                onNoUser()
                return@launch
            }
            val u = authRepository.refreshUser(uid)
            if (u != null) {
                _currentUser.value = u
                onUser(u)
            } else {
                onNoUser()
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signIn(email, password)
            _uiState.value = result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    AuthUiState.Success(user)
                },
                onFailure = { e -> AuthUiState.Error(e.message ?: "Sign in failed") }
            )
        }
    }

    fun signUp(email: String, password: String, name: String, phone: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signUp(email, password, name, phone)
            _uiState.value = result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    AuthUiState.Success(user)
                },
                onFailure = { e -> AuthUiState.Error(e.message ?: "Sign up failed") }
            )
        }
    }

    fun clearUiState() {
        _uiState.value = AuthUiState.Idle
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _currentUser.value = null
        }
    }

    fun setCurrentUser(user: User?) {
        _currentUser.value = user
    }
}
