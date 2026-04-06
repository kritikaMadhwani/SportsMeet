package com.madhwanikritika.sportsmeet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madhwanikritika.sportsmeet.data.model.AppConfig
import com.madhwanikritika.sportsmeet.data.model.WalletTransaction
import com.madhwanikritika.sportsmeet.data.repository.ConfigRepository
import com.madhwanikritika.sportsmeet.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerWalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _uid = MutableStateFlow<String?>(null)

    private val _playerTabIndex = MutableStateFlow(0)
    val playerTabIndex: StateFlow<Int> = _playerTabIndex.asStateFlow()

    fun setUserId(uid: String?) {
        _uid.value = uid
    }

    fun setPlayerTabIndex(index: Int) {
        _playerTabIndex.value = index
    }

    val balance: StateFlow<Double> = _uid.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(0.0) else walletRepository.getWalletBalance(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val transactions: StateFlow<List<WalletTransaction>> = _uid.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else walletRepository.getTransactions(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val appConfig: StateFlow<AppConfig> = configRepository.getAppConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppConfig()
        )

    init {
        viewModelScope.launch {
            runCatching { configRepository.ensureDefaultConfig() }
        }
    }
}
