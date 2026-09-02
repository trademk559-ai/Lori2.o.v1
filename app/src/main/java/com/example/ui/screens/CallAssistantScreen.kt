package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.modules.telephony.IncomingCallInfo
import com.example.modules.telephony.LoriCallModule
import com.example.ui.theme.LoriCallBlue
import com.example.viewmodel.LoriMainViewModel

@Composable
fun CallAssistantScreen(
    viewModel: LoriMainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val callState by viewModel.incomingCallState.collectAsState()
    val permissions by viewModel.permissionStatus.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("call_assistant_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = null,
                        tint = LoriCallBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Incoming Call Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Lori identifies who is calling and announces it in natural Hindi aloud.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Active Call Banner or Idle State Card
        item {
            if (callState != null && callState?.state == "RINGING") {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, LoriCallBlue, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(LoriCallBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhoneInTalk,
                                contentDescription = "Incoming Call",
                                tint = LoriCallBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Incoming Call Active",
                            style = MaterialTheme.typography.labelMedium,
                            color = LoriCallBlue,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = callState?.callerName ?: (callState?.phoneNumber ?: "Unknown Caller"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = LoriCallModule.formatCallAnnouncement(callState?.callerName, callState?.phoneNumber),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { LoriCallModule.endCall(context) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reject_call_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.CallEnd, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cut Karo", color = Color.White)
                            }

                            Button(
                                onClick = { LoriCallModule.answerCall(context) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("answer_call_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Receive Karo", color = Color.White)
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Call, contentDescription = null, tint = LoriCallBlue)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Call Assistant Active & Listening",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Jab bhi kisi ka call aayega, Lori aawaz dekar batayegi: “Bhai, [Name] ka call aa raha hai.”",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Test Call Announcement Demo
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Test Call Announcement Simulation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Simulate an incoming call from 'Rahul' to test Lori's voice announcement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val testCall = IncomingCallInfo(
                                    phoneNumber = "+91 98765 43210",
                                    callerName = "Rahul",
                                    state = "RINGING"
                                )
                                LoriCallModule.updateCallState(testCall)
                                viewModel.speakMessageAloud("Bhai, Rahul ka call aa raha hai.")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("simulate_known_call_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Rahul Call (Known)")
                        }

                        OutlinedButton(
                            onClick = {
                                val testCall = IncomingCallInfo(
                                    phoneNumber = "+91 91234 56789",
                                    callerName = null,
                                    state = "RINGING"
                                )
                                LoriCallModule.updateCallState(testCall)
                                viewModel.speakMessageAloud("Bhai, ek unknown number ka call aa raha hai.")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("simulate_unknown_call_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Unknown Call")
                        }
                    }
                }
            }
        }

        // Voice Command Cheat Sheet
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Voice Commands when Phone Rings:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("• “Haan, receive karo” / “Call utha lo” → Lori answers call", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• “Cut kar do” / “Call kaat do” → Lori rejects call", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• “Nahi” / “Rehne do” → Lori keeps phone ringing quietly", fontSize = 13.sp)
                }
            }
        }
    }
}
