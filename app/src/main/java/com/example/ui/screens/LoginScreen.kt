package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.modules.security.AuthState
import com.example.modules.security.SecureAuthManager
import com.example.ui.theme.LoriCyanSecondary
import com.example.ui.theme.LoriIndigoPrimary
import com.example.ui.theme.LoriPinkAccent

/**
 * Single-User Secure Login Screen for Lori.
 * Enforces rate limiting, cooldown timers, secure password hash comparison, and biometric access.
 */
@Composable
fun LoginScreen(
    authManager: SecureAuthManager,
    onLoginSuccess: () -> Unit
) {
    val authState by authManager.authState.collectAsState()
    var phoneNumber by remember { mutableStateOf(authState.authorizedPhone) }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val isLocked = System.currentTimeMillis() < authState.cooldownUntilTimestamp
    val remainingSec = if (isLocked) ((authState.cooldownUntilTimestamp - System.currentTimeMillis()) / 1000).coerceAtLeast(1) else 0

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lori_login_screen"),
        color = Color(0xFF0F0D15)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Ambient Background Glow
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                LoriIndigoPrimary.copy(alpha = 0.25f),
                                LoriCyanSecondary.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1A29).copy(alpha = 0.95f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFD0BCFF).copy(alpha = 0.3f),
                            Color(0xFF4DD0E1).copy(alpha = 0.3f)
                        )
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(LoriIndigoPrimary, LoriCyanSecondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Security",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "LORI ASSISTANT",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )

                    Text(
                        text = "Private AI Ecosystem • Single User Access",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCAC4D0),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Phone Number Field
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = {
                            phoneNumber = it
                            errorMessage = null
                        },
                        label = { Text("Authorized Phone Number") },
                        leadingIcon = {
                            Icon(Icons.Filled.Phone, contentDescription = "Phone", tint = Color(0xFFD0BCFF))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoriCyanSecondary,
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedLabelColor = LoriCyanSecondary,
                            unfocusedLabelColor = Color(0xFFCAC4D0),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_login_phone")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = "Password", tint = Color(0xFFD0BCFF))
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFFCAC4D0)
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (!isLocked && !isLoading) {
                                    isLoading = true
                                    val res = authManager.login(phoneNumber, password)
                                    isLoading = false
                                    if (res is SecureAuthManager.AuthResult.Success) {
                                        onLoginSuccess()
                                    } else if (res is SecureAuthManager.AuthResult.Error) {
                                        errorMessage = res.message
                                    }
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoriCyanSecondary,
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedLabelColor = LoriCyanSecondary,
                            unfocusedLabelColor = Color(0xFFCAC4D0),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_login_password")
                    )

                    AnimatedVisibility(visible = errorMessage != null || isLocked) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isLocked) "Cooldown active. Try again in $remainingSec s." else (errorMessage ?: ""),
                                color = LoriPinkAccent,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Login Action Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            isLoading = true
                            val result = authManager.login(phoneNumber, password)
                            isLoading = false
                            if (result is SecureAuthManager.AuthResult.Success) {
                                onLoginSuccess()
                            } else if (result is SecureAuthManager.AuthResult.Error) {
                                errorMessage = result.message
                            }
                        },
                        enabled = !isLocked && !isLoading && phoneNumber.isNotBlank() && password.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LoriIndigoPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_submit_login")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "LOGIN TO LORI",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Biometric Unlock Shortcut if available
                    if (authState.isBiometricEnabled && authState.authorizedPhone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val res = authManager.loginWithBiometric()
                                if (res is SecureAuthManager.AuthResult.Success) {
                                    onLoginSuccess()
                                } else if (res is SecureAuthManager.AuthResult.Error) {
                                    errorMessage = res.message
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_biometric_login")
                        ) {
                            Icon(Icons.Filled.Fingerprint, contentDescription = "Biometric", tint = LoriCyanSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Biometric Quick Unlock", color = LoriCyanSecondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
