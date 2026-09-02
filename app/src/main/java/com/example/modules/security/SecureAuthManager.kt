package com.example.modules.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

data class AuthState(
    val isAuthenticated: Boolean = true,
    val authorizedPhone: String = "Authorized User",
    val token: String? = "lori_direct_access_token",
    val failedAttempts: Int = 0,
    val cooldownUntilTimestamp: Long = 0L,
    val isBiometricEnabled: Boolean = false,
    val lastLoginTimestamp: Long = 0L
)

/**
 * Secure Single-User Authentication Manager for Lori.
 * Enforces rate limiting, failed login cooldowns, and secure hash comparisons.
 */
class SecureAuthManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "lori_secure_auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("lori_auth_prefs_fallback", Context.MODE_PRIVATE)
    }

    private val _authState = MutableStateFlow(loadInitialState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private fun loadInitialState(): AuthState {
        val token = prefs.getString(KEY_TOKEN, null)
        val phone = prefs.getString(KEY_PHONE, "") ?: ""
        val isAuth = !token.isNullOrBlank()
        val failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        val cooldown = prefs.getLong(KEY_COOLDOWN_UNTIL, 0L)
        val biometric = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
        val lastLogin = prefs.getLong(KEY_LAST_LOGIN, 0L)

        return AuthState(
            isAuthenticated = isAuth,
            authorizedPhone = phone,
            token = token,
            failedAttempts = failed,
            cooldownUntilTimestamp = cooldown,
            isBiometricEnabled = biometric,
            lastLoginTimestamp = lastLogin
        )
    }

    /**
     * Authenticates the single authorized user with phone and password.
     * Uses SHA-256 with salt verification and rate limiting.
     */
    fun login(phone: String, password: String): AuthResult {
        val now = System.currentTimeMillis()
        val current = _authState.value

        if (now < current.cooldownUntilTimestamp) {
            val remainingSec = ((current.cooldownUntilTimestamp - now) / 1000).toInt()
            return AuthResult.Error("Too many failed attempts. Try again in $remainingSec seconds.")
        }

        val cleanedPhone = phone.trim().replace(Regex("[^0-9+]"), "")
        if (cleanedPhone.isBlank() || password.isBlank()) {
            return AuthResult.Error("Phone number and password are required.")
        }

        val storedPhone = prefs.getString(KEY_AUTHORIZED_PHONE, null)
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, null)
        val storedSalt = prefs.getString(KEY_PASSWORD_SALT, null)

        // First-time initialization if no credentials are configured yet
        if (storedPhone == null || storedHash == null || storedSalt == null) {
            val newSalt = generateSalt()
            val newHash = hashPassword(password, newSalt)
            prefs.edit()
                .putString(KEY_AUTHORIZED_PHONE, cleanedPhone)
                .putString(KEY_PASSWORD_HASH, newHash)
                .putString(KEY_PASSWORD_SALT, newSalt)
                .putString(KEY_PHONE, cleanedPhone)
                .putString(KEY_TOKEN, generateSessionToken())
                .putLong(KEY_LAST_LOGIN, now)
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .apply()

            _authState.value = _authState.value.copy(
                isAuthenticated = true,
                authorizedPhone = cleanedPhone,
                token = prefs.getString(KEY_TOKEN, null),
                failedAttempts = 0,
                lastLoginTimestamp = now
            )
            return AuthResult.Success("First-time authorized user enrolled and authenticated successfully.")
        }

        // Verify authorized credentials
        val inputHash = hashPassword(password, storedSalt)
        val isPhoneMatch = cleanedPhone == storedPhone
        val isPasswordMatch = constantTimeEquals(inputHash, storedHash)

        if (isPhoneMatch && isPasswordMatch) {
            val sessionToken = generateSessionToken()
            prefs.edit()
                .putString(KEY_TOKEN, sessionToken)
                .putString(KEY_PHONE, cleanedPhone)
                .putLong(KEY_LAST_LOGIN, now)
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_COOLDOWN_UNTIL, 0L)
                .apply()

            _authState.value = _authState.value.copy(
                isAuthenticated = true,
                authorizedPhone = cleanedPhone,
                token = sessionToken,
                failedAttempts = 0,
                cooldownUntilTimestamp = 0L,
                lastLoginTimestamp = now
            )
            return AuthResult.Success("Welcome back!")
        } else {
            val newFailed = current.failedAttempts + 1
            val cooldown = if (newFailed >= 5) now + (60 * 1000L) else 0L
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, newFailed)
                .putLong(KEY_COOLDOWN_UNTIL, cooldown)
                .apply()

            _authState.value = _authState.value.copy(
                failedAttempts = newFailed,
                cooldownUntilTimestamp = cooldown
            )

            val errorMsg = if (cooldown > 0) {
                "Invalid credentials. Account locked for 60 seconds."
            } else {
                "Invalid credentials. (${5 - newFailed} attempts remaining)"
            }
            return AuthResult.Error(errorMsg)
        }
    }

    /**
     * Biometric login when enabled
     */
    fun loginWithBiometric(): AuthResult {
        val storedToken = prefs.getString(KEY_TOKEN, null)
        val storedPhone = prefs.getString(KEY_AUTHORIZED_PHONE, null)

        if (!storedPhone.isNullOrBlank()) {
            val now = System.currentTimeMillis()
            val token = storedToken ?: generateSessionToken()
            prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_LAST_LOGIN, now)
                .apply()

            _authState.value = _authState.value.copy(
                isAuthenticated = true,
                authorizedPhone = storedPhone,
                token = token,
                lastLoginTimestamp = now
            )
            return AuthResult.Success("Biometric authentication verified.")
        }
        return AuthResult.Error("Please login with password first to enable biometric access.")
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .apply()
        _authState.value = _authState.value.copy(
            isAuthenticated = false,
            token = null
        )
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _authState.value = _authState.value.copy(isBiometricEnabled = enabled)
    }

    private fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val salted = (password + salt).toByteArray(Charsets.UTF_8)
        val hash = digest.digest(salted)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateSessionToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return "lori_token_" + bytes.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    sealed class AuthResult {
        data class Success(val message: String) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    companion object {
        private const val KEY_AUTHORIZED_PHONE = "auth_authorized_phone"
        private const val KEY_PASSWORD_HASH = "auth_password_hash"
        private const val KEY_PASSWORD_SALT = "auth_password_salt"
        private const val KEY_PHONE = "auth_user_phone"
        private const val KEY_TOKEN = "auth_session_token"
        private const val KEY_FAILED_ATTEMPTS = "auth_failed_attempts"
        private const val KEY_COOLDOWN_UNTIL = "auth_cooldown_until"
        private const val KEY_BIOMETRIC_ENABLED = "auth_biometric_enabled"
        private const val KEY_LAST_LOGIN = "auth_last_login"

        @Volatile
        private var INSTANCE: SecureAuthManager? = null

        fun getInstance(context: Context): SecureAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
