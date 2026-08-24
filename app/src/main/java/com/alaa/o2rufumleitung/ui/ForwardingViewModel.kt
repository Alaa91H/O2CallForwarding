package com.alaa.o2rufumleitung.ui

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.alaa.o2rufumleitung.data.ForwardingType

enum class NumberSource { VOICEMAIL, CUSTOM }

enum class RequestState { IDLE, LOADING }

data class CardUiState(
    val numberSource: NumberSource = NumberSource.VOICEMAIL,
    val customNumber: String = "",
    val isActive: Boolean? = null,
    val requestState: RequestState = RequestState.IDLE,
    val statusMessage: String = ""
)

/** Holds the five cards' state so it survives rotation/config changes. */
class ForwardingViewModel : ViewModel() {

    val cardStates = mutableStateMapOf<ForwardingType, CardUiState>().apply {
        ForwardingType.entries.forEach { type -> put(type, CardUiState()) }
    }

    fun update(type: ForwardingType, transform: (CardUiState) -> CardUiState) {
        cardStates[type] = transform(cardStates.getValue(type))
    }
}
