package com.example.modules.telephony

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class IncomingCallInfo(
    val phoneNumber: String?,
    val callerName: String?,
    val state: String, // "RINGING", "OFFHOOK", "IDLE"
    val timestamp: Long = System.currentTimeMillis()
)

object LoriCallModule {

    private val _currentCallState = MutableStateFlow<IncomingCallInfo?>(null)
    val currentCallState: StateFlow<IncomingCallInfo?> = _currentCallState.asStateFlow()

    fun updateCallState(callInfo: IncomingCallInfo?) {
        _currentCallState.value = callInfo
    }

    /**
     * Resolves contact name from phone number if READ_CONTACTS permission is granted
     */
    fun resolveContactName(context: Context, phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) return null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return it.getString(nameIndex)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("LoriCallModule", "Error resolving contact name", e)
            null
        }
    }

    /**
     * Formats natural Hindi announcement for incoming call
     */
    fun formatCallAnnouncement(callerName: String?, phoneNumber: String?): String {
        return if (!callerName.isNullOrBlank()) {
            "Bhai, $callerName ka call aa raha hai."
        } else if (!phoneNumber.isNullOrBlank()) {
            "Bhai, number $phoneNumber se call aa raha hai."
        } else {
            "Bhai, ek unknown number ka call aa raha hai."
        }
    }

    /**
     * Determines if user voice input is answering, rejecting, or ignoring call
     */
    fun parseCallVoiceCommand(command: String): CallAction {
        val lower = command.lowercase().trim()
        val answerPhrases = listOf("haan, receive karo", "haan receive karo", "call utha lo", "utha lo", "receive", "receive karo", "pick up", "answer")
        val rejectPhrases = listOf("cut kar do", "call kaat do", "kaat do", "reject karo", "reject", "decline", "cut karo")
        val ignorePhrases = listOf("nahi", "rehne do", "mat uthao", "ignore", "chhod do")

        return when {
            answerPhrases.any { lower.contains(it) } -> CallAction.ANSWER
            rejectPhrases.any { lower.contains(it) } -> CallAction.REJECT
            ignorePhrases.any { lower.contains(it) } -> CallAction.IGNORE
            else -> CallAction.NONE
        }
    }

    /**
     * Answers call using TelecomManager if permissions allow
     */
    @SuppressLint("MissingPermission")
    fun answerCall(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null && ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                try {
                    telecomManager.acceptRingingCall()
                    return true
                } catch (e: Exception) {
                    Log.e("LoriCallModule", "Error accepting call", e)
                }
            }
        }
        return false
    }

    /**
     * Ends call using TelecomManager if supported
     */
    @SuppressLint("MissingPermission")
    fun endCall(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null && ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                try {
                    return telecomManager.endCall()
                } catch (e: Exception) {
                    Log.e("LoriCallModule", "Error ending call", e)
                }
            }
        }
        return false
    }

    /**
     * Fallback to open system dialer
     */
    fun openDialer(context: Context, phoneNumber: String? = null) {
        try {
            val intent = if (!phoneNumber.isNullOrBlank()) {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber)}"))
            } else {
                Intent(Intent.ACTION_DIAL)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

enum class CallAction {
    ANSWER,
    REJECT,
    IGNORE,
    NONE
}
