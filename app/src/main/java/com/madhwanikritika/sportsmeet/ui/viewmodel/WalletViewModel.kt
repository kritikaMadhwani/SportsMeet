package com.madhwanikritika.sportsmeet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madhwanikritika.sportsmeet.data.model.AppConfig
import com.madhwanikritika.sportsmeet.data.model.User
import com.madhwanikritika.sportsmeet.data.repository.ConfigRepository
import com.madhwanikritika.sportsmeet.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CreditUiState {
    data object Idle : CreditUiState
    data object Loading : CreditUiState
    data object Success : CreditUiState
    data class Error(val message: String) : CreditUiState
}

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    val allUsers: StateFlow<List<User>> = walletRepository.getAllUsersWithBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val appConfig: StateFlow<AppConfig> = configRepository.getAppConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConfig())

    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()

    private val _creditAmount = MutableStateFlow("")
    val creditAmount: StateFlow<String> = _creditAmount.asStateFlow()

    fun setCreditAmount(value: String) {
        _creditAmount.value = value
    }

    private val _creditState = MutableStateFlow<CreditUiState>(CreditUiState.Idle)
    val creditState: StateFlow<CreditUiState> = _creditState.asStateFlow()

    fun selectUser(user: User?) {
        _selectedUser.value = user
    }

    fun creditWallet(adminUid: String, targetUid: String, amount: Double, description: String) {
        viewModelScope.launch {
            _creditState.value = CreditUiState.Loading
            val result = walletRepository.creditWallet(adminUid, targetUid, amount, description)
            _creditState.value = result.fold(
                onSuccess = { CreditUiState.Success },
                onFailure = { e -> CreditUiState.Error(e.message ?: "Credit failed") }
            )
        }
    }

    fun clearCreditState() {
        _creditState.value = CreditUiState.Idle
    }

    fun saveSettings(lowBalance: Double, interacEmail: String, adminEmail: String, adminUid: String) {
        viewModelScope.launch {
            configRepository.updateLowBalanceThreshold(lowBalance, adminUid)
            configRepository.updateAdminInteracEmail(interacEmail, adminUid)
            configRepository.updateAdminEmail(adminEmail, adminUid)
        }
    }
}
