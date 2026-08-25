@file:OptIn(ExperimentalMaterial3Api::class)

package com.alaa.o2rufumleitung.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alaa.o2rufumleitung.R
import com.alaa.o2rufumleitung.data.ForwardingType
import com.alaa.o2rufumleitung.ussd.UssdManager
import com.alaa.o2rufumleitung.ussd.UssdOutcome

/** One card = one forwarding type: explanation, destination number, on/off switch, status. */
@Composable
fun ForwardingCard(
    type: ForwardingType,
    state: CardUiState,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onStateChange: ((CardUiState) -> CardUiState) -> Unit,
    ussdManager: UssdManager
) {
    // Resolved here, during composition - the callbacks below run outside
    // composition and can't call stringResource() themselves.
    val strings = ForwardingCardStrings(
        statusUnknown = stringResource(R.string.status_unknown),
        statusChecking = stringResource(R.string.status_checking),
        statusActiveTemplate = stringResource(R.string.status_active_label),
        statusErrorGeneric = stringResource(R.string.status_error_generic),
        statusErrorPermission = stringResource(R.string.status_error_permission),
        statusOpenedDialer = stringResource(R.string.status_opened_dialer),
        errorMissingNumber = stringResource(R.string.error_missing_number)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(type.titleRes), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = stringResource(type.explanationRes), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.width(12.dp))
                if (state.requestState == RequestState.LOADING) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                } else {
                    Switch(
                        checked = state.isActive == true,
                        enabled = enabled,
                        onCheckedChange = { checked ->
                            handleToggle(
                                checked = checked,
                                type = type,
                                state = state,
                                ussdManager = ussdManager,
                                strings = strings,
                                onStateChange = onStateChange
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            NumberSourceSelector(
                type = type,
                state = state,
                enabled = enabled && state.requestState == RequestState.IDLE,
                onStateChange = onStateChange,
                ussdManager = ussdManager,
                strings = strings
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val statusText = when {
                    state.requestState == RequestState.LOADING -> strings.statusChecking
                    state.statusMessage.isNotBlank() -> state.statusMessage
                    else -> strings.statusUnknown
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                TextButton(
                    enabled = enabled && state.requestState == RequestState.IDLE,
                    onClick = {
                        onStateChange { it.copy(requestState = RequestState.LOADING) }
                        ussdManager.sendUssd(type.statusCode) { outcome ->
                            onStateChange { applyOutcome(it, outcome, activating = null, strings = strings) }
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.cd_check_status),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.check_status_button))
                }
            }
        }
    }
}

@Composable
private fun NumberSourceSelector(
    type: ForwardingType,
    state: CardUiState,
    enabled: Boolean,
    onStateChange: ((CardUiState) -> CardUiState) -> Unit,
    ussdManager: UssdManager,
    strings: ForwardingCardStrings
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.numberSource == NumberSource.O2_MAILBOX,
                enabled = enabled,
                onClick = {
                    onStateChange {
                        it.copy(
                            numberSource = NumberSource.O2_MAILBOX,
                            requestState = RequestState.LOADING
                        )
                    }
                    ussdManager.sendUssd(
                        type.activationCode(UssdManager.O2_MAILBOX_SHORT_CODE)
                    ) { outcome ->
                        onStateChange { applyOutcome(it, outcome, activating = true, strings = strings) }
                    }
                },
                label = { Text(stringResource(R.string.number_source_o2_mailbox)) }
            )
            FilterChip(
                selected = state.numberSource == NumberSource.CUSTOM,
                enabled = enabled,
                onClick = { onStateChange { it.copy(numberSource = NumberSource.CUSTOM) } },
                label = { Text(stringResource(R.string.number_source_custom)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.numberSource == NumberSource.O2_MAILBOX) {
            Text(
                text = stringResource(R.string.o2_mailbox_preset_hint),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            OutlinedTextField(
                value = state.customNumber,
                onValueChange = { value -> onStateChange { it.copy(customNumber = value) } },
                enabled = enabled,
                singleLine = true,
                label = { Text(stringResource(R.string.custom_number_label)) },
                placeholder = { Text(stringResource(R.string.custom_number_placeholder)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class ForwardingCardStrings(
    val statusUnknown: String,
    val statusChecking: String,
    val statusActiveTemplate: String,
    val statusErrorGeneric: String,
    val statusErrorPermission: String,
    val statusOpenedDialer: String,
    val errorMissingNumber: String
)

private fun handleToggle(
    checked: Boolean,
    type: ForwardingType,
    state: CardUiState,
    ussdManager: UssdManager,
    strings: ForwardingCardStrings,
    onStateChange: ((CardUiState) -> CardUiState) -> Unit
) {
    if (checked) {
        val number = numberToUse(state)
        if (number.isNullOrBlank()) {
            onStateChange { it.copy(statusMessage = strings.errorMissingNumber) }
            return
        }
        onStateChange { it.copy(requestState = RequestState.LOADING) }
        ussdManager.sendUssd(type.activationCode(number)) { outcome ->
            onStateChange { applyOutcome(it, outcome, activating = true, strings = strings) }
        }
    } else {
        onStateChange { it.copy(requestState = RequestState.LOADING) }
        ussdManager.sendUssd(type.deactivationCode) { outcome ->
            onStateChange { applyOutcome(it, outcome, activating = false, strings = strings) }
        }
    }
}

private fun numberToUse(state: CardUiState): String? =
    when (state.numberSource) {
        NumberSource.O2_MAILBOX -> UssdManager.O2_MAILBOX_SHORT_CODE
        NumberSource.CUSTOM -> state.customNumber.takeIf { it.isNotBlank() }
    }

private fun applyOutcome(
    current: CardUiState,
    outcome: UssdOutcome,
    activating: Boolean?,
    strings: ForwardingCardStrings
): CardUiState = when (outcome) {
    is UssdOutcome.Success -> current.copy(
        requestState = RequestState.IDLE,
        isActive = activating ?: current.isActive,
        statusMessage = String.format(strings.statusActiveTemplate, outcome.response)
    )
    is UssdOutcome.NetworkError -> current.copy(
        requestState = RequestState.IDLE,
        statusMessage = strings.statusErrorGeneric
    )
    UssdOutcome.PermissionMissing -> current.copy(
        requestState = RequestState.IDLE,
        statusMessage = strings.statusErrorPermission
    )
    UssdOutcome.OpenedInDialer -> current.copy(
        requestState = RequestState.IDLE,
        statusMessage = strings.statusOpenedDialer
    )
}
