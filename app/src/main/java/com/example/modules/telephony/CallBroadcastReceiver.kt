package com.example.modules.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.example.data.prefs.LoriPreferences
import com.example.modules.voice.LoriVoiceEngine

class CallBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            val prefs = LoriPreferences.getInstance(context).settings.value
            if (!prefs.isCallAssistantEnabled || prefs.isQuietMode) return

            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    val callerName = LoriCallModule.resolveContactName(context, incomingNumber)
                    val callInfo = IncomingCallInfo(
                        phoneNumber = incomingNumber,
                        callerName = callerName,
                        state = "RINGING"
                    )
                    LoriCallModule.updateCallState(callInfo)

                    // Announce incoming caller if voice alerts are enabled
                    val announcement = LoriCallModule.formatCallAnnouncement(callerName, incomingNumber)
                    LoriVoiceEngine.getInstance(context).speak(announcement, prefs.ttsSpeechRate, prefs.ttsSpeechPitch)
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    LoriCallModule.updateCallState(
                        IncomingCallInfo(
                            phoneNumber = incomingNumber,
                            callerName = null,
                            state = "OFFHOOK"
                        )
                    )
                    LoriVoiceEngine.getInstance(context).stopSpeaking()
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    LoriCallModule.updateCallState(null)
                }
            }
        }
    }
}
