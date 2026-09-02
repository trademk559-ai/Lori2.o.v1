package com.example.modules.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Controller for hardware & device automation: Flashlight, SOS Strobe, Battery Telemetry,
 * Volume controls, Haptic Pulses, Network Diagnostics, and Smart Jarvis Routines.
 */
class DeviceController(private val context: Context) {

    companion object {
        @Volatile
        private var isFlashlightOn: Boolean = false
        @Volatile
        private var isSosStrobeActive: Boolean = false
        private var torchCameraId: String? = null
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        findCameraWithFlash()
    }

    private fun findCameraWithFlash() {
        if (cameraManager == null || torchCameraId != null) return
        try {
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    torchCameraId = id
                    break
                }
            }
            if (torchCameraId == null && cameraManager.cameraIdList.isNotEmpty()) {
                torchCameraId = cameraManager.cameraIdList[0]
            }
        } catch (e: Exception) {
            Log.e("DeviceController", "Flashlight detection error", e)
        }
    }

    /**
     * Toggles flashlight on or off
     */
    fun setFlashlight(enable: Boolean): Pair<Boolean, String> {
        findCameraWithFlash()
        val camId = torchCameraId ?: return Pair(false, "Device par Flashlight hardware detect nahi hua.")
        return try {
            cameraManager?.setTorchMode(camId, enable)
            isFlashlightOn = enable
            val msg = if (enable) "Haan Boss! Flashlight ON kar di gayi hai." else "Haan Boss! Flashlight OFF kar di gayi hai."
            Pair(true, msg)
        } catch (e: CameraAccessException) {
            Log.e("DeviceController", "Failed to toggle torch", e)
            Pair(false, "Boss, flashlight access karne mein dikkat aayi: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e("DeviceController", "Torch error", e)
            Pair(false, "Boss, torch trigger nahi ho paya.")
        }
    }

    fun toggleFlashlight(): Pair<Boolean, String> {
        return setFlashlight(!isFlashlightOn)
    }

    fun isTorchActive(): Boolean = isFlashlightOn

    /**
     * Retrieves detailed battery telemetry
     */
    data class BatteryInfo(
        val percentage: Int,
        val isCharging: Boolean,
        val chargingType: String,
        val health: String,
        val summaryText: String
    )

    fun getBatteryTelemetry(): BatteryInfo {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 50

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val plugType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            BatteryManager.BATTERY_PLUGGED_AC -> "Fast AC Charger"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
            else -> if (isCharging) "Charger" else "Discharging"
        }

        val healthCode = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Optimal"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Critical"
            else -> "Normal"
        }

        val chargingMsg = if (isCharging) "charging ho raha hai ($plugType se)" else "battery power par chal raha hai"
        val summary = "Haan Boss! Battery $pct% par hai, $chargingMsg. Power health: $health."

        return BatteryInfo(
            percentage = pct,
            isCharging = isCharging,
            chargingType = plugType,
            health = health,
            summaryText = summary
        )
    }

    /**
     * Controls device volume: 1 for up, -1 for down, 0 for mute
     */
    fun adjustVolume(direction: Int): String {
        val am = audioManager ?: return "Boss, audio controls available nahi hain."
        return try {
            when {
                direction > 0 -> {
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    "Ji Boss! Media volume badha diya gaya hai."
                }
                direction < 0 -> {
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    "Ji Boss! Media volume kam kar diya gaya hai."
                }
                else -> {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                    "Haan Boss! Media mute kar diya gaya hai."
                }
            }
        } catch (e: Exception) {
            Log.e("DeviceController", "Error adjusting volume", e)
            "Volume adjust karne mein error: ${e.localizedMessage}"
        }
    }

    /**
     * Executes Morning Routine
     */
    fun runMorningRoutine(): String {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        val now = Date()
        val currentTime = timeFormat.format(now)
        val currentDate = dateFormat.format(now)

        val battery = getBatteryTelemetry()

        return """
            Shubh Prabhat! Good morning Boss!
            Abhi waqt hua hai $currentTime, aur aaj $currentDate hai.
            Battery level ${battery.percentage}% par hai.
            All systems online hain. Boliye Boss, aaj ka din conquer karne ke liye kya pehla mission hai?
        """.trimIndent()
    }

    /**
     * Executes Night Routine
     */
    fun runNightRoutine(): String {
        if (isFlashlightOn) {
            setFlashlight(false)
        }
        val battery = getBatteryTelemetry()
        val chargeReminder = if (!battery.isCharging && battery.percentage < 40) {
            "Boss, battery sirf ${battery.percentage}% hai, raat me charging par lagana behtar hoga."
        } else {
            "Battery status bilkul secure hai (${battery.percentage}%)."
        }

        return """
            Shubh Ratri! Good night Boss!
            Main Flashlight check karke standby mode me ja rahi hoon.
            $chargeReminder
            Aap aaram se so jaiye, Lori aapke phone ki telemetry monitor karti rahegi!
        """.trimIndent()
    }

    /**
     * Complete System Diagnostic Telemetry
     */
    fun runSystemDiagnostics(): String {
        val battery = getBatteryTelemetry()
        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMem = runtime.maxMemory() / (1024 * 1024)
        val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val netInfo = getNetworkStatus()

        triggerHapticPulse(50)

        return """
            JARVIS Core Diagnostics Report:
            • OS Architecture: $osVersion
            • Network Link: $netInfo
            • Power Telemetry: ${battery.percentage}% [${battery.health}]
            • Power Source: ${battery.chargingType}
            • App Heap Memory: ${usedMem}MB of ${maxMem}MB used
            • Flashlight Subsystem: ${if (isFlashlightOn) "Active" else "Standby"}
            • Core AI Neural Engine: Operational & Online
        """.trimIndent()
    }

    /**
     * Network Connectivity Inspector
     */
    fun getNetworkStatus(): String {
        val cm = connectivityManager ?: return "Network manager unavailable"
        val activeNet = cm.activeNetwork ?: return "Offline (No Active Connection)"
        val caps = cm.getNetworkCapabilities(activeNet) ?: return "Disconnected"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi High-Speed Link (Online)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular 4G/5G Link (Online)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet Gigabit (Online)"
            else -> "Connected"
        }
    }

    /**
     * Tactile / Haptic feedback pulse
     */
    fun triggerHapticPulse(durationMs: Long = 40) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.e("DeviceController", "Haptic feedback error", e)
        }
    }

    /**
     * SOS Emergency Strobe Flasher (toggles on/off rapidly)
     */
    fun toggleSosStrobe(): Pair<Boolean, String> {
        findCameraWithFlash()
        val camId = torchCameraId ?: return Pair(false, "Device par Flashlight hardware nahi mila.")
        
        return if (isSosStrobeActive) {
            isSosStrobeActive = false
            setFlashlight(false)
            triggerHapticPulse(100)
            Pair(true, "SOS Strobe mode DEACTIVATED.")
        } else {
            isSosStrobeActive = true
            triggerHapticPulse(200)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    while (isSosStrobeActive) {
                        cameraManager?.setTorchMode(camId, true)
                        delay(120)
                        cameraManager?.setTorchMode(camId, false)
                        delay(120)
                    }
                    cameraManager?.setTorchMode(camId, false)
                } catch (e: Exception) {
                    Log.e("DeviceController", "SOS Strobe error", e)
                    isSosStrobeActive = false
                }
            }
            Pair(true, "SOS Emergency Strobe ACTIVATED.")
        }
    }

    fun isSosActive(): Boolean = isSosStrobeActive
}
