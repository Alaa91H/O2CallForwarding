package com.alaa.o2rufumleitung.ussd

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/** Result of sending a USSD (network control) code. */
sealed class UssdOutcome {
    /** The network answered in-app, with its free-text reply. */
    data class Success(val response: String) : UssdOutcome()

    /** The network rejected the request (bad code, no signal, barred, etc.). */
    data class NetworkError(val failureCode: Int) : UssdOutcome()

    /** CALL_PHONE was not granted. */
    object PermissionMissing : UssdOutcome()

    /** In-app USSD wasn't available (old Android version, or the OEM dialer
     *  blocked it), so the code was opened in the system Phone app instead -
     *  the user only needs to press the call button to finish. */
    object OpenedInDialer : UssdOutcome()
}

/**
 * Sends GSM call-forwarding control codes - the same codes you'd dial by
 * hand, e.g. **21*+491701234567#. On API 26+ with CALL_PHONE granted, the
 * request and the network's reply are both handled in-app. Otherwise this
 * falls back to opening the system dialer with the code pre-filled.
 */
class UssdManager(private val context: Context) {

    fun hasCallPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun sendUssd(code: String, onResult: (UssdOutcome) -> Unit) {
        if (!hasCallPermission()) {
            onResult(UssdOutcome.PermissionMissing)
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            openInDialer(code)
            onResult(UssdOutcome.OpenedInDialer)
            return
        }

        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        try {
            telephonyManager.sendUssdRequest(
                code,
                object : TelephonyManager.UssdResponseCallback() {
                    override fun onReceiveUssdResponse(
                        tm: TelephonyManager,
                        request: String,
                        response: CharSequence
                    ) {
                        onResult(UssdOutcome.Success(response.toString()))
                    }

                    override fun onReceiveUssdResponseFailed(
                        tm: TelephonyManager,
                        request: String,
                        failureCode: Int
                    ) {
                        onResult(UssdOutcome.NetworkError(failureCode))
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            openInDialer(code)
            onResult(UssdOutcome.OpenedInDialer)
        }
    }

    /**
     * Starts the phone call carrying an MMI/USSD code immediately. This is used
     * for the O2 mailbox preset because some OEMs suppress in-app USSD
     * callbacks even though their Phone app can execute the same network code.
     */
    fun startCall(code: String, onResult: (UssdOutcome) -> Unit) {
        if (!hasCallPermission()) {
            onResult(UssdOutcome.PermissionMissing)
            return
        }
        try {
            openInDialer(code)
            onResult(UssdOutcome.OpenedInDialer)
        } catch (_: Exception) {
            onResult(UssdOutcome.NetworkError(-1))
        }
    }

    @SuppressLint("MissingPermission")
    private fun openInDialer(code: String) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(code)))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    companion object {
        /** O2 Germany's official short destination for forwarding to its mailbox. */
        const val O2_MAILBOX_SHORT_CODE = "333"

        /** Standard GSM code that cancels every active forwarding at once. */
        const val CANCEL_ALL_CODE = "##002#"
    }
}
