@file:OptIn(ExperimentalMaterial3Api::class)

package com.alaa.o2rufumleitung.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alaa.o2rufumleitung.R
import com.alaa.o2rufumleitung.data.ForwardingType
import com.alaa.o2rufumleitung.ussd.UssdManager
import com.alaa.o2rufumleitung.ussd.UssdOutcome
import com.alaa.o2rufumleitung.ui.theme.CardBlueGrayDark
import com.alaa.o2rufumleitung.ui.theme.CardBlueGrayLight
import com.alaa.o2rufumleitung.ui.theme.CardGrayDark
import com.alaa.o2rufumleitung.ui.theme.CardGrayLight
import com.alaa.o2rufumleitung.ui.theme.OnCardDark
import com.alaa.o2rufumleitung.ui.theme.OnCardLight

@Composable
fun ForwardingScreen(viewModel: ForwardingViewModel = viewModel()) {
    val context = LocalContext.current
    val ussdManager = remember { UssdManager(context) }
    var hasPermission by remember { mutableStateOf(ussdManager.hasCallPermission()) }
    val detectedVoicemail = remember(hasPermission) {
        if (hasPermission) ussdManager.systemVoiceMailNumber() else null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    val darkTheme = isSystemInDarkTheme()
    val grayColor = if (darkTheme) CardGrayDark else CardGrayLight
    val blueGrayColor = if (darkTheme) CardBlueGrayDark else CardBlueGrayLight
    val onCardColor = if (darkTheme) OnCardDark else OnCardLight
    val types = ForwardingType.entries

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(stringResource(R.string.app_name)) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { InfoBanner() }

            if (!hasPermission) {
                item {
                    PermissionCard(
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CALL_PHONE) }
                    )
                }
            }

            itemsIndexed(types) { index, type ->
                val container = if (index % 2 == 0) grayColor else blueGrayColor
                ForwardingCard(
                    type = type,
                    state = viewModel.cardStates.getValue(type),
                    containerColor = container,
                    contentColor = onCardColor,
                    enabled = hasPermission,
                    detectedVoicemailNumber = detectedVoicemail,
                    onStateChange = { transform -> viewModel.update(type, transform) },
                    ussdManager = ussdManager
                )
            }

            item {
                CancelAllCard(
                    enabled = hasPermission,
                    containerColor = if (types.size % 2 == 0) grayColor else blueGrayColor,
                    contentColor = onCardColor,
                    ussdManager = ussdManager
                )
            }

            item {
                Text(
                    text = stringResource(R.string.footer_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.info_banner_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.info_banner_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Phone,
                    contentDescription = stringResource(R.string.cd_permission_icon),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.permission_rationale_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.permission_rationale_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRequestPermission) {
                Text(stringResource(R.string.permission_grant_button))
            }
        }
    }
}

@Composable
private fun CancelAllCard(
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    ussdManager: UssdManager
) {
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val genericError = stringResource(R.string.status_error_generic)
    val permissionError = stringResource(R.string.status_error_permission)
    val openedDialer = stringResource(R.string.status_opened_dialer)
    val successTemplate = stringResource(R.string.status_active_label)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.cancel_all_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.cancel_all_explanation), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    enabled = enabled && !loading,
                    onClick = {
                        loading = true
                        ussdManager.sendUssd(UssdManager.CANCEL_ALL_CODE) { outcome ->
                            loading = false
                            message = when (outcome) {
                                is UssdOutcome.Success -> String.format(successTemplate, outcome.response)
                                is UssdOutcome.NetworkError -> genericError
                                UssdOutcome.PermissionMissing -> permissionError
                                UssdOutcome.OpenedInDialer -> openedDialer
                            }
                        }
                    }
                ) {
                    Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.cancel_all_button))
                }
                if (loading) {
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
            if (message.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
